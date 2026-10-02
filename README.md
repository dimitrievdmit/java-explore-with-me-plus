# Explore With Me Plus

## О проекте

Explore With Me Plus - распределенное Spring Boot приложение для работы с событиями, пользователями, заявками на участие, локациями и рекомендательной системой.

Проект разделен на несколько Maven модулей:

- `core` - бизнес сервисы и общий модуль внутренних API.
- `infra` - инфраструктурные сервисы Spring Cloud.
- `stat` - сервисы рекомендательной системы и общие схемы обмена данными.

Основные технологии:

- Java 21.
- Spring Boot 3.3.2.
- Spring Cloud 2023.0.3.
- Spring Web.
- Spring Data JPA.
- PostgreSQL.
- Apache Kafka.
- Apache Avro.
- gRPC.
- Spring Cloud OpenFeign.
- Eureka Service Discovery.
- Spring Cloud Config.
- Spring Cloud Gateway.
- Resilience4j.

## Архитектура

Система состоит из бизнес сервисов, инфраструктурных сервисов и рекомендательной системы.

```text
                              +----------------+
                              |     Client     |
                              +-------+--------+
                                      |
                                      v
                         +--------------------------+
                         |      gateway-server      |
                         |          8080            |
                         +------------+-------------+
                                      |
          +---------------------------+---------------------------+
          |                           |                           |
          v                           v                           v
 +----------------+         +----------------+         +----------------+
 | event-service  |         | request-service|         |  user-service  |
 +--------+-------+         +--------+-------+         +----------------+
          |                           |
          |                           |
          |                           |
          v                           v
 +----------------+         +----------------+
 |     Collector  |         |  event-service |
 |      gRPC      |         |      Feign      |
 +--------+-------+         +----------------+
          |
          v
 +--------------------------+
 | Apache Kafka             |
 | stats.user-actions.v1    |
 +------------+-------------+
              |
              v
 +--------------------------+
 |       Aggregator         |
 | расчет сходства событий  |
 +------------+-------------+
              |
              v
 +-------------------------------+
 | Apache Kafka                  |
 | stats.events-similarity.v1    |
 +---------------+---------------+
                 |
                 v
 +-------------------------------+
 |            Analyzer           |
 | рекомендации и история        |
 +---------------+---------------+
                 |
       +---------+----------+
       |                    |
       v                    v
 +-----------+     +--------------------------+
 | PostgreSQL|     | Apache Kafka              |
 | stats DB  |     | stats.event-ratings.v1   |
 +-----------+     +------------+-------------+
                                |
                                v
                     +------------------------+
                     |    event-service       |
                     |      Event.rating      |
                     +------------------------+

 +--------------------+        +---------------------+
 | location-service   | -----> |    event-service    |
 +--------------------+ Feign  +---------------------+

 +--------------------+        +---------------------+
 | discovery-server   |        |    config-server    |
 |       8761         |        | Spring Cloud Config |
 +--------------------+        +---------------------+
```

### Инфраструктурные сервисы

`discovery-server`

Eureka Server. Хранит реестр сервисов и позволяет клиентам находить экземпляры сервисов по имени.

Класс запуска:

`infra/discovery-server/src/main/java/ru/yandex/practicum/DiscoveryServer.java`

Конфигурация:

`infra/discovery-server/src/main/resources/application.yml`

Основной порт:

`8761`

`config-server`

Spring Cloud Config Server. Раздает централизованные настройки из каталогов внутри classpath.

Класс запуска:

`infra/config-server/src/main/java/ru/yandex/practicum/ConfigServer.java`

Конфигурация самого сервера:

`infra/config-server/src/main/resources/application.yml`

Каталоги централизованной конфигурации:

`infra/config-server/src/main/resources/config/core/{application}/application.yaml`

`infra/config-server/src/main/resources/config/infra/{application}/application.yaml`

`infra/config-server/src/main/resources/config/stat/application.yaml`

`infra/config-server/src/main/resources/config/stat/{application}/application.yaml`

