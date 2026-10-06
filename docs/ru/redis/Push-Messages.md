Push-сообщения Redis (Pub/Sub)
==============================

[English version](../../en/redis/Push-Messages.md)

Помимо изменений ключей, фреймворк перехватывает push-сообщения Redis Pub/Sub: команды
`PUBLISH` и `SPUBLISH`, замеченные в потоке репликации, превращаются в сообщения `RedisPush`,
поэтому их можно проверять в `<outboundMessage>` и отправлять из теста через
`<inboundMessage>`.

Как это работает
----------------

1. Фоновый механизм репликации модуля (тот же, что следит за ключами) видит все команды
   `PUBLISH`/`SPUBLISH` подключения.
2. Команда превращается в сообщение `RedisPush` с атрибутом `channel` и десериализованным
   значением `value` и сохраняется для проверок.
3. Push, отправленный самим тестом, публикуется в Redis и отсеивается из захваченных
   сообщений по своей транспортной идентичности (см. [Дедупликация](#дедупликация)).

Push-сообщения захватываются с момента, когда поток репликации активен, включая опубликованные
до старта первого теста (например, внутренними механизмами приложения или библиотек при
запуске). Технические каналы, которые не должны попадать в проверки, скрываются через
`ignore: true` (см. [Игнорирование шума](#игнорирование-шума)).

Направление 1: приложение публикует push
----------------------------------------

Вызовите метод приложения, который публикует сообщение, и опишите ожидаемый push в
`<outboundMessage>`:

```xml
<test type="Case" name="Сервис заказов публикует уведомление">
    <bean>orderService</bean>
    <method>complete</method>
    <request>order-1</request>

    <outboundMessage type="RedisPush" connection="redissonConnectionFactory" channel="orders.events">
        <value type="OrderEvent">
            <orderId>order-1</orderId>
            <status>COMPLETED</status>
        </value>
    </outboundMessage>
</test>
```

`connection` — имя бина `RedisConnectionFactory`, на котором замечен push. Если несколько
фабрик смотрят в один Redis, один и тот же push захватывается по разу на каждую фабрику;
технические каналы помечайте `ignore: true` (см. ниже), если не хотите проверять каждую копию.

Направление 2: тест отправляет push приложению
----------------------------------------------

Используйте `<inboundMessage>`, чтобы опубликовать push в Redis. Приложение обработает его
как обычное Pub/Sub сообщение; эхо собственного push теста никогда не попадает в
`<outboundMessage>`:

```xml
<test type="Case" name="Приложение обрабатывает входящий push">
    <test type="Part" name="Отправка">
        <inboundMessage type="RedisPush" connection="redissonConnectionFactory" channel="orders.in">
            <value type="OrderEvent">
                <orderId>order-2</orderId>
                <status>NEW</status>
            </value>
        </inboundMessage>
    </test>
    <test type="Part" name="SUT подтвердил">
        <bean>pushTestFacade</bean>
        <method>awaitOrder</method>
        <response type="OrderEvent">
            <orderId>order-2</orderId>
            <status>NEW</status>
        </response>
    </test>
</test>
```

Приложение обрабатывает push асинхронно, поэтому фреймворк не знает, когда сторона SUT
закончила. Если тесту нужно увидеть реакцию SUT, дожидайтесь её явно (как в примере выше).

Сериализация каналов (`schemas`)
--------------------------------

Каналы матчатся теми же схемами, что и ключи: действуют те же правила longest-prefix,
`defaultSchema` и записи `schemas.*` (см.
[Configuration.md](Configuration.md#разрешение-паттернов-ключей-longest-prefix-match)).
Имя канала кодируется и декодируется кодеком `keyCodec` подключения — так же, как имена
ключей. Полезная нагрузка сериализуется и десериализуется кодеком `value` подходящей схемы;
`ignore` и `excludedFields` тоже работают:

```yaml
integration:
  testing:
    redis:
      connections:
        redisConnectionFactory:
          defaultSchema:
            value:
              type: string      # каналы, не попавшие ни под один префикс
          schemas:
            "orders.events":
              value:
                bean-ref: orderEventSerializer
            "internal.":
              ignore: true      # технические каналы не захватываются
```

`excludedFields` подходящей схемы применяются к `value` push-сообщения.

Дедупликация
------------

Захваченный push отсеивается, если его идентичность совпадает с push-сообщением,
отправленным тестом. Идентичность состоит из:

* подключения (имя бина фабрики),
* канала,
* сырых байтов полезной нагрузки.

Идентичности не поглощаются: один push, видимый через несколько транспортов, отсеивается
везде. Обратная сторона: если SUT в рамках одного теста опубликует ровно ту же нагрузку в
тот же канал, её невозможно отличить от эха — она тоже будет отсеяна. Аналога
`ignore-inbound` для Redis нет: собственные push-сообщения теста скрываются всегда.

SPUBLISH
--------

Шардированные push-сообщения (`SPUBLISH`) захватываются так же и отображаются обычными
`RedisPush`; отдельного типа сообщения нет.

Ошибки десериализации
---------------------

Если полезную нагрузку не удалось десериализовать кодеком `value` канала, push всё равно
захватывается: сырые байты сохраняются в `<value type="byte[]">` (Base64 в XML), а ошибка
описывается в `<exception>`:

```xml
<outboundMessage type="RedisPush" connection="redissonConnectionFactory" channel="orders.events">
    <value type="byte[]">eyJvcmRlcklkIjoib3JkZXItMSJ9</value>
    <exception>
        <exceptionType>IllegalArgumentException</exceptionType>
        <message>...</message>
    </exception>
</outboundMessage>
```

Ожидание
--------

Репликация ожидается через подсистему wait-completion: после инициализации (чтобы снапшоты и
диффы хранилищ читались из догнанного зеркала) и после действия теста (чтобы push-сообщения и
изменения были захвачены до проверок). Для этого модуль публикует барьерный токен в технический
канал и ждёт, пока поток репликации догонит Redis. Сам барьерный канал
(`__integration_testing_sync__`) никогда не захватывается; не используйте его в приложении.
Для этого нужно `integration.testing.wait.completion.enabled=true` (уже задано в
`redis.properties`); при явном `false` модуль не ожидает репликацию.

Игнорирование шума
------------------

Технические каналы (например, внутренние каналы Redisson) нужно исключать через
`ignore: true` для соответствующего префикса; игнорируемые каналы не захватываются и не
попадают в `<outboundMessage>`. Если неожиданный push пропал из проверок или, наоборот,
появился в них, см.
[Troubleshooting.md](Troubleshooting.md#проблема-push-сообщения-не-попадают-в-outboundmessage).

---
[← На главную](../README.md)
