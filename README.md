# Explore With Me Plus

## О проекте

Explore With Me Plus - распределенное Spring Boot приложение для работы с событиями, пользователями, заявками на участие, локациями и статистикой просмотров.

Проект разделен на несколько Maven модулей:

- `core` - бизнес сервисы и общий модуль внутренних API.
- `infra` - инфраструктурные сервисы Spring Cloud.
- `stat` - сервис статистики.

Основные технологии:

- Java 21.
- Spring Boot 3.3.2.
- Spring Cloud 2023.0.3.
- Spring Web.
- Spring Data JPA.
- PostgreSQL.
- Spring Cloud OpenFeign.
- Eureka Service Discovery.
- Spring Cloud Config.
- Spring Cloud Gateway.
- Resilience4j.

## Архитектура

В системе используются отдельные сервисы с собственными зонами ответственности.

```text
                         +----------------------+
                         |      Client          |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |    gateway-server    |
                         |       port 8080      |
                         +----------+-----------+
                                    |
                +-------------------+-------------------+
                |                   |                   |
                v                   v                   v
       +----------------+  +----------------+  +----------------+
       | event-service  |  | request-service|  |  user-service  |
       +-------+--------+  +--------+-------+  +----------------+
               |                    |
               |                    |
               |              +-----+-----+
               |              |           |
               v              v           v
       +---------------+  +----------+  +----------+
       |  stat-server  |  |  event   |  |   user   |
       +---------------+  | service  |  | service  |
                          +----------+  +----------+

       +----------------+
       |location-service|
       +-------+--------+
               |
               v
       +----------------+
       | event-service  |
       +----------------+

       +----------------+       +----------------+
       | discovery-server|      |  config-server |
       |    port 8761    |      | Spring Config  |
       +----------------+       +----------------+
```

### Инфраструктурные сервисы

`discovery-server`

Eureka Server. Хранит реестр зарегистрированных сервисов и позволяет клиентам находить сервисы по имени.

Конфигурация:

`infra/discovery-server/src/main/resources/application.yml`

Основной порт:

`8761`

`config-server`

Spring Cloud Config Server. Раздает централизованные настройки сервисов из каталогов внутри classpath.

Конфигурация самого сервера:

`infra/config-server/src/main/resources/application.yml`

Каталоги конфигурации:

`infra/config-server/src/main/resources/config/core/{application}/application.yaml`

`infra/config-server/src/main/resources/config/infra/{application}/application.yaml`

`infra/config-server/src/main/resources/config/stat/{application}/application.yaml`

`gateway-server`

Spring Cloud Gateway. Принимает внешние HTTP запросы на порту `8080` и направляет их в нужный сервис через балансировку `lb://...`.

Конфигурация:

`infra/gateway-server/src/main/resources/application.yml`

Центральная конфигурация маршрутов:

`infra/config-server/src/main/resources/config/infra/gateway-server/application.yaml`

Маршруты:

- `/admin/categories/**`, `/categories/**`, `/admin/compilations/**`, `/compilations/**`, `/admin/events/**`, `/events/**`, `/users/*/events/**` -> `event-service`
- `/users/*/requests/**`, `/users/*/events/*/requests/**` -> `request-service`
- `/admin/users/**` -> `user-service`
- `/admin/locations/**` -> `location-service`

### Бизнес сервисы

`event-service`

Отвечает за:

- категории;
- события;
- подборки событий;
- публичное и административное управление событиями;
- обогащение данных события информацией о пользователе, заявках и просмотрах.

Класс запуска:

`core/event-service/src/main/java/ru/practicum/explorewithme/EventServiceApp.java`

Конфигурация загрузки:

`core/event-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/event-service/application.yaml`

`request-service`

Отвечает за заявки пользователей на участие в событиях.

Класс запуска:

`core/request-service/src/main/java/ru/practicum/explorewithme/RequestServiceApp.java`

Конфигурация загрузки:

`core/request-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/request-service/application.yaml`

`user-service`

Отвечает за пользователей.

Класс запуска:

`core/user-service/src/main/java/ru/practicum/explorewithme/UserServiceApp.java`