`gateway-server`

Spring Cloud Gateway. Принимает внешние HTTP запросы на порту `8080` и направляет их в бизнес сервисы через Eureka.

Класс запуска:

`infra/gateway-server/src/main/java/ru/yandex/practicum/Gateway.java`

Конфигурация запуска:

`infra/gateway-server/src/main/resources/application.yml`

Центральная конфигурация маршрутов:

`infra/config-server/src/main/resources/config/infra/gateway-server/application.yaml`

Основные маршруты:

- `/admin/categories/**` и `/categories/**` -> `event-service`.
- `/admin/compilations/**` и `/compilations/**` -> `event-service`.
- `/admin/events/**`, `/events/**` и `/users/*/events/**` -> `event-service`.
- `/users/*/requests/**` и `/users/*/events/*/requests/**` -> `request-service`.
- `/admin/users/**` -> `user-service`.
- `/admin/locations/**` -> `location-service`.

### Бизнес сервисы

`event-service`

Отвечает за:

- категории событий;
- события;
- подборки событий;
- административное и пользовательское управление событиями;
- публичный поиск событий;
- поиск событий по радиусу;
- регистрацию просмотров событий;
- лайки мероприятий;
- отображение рейтинга события;
- получение персональных рекомендаций.

Класс запуска:

`core/event-service/src/main/java/ru/practicum/explorewithme/EventServiceApp.java`

Локальная конфигурация:

`core/event-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/event-service/application.yaml`

`request-service`

Отвечает за заявки пользователей на участие в событиях.

Поддерживает:

- создание заявки;
- получение заявок пользователя;
- получение заявок на событие инициатором;
- отмену заявки;
- изменение статусов заявок;
- подсчет подтвержденных заявок для списка событий.

Класс запуска:

`core/request-service/src/main/java/ru/practicum/explorewithme/RequestServiceApp.java`

Локальная конфигурация:

`core/request-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/request-service/application.yaml`

`user-service`

Отвечает за пользователей.

Поддерживает:

- создание пользователей;
- получение списка пользователей;
- получение пользователя для внутренних запросов;
- получение нескольких пользователей по идентификаторам;
- удаление пользователя.

Класс запуска:

`core/user-service/src/main/java/ru/practicum/explorewithme/UserServiceApp.java`

Локальная конфигурация:

`core/user-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/user-service/application.yaml`

`location-service`

Отвечает за административные локации и получение событий в заданном радиусе.

Класс запуска:

`core/location-service/src/main/java/ru/practicum/explorewithme/LocationServiceApp.java`

Локальная конфигурация:

`core/location-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/location-service/application.yaml`

### Сервисы рекомендательной системы

`collector`

Принимает действия пользователей по gRPC и публикует их в Kafka.

Действия:

- `VIEW` - просмотр события;
- `REGISTER` - заявка на участие;
- `LIKE` - лайк события.

Класс запуска:

`stat/collector/src/main/java/ru/practicum/explorewithme/stats/collector/CollectorApplication.java`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/stat/collector/application.yaml`

`aggregator`

Читает действия пользователей из Kafka и рассчитывает сходство мероприятий.

Класс запуска:

`stat/aggregator/src/main/java/ru/practicum/explorewithme/stats/aggregator/AggregatorApplication.java`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/stat/aggregator/application.yaml`

`analyzer`

Читает действия пользователей и результаты расчета сходства, хранит историю взаимодействий и предоставляет gRPC API рекомендаций.

Также публикует рассчитанные рейтинги мероприятий в Kafka.

Класс запуска:

