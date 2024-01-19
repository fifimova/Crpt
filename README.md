# CrptApi Java Client

## Описание
CrptApi - техническое задание, цель которого - создание Java клиента для взаимодействия с API Честного знака. Этот клиент позволяет отправлять документы и создавать их в формате JSON. 
Реализация включает механизм контроля частоты запросов с использованием `Thread.sleep()`.

## Основные характеристики
- **Ограничение запросов**: Класс `CrptApi` реализует ограничение количества запросов к API в рамках заданного временного интервала.
- **Thread-Safe**: Реализация учитывает многопоточное использование, обеспечивая корректную работу в многопоточной среде.
- **Гибкая настройка**: Пользователь может задать временной интервал и лимит запросов при создании экземпляра класса.

## Как это работает
Клиент использует `Semaphore` для контроля количества одновременных запросов. При достижении лимита запросов дополнительные запросы блокируются до тех пор, пока не будет доступно новое разрешение для их выполнения.

```java
public class CrptApi {
    private final Semaphore requestSemaphore;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestSemaphore = new Semaphore(requestLimit);
        // Настройка периодического сброса семафора
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> 
            requestSemaphore.release(requestLimit - requestSemaphore.availablePermits()), 
            0, 1, timeUnit);
    }
```


## Как использовать
Для использования `CrptApi` необходимо создать экземпляр класса с указанием временного интервала и лимита запросов. Например:

```java
CrptApi api = new CrptApi(TimeUnit.SECONDS, 5); // 5 запросов в секунду
```

## Отправка документа
Для отправки документа используйте метод sendDocument, передавая JSON-документ и подпись:

```java
String jsonDocument = "..."; // Ваш JSON-документ
String signature = "..."; // Ваша подпись
api.sendDocument(jsonDocument, signature);
```

## Классы данных
- Document: Основной класс для представления документа.
- Description: Класс описания документа.
- Product: Класс продукта в документе.

## Зависимости
- Jakarta Bean Validation API
- Jackson для сериализации/десериализации JSON
- Lombok для сокращения бойлерплейт-кода