Конфигурация загрузки:

`core/user-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/user-service/application.yaml`

`location-service`

Отвечает за административные локации и поиск событий в заданном радиусе.

Класс запуска:

`core/location-service/src/main/java/ru/practicum/explorewithme/LocationServiceApp.java`

Конфигурация загрузки:

`core/location-service/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/core/location-service/application.yaml`

`stat-server`

Отвечает за запись хитов и получение статистики по URI.

Класс запуска:

`stat/stat-server/src/main/java/ru/practicum/explorewithme/stats/StatServerApp.java`

Конфигурация загрузки:

`stat/stat-server/src/main/resources/application.yaml`

Центральная конфигурация:

`infra/config-server/src/main/resources/config/stat/stat-server/application.yaml`

### Общий модуль interaction-api

Модуль:

`core/interaction-api`

Содержит единые контракты внутренних HTTP API:

`core/interaction-api/src/main/java/ru/practicum/explorewithme/interaction/api`

Также здесь находятся:

- DTO для обмена между сервисами;
- Feign клиенты;
- fallback фабрики;
- обработка ошибок Feign;
- общая конфигурация Feign.

Сервисы используют `@FeignClient` с именами:

- `event-service`
- `request-service`
- `user-service`
- `stat-server`

Благодаря Eureka адрес сервиса определяется по имени, поэтому в бизнес коде не задаются фиксированные адреса и порты.

## Взаимодействие сервисов

Основные зависимости между сервисами:

```text
event-service -> user-service
event-service -> request-service
event-service -> stat-server

request-service -> user-service
request-service -> event-service

location-service -> event-service
```

### event-service -> user-service

Используется для получения данных инициаторов событий.

```text
GET /internal/users/{userId}
GET /internal/users?ids={id1}&ids={id2}
```

Для списков используется батч запрос по нескольким идентификаторам, чтобы не делать отдельный HTTP вызов для каждого события.

### event-service -> request-service

Используется для получения количества подтвержденных заявок по списку событий.

```text
POST /internal/requests/confirmed-counts
```

Запрос передает список `eventIds`, ответ содержит пары `eventId` и `count`.

Батч API используется для уменьшения количества межсервисных запросов при выдаче списка событий.

### event-service -> stat-server

Используется в двух сценариях:

- запись факта обращения к публичным URI;
- получение числа просмотров событий.

```text
POST /hit
GET /stats
```

При запросе публичных событий `event-service` отправляет в `stat-server` данные о URI, IP и времени запроса.

### request-service -> user-service

Используется для проверки существования пользователя и получения его кратких данных.

```text
GET /internal/users/{userId}
```

### request-service -> event-service

Используется для получения сведений о событии перед созданием или изменением заявки.

```text
GET /internal/events/{eventId}
```

В ответе доступны данные, необходимые для бизнес проверки:

- идентификатор события;
- идентификатор инициатора;
- состояние события;
- лимит участников;
- признак модерации заявок.

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

Контракты внутренних API находятся в модуле:

`core/interaction-api`

### API event-service

#### Получить данные события для других сервисов

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

#### Получить число подтвержденных заявок

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

### API stat-server

Контракт находится в общем модуле `interaction-api` в интерфейсе `StatsApi`.

#### Записать хит

```text
POST /hit
```

Тело содержит:

```text
app
uri
ip
timestamp
```

Дата и время передаются в формате:

```text
yyyy-MM-dd HH:mm:ss
```

#### Получить статистику

```text
GET /stats
```

Параметры:

```text
start
end
uris
unique
```

Ответ содержит:

```text
app
uri
hits
```

`event-service` использует `unique=true` для получения числа уникальных просмотров.

## Отказоустойчивость внутренних вызовов

Feign клиенты объявлены в:

`core/interaction-api/src/main/java/ru/practicum/explorewithme/interaction/feign`

Для клиентов используются fallback фабрики.

Основные настройки находятся в центральных конфигурациях сервисов:

```text
resilience4j.retry
resilience4j.circuitbreaker
resilience4j.timelimiter
```

Текущие общие параметры:

- до 3 попыток вызова;
- начальная пауза между попытками 1 секунда;
- exponential backoff с множителем 2;
- окно circuit breaker 50 вызовов;
- порог ошибок 50 процентов;
- время open состояния 10 секунд;
- до 5 пробных вызовов в half-open состоянии;
- общий timeout timelimiter 5 секунд.

Поведение fallback зависит от сервиса:

- недоступность `stat-server` не отменяет основной пользовательский запрос, а просмотры считаются равными 0;
- недоступность `request-service` при обогащении событий приводит к нулевому числу подтвержденных заявок;
- недоступность `user-service` допускает выдачу заглушки для одного пользователя или пустого списка для батч запроса;
- недоступность `event-service` при запросе из `request-service` приводит к `ServiceUnavailableException`, а поиск событий по радиусу для `location-service` возвращает пустой список.

HTTP 404 от внутренних сервисов преобразуется Feign decoder в `NotFoundException` и обрабатывается отдельно от недоступности сервиса.

## Конфигурация

Архитектура конфигурации состоит из двух уровней.

### Локальная конфигурация запуска

Каждый сервис содержит локальный файл `application.yaml`, где задаются:

- имя приложения;
- подключение к config-server;
- включение поиска config-server через Eureka;
- подключение к Eureka.

Примеры:

```text
core/event-service/src/main/resources/application.yaml
core/request-service/src/main/resources/application.yaml
core/user-service/src/main/resources/application.yaml
core/location-service/src/main/resources/application.yaml
stat/stat-server/src/main/resources/application.yaml
infra/gateway-server/src/main/resources/application.yml
infra/discovery-server/src/main/resources/application.yml
infra/config-server/src/main/resources/application.yml
```

### Центральная конфигурация

Бизнес настройки сервисов хранятся в `config-server`.

```text
infra/config-server/src/main/resources/config/core/event-service/application.yaml
infra/config-server/src/main/resources/config/core/request-service/application.yaml
infra/config-server/src/main/resources/config/core/user-service/application.yaml
infra/config-server/src/main/resources/config/core/location-service/application.yaml
infra/config-server/src/main/resources/config/stat/stat-server/application.yaml
infra/config-server/src/main/resources/config/infra/gateway-server/application.yaml
```

В этих файлах находятся:

- порты;
- параметры PostgreSQL;
- настройки JPA;
- настройки OpenFeign;
- timeout;
- retry;
- circuit breaker;
- timelimiter;
- маршруты gateway.

В текущей конфигурации порты бизнес сервисов заданы как `0`, поэтому Spring выбирает свободный порт. Доступ к ним выполняется через Eureka по имени сервиса.

### Базы данных

Core сервисы используют PostgreSQL базу `ewm_main_db`.

Логическое разделение данных реализовано по таблицам:

- `event-service` - `categories`, `events`, `compilations`, `compilation_events`;
- `request-service` - `participation_requests`;
- `user-service` - `users`;
- `location-service` - `admin_locations`.

`stat-server` использует отдельную PostgreSQL базу `ewm_stats_db` и таблицу `hits`.

SQL схемы находятся рядом с исходным кодом соответствующих сервисов:

```text
core/event-service/src/main/resources/schema.sql
core/request-service/src/main/resources/schema.sql
core/user-service/src/main/resources/schema.sql
core/location-service/src/main/resources/schema.sql
stat/stat-server/src/main/resources/schema.sql
```

## Внешний API

Основная спецификация внешнего API проекта:

[ewm-main-service-spec.json](https://github.com/dimitrievdmit/java-explore-with-me-plus/blob/main/ewm-main-service-spec.json)

Спецификация API сервиса статистики находится в проекте:

`ewm-stats-service-spec.json`

## Структура проекта

```text
.
|-- pom.xml
|-- ewm-main-service-spec.json
|-- ewm-stats-service-spec.json
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
    `-- stat-server
```

Для исходного кода бизнес сервисов используется обычное разделение на controller, service, repository, model, dto и mapper.

В модуле `interaction-api` дополнительно сосредоточены общие контракты и механизмы межсервисного взаимодействия.