`stat/analyzer/src/main/java/ru/practicum/explorewithme/stats/analyzer/AnalyzerApplication.java`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/stat/analyzer/application.yaml`

## Общие модули

### interaction-api

Модуль:

`core/interaction-api`

Содержит контракты внутренних HTTP API и общие механизмы межсервисного взаимодействия.

Основные группы содержимого:

- DTO для обмена между core сервисами;
- Feign клиенты;
- fallback фабрики Feign клиентов;
- обработка ошибок внутренних HTTP вызовов;
- gRPC клиенты Collector и Analyzer;
- общие исключения и модели ошибок;
- общая конфигурация HTTP взаимодействия.

### stat/common

Модуль:

`stat/common`

Содержит общие компоненты рекомендательной системы.

Основной компонент:

`ActionWeightResolver` - получение весов действий из конфигурации.

### stat/serialization

Модуль:

`stat/serialization`

Содержит схемы обмена данными и код сериализации.

Подмодули:

- `stat/serialization/proto-schemas` - Proto схемы gRPC сервисов;
- `stat/serialization/avro-schemas` - Avro схемы сообщений Kafka и сериализация.

Proto файлы:

`stat/serialization/proto-schemas/src/main/protobuf/collector.proto`

`stat/serialization/proto-schemas/src/main/protobuf/dashboard.proto`

Avro схема:

`stat/serialization/avro-schemas/src/main/avro/stats.avdl`

## Взаимодействие сервисов

Основные зависимости между сервисами:

```text
 gateway-server
      |
      +-> event-service
      +-> request-service
      +-> user-service
      +-> location-service

 event-service -> user-service       Feign
 event-service -> request-service    Feign
 event-service -> collector          gRPC
 event-service -> analyzer           gRPC
 request-service -> user-service     Feign
 request-service -> event-service    Feign
 request-service -> collector        gRPC
 location-service -> event-service   Feign
```

Аналитический поток работает отдельно от основного пользовательского потока:

```text
 event-service + request-service
             |
             | gRPC
             v
          collector
             |
             v
           Kafka
             |
             v
         aggregator
             |
             v
           Kafka
             |
             v
          analyzer
             |
             +-> PostgreSQL
             |
             +-> Kafka event-ratings
                       |
                       v
                  event-service
