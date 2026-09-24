# Explore With Me Plus

Проект представляет микросервисное приложение Explore With Me с рекомендательной системой.

## Архитектура

В проекте используются Config Server и Eureka Discovery Server. Core сервисы работают через HTTP REST и внутренние Feign API. Рекомендательная часть использует gRPC для синхронного обмена и Apache Kafka для потоковой обработки действий пользователей.

### Core сервисы

- event-service - события, публичный поиск событий, выдача рейтинга и рекомендаций, отправка действий VIEW и LIKE в Collector.
- request-service - заявки на участие. После создания заявки отправляет действие REGISTER в Collector.
- user-service - пользователи.
- location-service - геоданные и поиск событий по радиусу.
- interaction-api - общие DTO, Feign клиенты и gRPC клиенты для core сервисов.

### Сервисы рекомендательной системы

- collector - принимает UserActionProto по gRPC и публикует UserActionAvro в stats.user-actions.v1.
- aggregator - читает stats.user-actions.v1, поддерживает агрегированные суммы весов и считает косинусное сходство мероприятий, после чего пишет EventSimilarityAvro в stats.events-similarity.v1.
- analyzer - читает оба топика, хранит историю взаимодействий и сходства в PostgreSQL и предоставляет gRPC API для рекомендаций.
- common - общий модуль с ActionWeightResolver и настройками, поступающими из Config Server.

### Поток данных

1. Пользователь открывает опубликованное мероприятие.
2. event-service отправляет в Collector действие ACTION_VIEW.
3. Пользователь создает заявку.
4. request-service отправляет в Collector действие ACTION_REGISTER.
5. Пользователь ставит лайк после просмотра мероприятия.
6. event-service отправляет в Collector действие ACTION_LIKE.
7. Collector преобразует Proto сообщение в Avro сообщение и пишет его в Kafka.
8. Aggregator обновляет состояние и публикует новые значения сходства.
9. Analyzer обновляет PostgreSQL и отвечает на gRPC запросы core сервисов.

## Kafka

Для локального запуска Kafka используется compose.yaml.

После запуска создаются топики:

- stats.user-actions.v1
- stats.events-similarity.v1

Пример запуска:

```bash
docker compose up -d
```

При запуске Java сервисов с хоста используется `localhost:9092`. Для контейнеризированных приложений внутри той же Docker сети адрес Kafka должен быть `kafka:29092`.

## Proto и gRPC

Исходные Proto схемы находятся в `stat/serialization/proto-schemas/src/main/protobuf`.

### Collector

Пакет gRPC сервиса: `stats.service.collector`.

Метод:

`CollectUserAction(UserActionProto) -> Empty`

`UserActionProto` содержит `user_id`, `event_id`, `action_type` и `timestamp`.

### Analyzer

Пакет gRPC сервиса: `stats.service.dashboard`.

Методы:

- `GetRecommendationsForUser`
- `GetSimilarEvents`
- `GetInteractionsCount`

Все методы Analyzer возвращают поток `RecommendedEventProto`.

Клиенты находятся в `core/interaction-api/src/main/java/ru/practicum/explorewithme/interaction/grpc`.

Для discovery используется адрес вида `discovery:///analyzer` или `discovery:///collector`.

## Avro

Исходная Avro схема находится в `stat/serialization/avro-schemas/src/main/avro/stats.avdl`.

Namespace: `ru.practicum.ewm.stats.avro`.

Типы сообщений:

- `UserActionAvro`
- `EventSimilarityAvro`
- `ActionTypeAvro`

Kafka работает с двоичной Avro сериализацией без внешнего Schema Registry.

## Алгоритм Aggregator

Для каждого мероприятия хранится отображение пользователь -> максимальный вес его действия.

Для каждого мероприятия хранится сумма весов пользователей. Для каждой пары мероприятий хранится сумма минимальных весов общих пользователей.

Коэффициент сходства считается по формуле:

`S_min(A,B) / (sqrt(S_A) * sqrt(S_B))`

Пара мероприятий всегда сохраняется в каноническом порядке: сначала меньший идентификатор, затем больший.

При повторном действии сходство пересчитывается только для затронутого мероприятия. Если максимальный вес действия пользователя не изменился, пересчет не выполняется. Пара мероприятия с самим собой не создается.

Веса действий хранятся в одном общем внешнем конфиге Config Server:

`infra/config-server/src/main/resources/config/stat/application.yaml`

Текущие значения:

- VIEW = 0.4
- REGISTER = 0.8
- LIKE = 1.0

Класс `ActionWeightResolver` находится в общем модуле `stat/common` и используется Aggregator и Analyzer.

## Алгоритмы Analyzer

### Похожие мероприятия

Analyzer получает все коэффициенты пар с указанным мероприятием, исключает мероприятия, с которыми пользователь уже взаимодействовал, сортирует по коэффициенту сходства и возвращает первые N.

### Персональные рекомендации

1. Выбираются последние взаимодействия пользователя.
2. Для них находятся похожие мероприятия, с которыми пользователь еще не взаимодействовал.
3. Для каждого кандидата выбираются наиболее похожие уже просмотренные пользователем мероприятия.
4. Вычисляется взвешенная оценка кандидата.
5. Кандидаты сортируются по оценке.

### Сумма взаимодействий

Для каждого переданного мероприятия Analyzer возвращает сумму максимальных весов действий всех пользователей.

Таблицы Analyzer создаются из `stat/analyzer/src/main/resources/schema.sql`. Скрипт использует `CREATE TABLE IF NOT EXISTS` и `CREATE INDEX IF NOT EXISTS`.

## Изменения event-service

- Поле `views` заменено на `rating`.
- `GET /events` больше не отправляет VIEW.
- `GET /events/{eventId}` требует заголовок `X-EWM-USER-ID` и отправляет VIEW в Collector.
- Добавлен `GET /events/recommendations`.
- Добавлен `PUT /events/{eventId}/like`.
- Лайк разрешен только пользователю, который ранее посещал страницу мероприятия.

Для проверки факта посещения event-service хранит уникальную пару `user_id` и `event_id` в таблице `event_views`.

## Конфигурация

Локальные конфигурации core, infra и stat сервисов находятся в Config Server:

`infra/config-server/src/main/resources/config`

Конфигурации рекомендательной системы:

- `config/stat/application.yaml` - общий конфиг, в том числе веса действий
- `config/stat/collector/application.yaml`
- `config/stat/aggregator/application.yaml`
- `config/stat/analyzer/application.yaml`

Общие веса действий находятся только в `config/stat/application.yaml`. Сервисные конфиги Collector, Aggregator и Analyzer не дублируют эти значения.

Порты HTTP и gRPC у Collector и Analyzer выбираются случайно через значение `0`.

## Внешний API

Спецификация внешнего API:

https://github.com/dimitrievdmit/java-explore-with-me-plus/blob/main/ewm-main-service-spec.json

Локальная копия спецификации находится в `ewm-main-service-spec.json`.

## Сборка

Требования к окружению:

- Java 21
- Maven 3.9+
- Docker и Docker Compose
- PostgreSQL

Сборка всего проекта:

```bash
mvn clean package
```

Полный сценарий локального запуска требует сначала поднять инфраструктуру и Kafka, затем Config Server и Discovery Server, после чего core и stat сервисы.