```

### event-service -> user-service

Используется для получения данных инициаторов событий.

```text
GET /internal/users/{userId}
GET /internal/users?ids={id1}&ids={id2}
```

Для списка событий используется батч запрос по нескольким идентификаторам, чтобы избежать N+1 межсервисных вызовов.

### event-service -> request-service

Используется для получения количества подтвержденных заявок по списку событий.

```text
POST /internal/requests/confirmed-counts
```

Запрос содержит список идентификаторов событий. Ответ содержит пары `eventId` и `count`.

### event-service -> collector

Используется для передачи действий пользователя:

- просмотр события;
- лайк события.

Действия отправляются после проверки основной бизнес операции. Ошибка Collector не отменяет пользовательскую операцию.

### event-service -> analyzer

Используется для получения персональных рекомендаций и похожих событий.

Рейтинг события для обычных операций не запрашивается через Analyzer. Сохраненный рейтинг хранится в `event-service` и обновляется асинхронно через Kafka.

### request-service -> user-service

Используется для проверки существования пользователя и получения его кратких данных.

```text
GET /internal/users/{userId}
```

### request-service -> event-service

Используется для получения сведений о событии перед созданием и изменением заявки.

```text
GET /internal/events/{eventId}
```

В ответе доступны данные, необходимые для бизнес проверки:

- идентификатор события;
- идентификатор инициатора;
- состояние события;
- лимит участников;
- признак премодерации заявок.

### request-service -> collector

После создания заявки отправляется действие `REGISTER`.

Ошибка Collector не отменяет сохранение заявки.

### location-service -> event-service

`location-service` хранит административные локации.

Поиск событий по радиусу выполняется через внутренний API `event-service`:

```text
GET /internal/events/search-by-radius
```

Параметры:

- `lat` - широта центра поиска;
- `lon` - долгота центра поиска;
- `radius` - радиус поиска;
- `from` - смещение;
- `size` - размер страницы.

## Внутренний API

Контракты внутренних HTTP API находятся в:

`core/interaction-api/src/main/java/ru/practicum/explorewithme/interaction/api`

### API event-service

#### Получить данные события

```text
GET /internal/events/{eventId}
```

Ответ: `EventInternalDto`.

Поля:

```text
id
initiatorId
state
participantLimit
requestModeration
```

Основной потребитель: `request-service`.

#### Найти события по радиусу

```text
GET /internal/events/search-by-radius
```

Параметры:

```text
lat
lon
radius
from
size
```

Ответ: список `EventFullDto`.

Основной потребитель: `location-service`.

### API request-service

#### Получить количество подтвержденных заявок

```text
POST /internal/requests/confirmed-counts
```

Тело запроса:

```json
{
  "eventIds": [1, 2, 3]
}
```

Ответ:

```json
[
  {
    "eventId": 1,
    "count": 10
  },
  {
    "eventId": 2,
    "count": 5
  }
]
```

Основной потребитель: `event-service`.

### API user-service

#### Получить пользователя

```text
GET /internal/users/{userId}
```

Ответ: `UserShortDto`.

```text
id
name
```

#### Получить пользователей списком

```text
GET /internal/users?ids={id1}&ids={id2}
```

Ответ: список `UserShortDto`.

Основные потребители: `event-service`, `request-service`.

## gRPC API

### Collector

Proto схема:

`stat/serialization/proto-schemas/src/main/protobuf/collector.proto`

gRPC сервис: `UserActionController`.

Метод:

```text
CollectUserAction(UserActionProto) -> Empty
```

`UserActionProto` содержит:

```text
user_id
event_id
action_type
timestamp
```

### Analyzer

Proto схема:

`stat/serialization/proto-schemas/src/main/protobuf/dashboard.proto`

gRPC сервис: `RecommendationsController`.

Методы:

```text
GetRecommendationsForUser
GetSimilarEvents
GetInteractionsCount
```

Все методы возвращают поток `RecommendedEventProto`.

Клиенты находятся в:

`core/interaction-api/src/main/java/ru/practicum/explorewithme/interaction/grpc`

Для discovery используются адреса:

```text
discovery:///collector
discovery:///analyzer
```

В Eureka metadata сохраняется реальный gRPC порт сервисов.

## Kafka

Kafka используется для асинхронной передачи действий пользователей, результатов расчета сходства и рейтингов мероприятий.

Основной bootstrap адрес для локального запуска:

```text
localhost:9092
```

Топики:

- `stats.user-actions.v1` - действия пользователей;
- `stats.events-similarity.v1` - коэффициенты сходства мероприятий;
- `stats.event-ratings.v1` - рассчитанные рейтинги мероприятий.

### Поток действий

```text
VIEW / REGISTER / LIKE
          |
          v
      Collector
          |
          v
stats.user-actions.v1
          |
          +------> Aggregator
          |             |
          |             v
          |     stats.events-similarity.v1
          |             |
          +----------> Analyzer
                        |
                        v
                   PostgreSQL
```

`event-service` читает `stats.event-ratings.v1` и сохраняет рейтинг в поле `Event.rating`.

Kafka используется с подтверждением записи `acks=all` и идемпотентностью producer.

## Avro

Avro схемы находятся в:

`stat/serialization/avro-schemas/src/main/avro/stats.avdl`

Основной namespace:

`ru.practicum.ewm.stats.avro`

Сообщения рекомендательной системы включают:

- `UserActionAvro`;
- `EventSimilarityAvro`;
- `ActionTypeAvro`;
- сообщение рейтинга события.

## Алгоритм Aggregator

Aggregator поддерживает агрегированное состояние взаимодействий пользователей с мероприятиями.

Для каждого сочетания пользователь и мероприятие хранится максимальный вес действия пользователя.

Текущие веса действий:

```text
VIEW      = 0.4
REGISTER  = 0.8
LIKE      = 1.0
```

Для каждого мероприятия хранится сумма максимальных весов пользователей. Для каждой пары мероприятий хранится сумма минимальных весов общих пользователей.

Коэффициент сходства рассчитывается по формуле:

```text
S_min(A,B) / (sqrt(S_A) * sqrt(S_B))
```

Пара мероприятий хранится в каноническом порядке: сначала меньший идентификатор, затем больший.

При поступлении нового действия пересчет ограничивается затронутым мероприятием. Если максимальный вес действия пользователя не изменился, пересчет не требуется.

Пара мероприятия с самим собой не создается.

Класс с общими весами:

`stat/common/src/main/java/ru/practicum/explorewithme/stats/common/service/ActionWeightResolver.java`

Общие настройки весов:

`infra/config-server/src/main/resources/config/stat/application.yaml`

## Алгоритмы Analyzer

### Похожие мероприятия

Analyzer получает коэффициенты сходства с указанным мероприятием, исключает мероприятия, с которыми пользователь уже взаимодействовал, сортирует кандидатов по коэффициенту и возвращает первые N.

### Персональные рекомендации

1. Из истории пользователя выбираются взаимодействия с мероприятиями.
2. Для просмотренных пользователем мероприятий находятся похожие мероприятия.
3. Из кандидатов исключаются уже известные пользователю мероприятия.
4. Для кандидатов рассчитывается взвешенная оценка.
5. Кандидаты сортируются по оценке.

### История взаимодействий

Analyzer хранит для пары `user_id` и `event_id` максимальный вес действия пользователя и время последнего события.

Данные используются для расчета рекомендаций, поиска похожих мероприятий и получения агрегированных оценок взаимодействий.

## Рейтинг мероприятий

Analyzer рассчитывает рейтинг события на основе накопленных взаимодействий пользователей и публикует изменения в:

`stats.event-ratings.v1`

`event-service` получает эти сообщения асинхронно и сохраняет значение в поле:

```text
Event.rating
```

Рейтинг хранится в базе `event-service`, поэтому операции чтения событий и сортировки с пагинацией по рейтингу не требуют синхронного вызова Analyzer.

## Просмотры и лайки

При запросе:

```text
GET /events/{eventId}
```

передается заголовок:

```text
X-EWM-USER-ID
```

`event-service` сохраняет факт просмотра в таблице `event_views` и отправляет действие `VIEW` в Collector.

Таблица хранит уникальную пару пользователя и события, поэтому повторные просмотры одного события одним пользователем не создают дубликаты для проверки права на лайк.

Лайк выполняется через:

```text
PUT /events/{eventId}/like
```

Лайк разрешен только пользователю, который ранее просматривал событие.

### Отказоустойчивость статистики

Collector и Analyzer не являются критическими зависимостями для обычных операций core сервисов.

При недоступности Collector:

- событие продолжает сохраняться;
- пользователь продолжает работать с событиями;
- заявка продолжает сохраняться;
- лайк продолжает обрабатываться;
- статистика не отправляется.

При недоступности Analyzer:

- обычное получение событий продолжает работать;
- последний сохраненный рейтинг остается доступным в `event-service`;
- рекомендации временно возвращаются как пустой результат;
- похожие мероприятия временно возвращаются как пустой результат.

Для gRPC клиентов используются Circuit Breaker и gRPC deadline.

Для Feign клиентов используются Circuit Breaker и fallback фабрики.

## Отказоустойчивость внутренних HTTP вызовов

Feign клиенты находятся в:

`core/interaction-api/src/main/java/ru/practicum/explorewithme/interaction/feign`

Используемые клиенты:

- `event-service`;
- `request-service`;
- `user-service`.

Fallback фабрики отделяют недоступность сервиса от его бизнес ошибок.

Основные параметры Resilience4j находятся в центральных конфигурациях:

```text
resilience4j.retry
resilience4j.circuitbreaker
```

Внутренние HTTP запросы используют Eureka для поиска адресов сервисов по имени.

## Конфигурация

Архитектура конфигурации состоит из локального файла запуска и централизованных настроек Config Server.

### Локальная конфигурация запуска

Локальные файлы содержат имя приложения, подключение к Config Server и Eureka.

Основные файлы:

```text
core/event-service/src/main/resources/application.yaml
core/request-service/src/main/resources/application.yaml
core/user-service/src/main/resources/application.yaml
core/location-service/src/main/resources/application.yaml
stat/collector/src/main/resources/application.yaml
stat/aggregator/src/main/resources/application.yaml
stat/analyzer/src/main/resources/application.yaml
infra/gateway-server/src/main/resources/application.yml
infra/discovery-server/src/main/resources/application.yml
infra/config-server/src/main/resources/application.yml
```

### Центральная конфигурация

Основная конфигурация хранится в:

`infra/config-server/src/main/resources/config`

Каталоги:

```text
config/core
config/infra
config/stat
```

В конфигурации сервисов находятся:

- порты;
- PostgreSQL параметры;
- Kafka параметры;
- gRPC параметры;
- Eureka параметры;
- OpenFeign параметры;
- Resilience4j параметры;
- настройки рекомендательной системы.

### Порты локального запуска

Основные фиксированные порты:

```text
Gateway        8080
Eureka         8761
Kafka          9092
```

HTTP порты бизнес сервисов, Collector, Aggregator, Analyzer и Config Server выбираются Spring автоматически, если в конфигурации указано `server.port: 0`.

gRPC клиенты используют Eureka discovery и metadata с реальными gRPC портами.

### Базы данных

Core сервисы используют PostgreSQL базу:

```text
ewm_main_db
```

В локальной конфигурации база доступна через порт `5433`.

Основные таблицы:

- `event-service` - `categories`, `events`, `compilations`, `compilation_events`, `event_views`;
- `request-service` - `participation_requests`;
- `user-service` - `users`;
- `location-service` - `admin_locations`.

Analyzer использует отдельную PostgreSQL базу:

```text
ewm_stats_db
```

В локальной конфигурации база доступна через порт `5432`.

Основные таблицы Analyzer:

- `user_event_interactions`;
- `event_similarity`.

SQL схемы находятся рядом с сервисами:

```text
core/event-service/src/main/resources/schema.sql
core/request-service/src/main/resources/schema.sql
core/user-service/src/main/resources/schema.sql
core/location-service/src/main/resources/schema.sql
stat/analyzer/src/main/resources/schema.sql
```

## Внешний API

Основная спецификация внешнего API находится в файле:

`ewm-main-service-spec.json`

Основные группы внешних endpoint:

```text
/admin/users
/admin/categories
/admin/compilations
/admin/events
/admin/locations

/users/{userId}/events
/users/{userId}/requests
/users/{userId}/events/{eventId}/requests

/categories
/compilations
/events
/events/recommendations
/events/{eventId}
/events/{eventId}/like
```

Для приватных и пользовательских операций идентификатор пользователя передается в URL или HTTP заголовке в соответствии с контрактом конкретного endpoint.

## Структура проекта

```text
.
|-- pom.xml
|-- ewm-main-service-spec.json
|
|-- core
|   |-- pom.xml
|   |-- event-service
|   |-- request-service
|   |-- user-service
|   |-- location-service
|   `-- interaction-api
|
|-- infra
|   |-- pom.xml
|   |-- config-server
|   |-- discovery-server
|   `-- gateway-server
|
`-- stat
    |-- pom.xml
    |-- collector
    |-- aggregator
    |-- analyzer
    |-- common
    `-- serialization
        |-- avro-schemas
        `-- proto-schemas
```

Для исходного кода бизнес сервисов используется разделение на controller, service, repository, model, dto и mapper.

Межсервисные HTTP контракты сосредоточены в `interaction-api`, а Proto и Avro схемы находятся в модуле `stat/serialization`.

## Сборка и запуск

Для локального запуска инфраструктура поднимается в следующем порядке:

```text
1. PostgreSQL
2. Kafka
3. discovery-server
4. config-server
5. gateway-server
6. core сервисы
7. stat сервисы
```

После запуска gateway доступен на:

```text
http://localhost:8080
```

Eureka доступна на:

```text
http://localhost:8761
```
