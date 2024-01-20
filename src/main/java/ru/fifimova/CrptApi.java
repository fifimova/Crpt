package ru.fifimova;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Клиент для работы с API Честного знака.
 * Позволяет отправлять документы и создавать их в формате JSON.
 */
public class CrptApi {

    private static final Logger log = Logger.getLogger(CrptApi.class.getName());
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;

    /**
     * Создает экземпляр CrptApi, который реализует ограничение на количество запросов к API.
     *
     * @param timeUnit     Единица измерения времени, используемая для определения интервала сброса разрешений.
     * @param duration     Длительность интервала времени, после которого семафор будет сбрасывать разрешения.
     *                     Значение указывается в соответствии с выбранной единицей измерения времени.
     * @param requestLimit Максимальное количество разрешений (запросов), доступных для отправки за один интервал времени.
     *                     <p>
     *                     <p>
     *                     Конструктор настраивает семафор с указанным количеством разрешений и планирует периодический сброс этих разрешений.
     *                     Периодический сброс обеспечивается с помощью ScheduledExecutorService, который освобождает указанное количество
     *                     разрешений семафора с заданной периодичностью. Это позволяет ограничить количество запросов к API, выполняемых
     *                     за определенный интервал времени.
     */
    public CrptApi(TimeUnit timeUnit, long duration, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                0, duration, timeUnit);
    }

    /**
     * Отправляет документ к API, управляя ограничениями доступа через семафор.
     * Этот метод приостанавливает выполнение, если количество одновременных запросов достигло своего предела.
     *
     * @param jsonDocument JSON представление документа для отправки.
     * @param signature    Подпись документа.
     * @throws InterruptedException если поток был прерван во время ожидания разрешения семафора.
     */
    public void sendDocument(String jsonDocument, String signature) {
        try {
            semaphore.acquire();
            log.info("Acquired a permit, proceeding with the request");
            executeHttpRequest(jsonDocument, signature);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to send a document", e);
        }
    }

    /**
     * Выполняет HTTP-запрос к API.
     *
     * @param jsonDocument JSON представление документа для отправки.
     * @param signature    Подпись для аутентификации запроса.
     * @throws InterruptedException если поток был прерван во время отправки запроса.
     */
    private void executeHttpRequest(String jsonDocument, String signature) throws InterruptedException {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = buildHttpRequest(jsonDocument, signature);
            log.info("Sending document to API");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response);
        } catch (IOException e) {
            log.severe("IOException occurred: " + e.getMessage());
        }
    }

    private HttpRequest buildHttpRequest(String jsonDocument, String signature) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();
    }

    private void handleResponse(HttpResponse<String> response) {
        if (response.statusCode() == 200) {
            log.info("Response received successfully");
        } else {
            log.warning("Response with status code: " + response.statusCode());
        }
    }


    public String createJsonDocument(Document document) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(document);
    }

    @Getter
    public static class Document {

        @NotEmpty
        private Description description;
        @NotNull
        private String docId;
        @NotNull
        private String docStatus;
        @NotNull
        private String docType;
        @NotNull
        private Boolean importRequest;
        @NotNull
        private String ownerInn;
        @NotNull
        private String producerInn;
        @NotNull
        private String productionDate;
        @NotNull
        private String productionType;
        @NotEmpty
        private List<Product> products;
        @NotNull
        private String regDate;
        @NotNull
        private String regNumber;
    }

    @Getter
    @Setter
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    public static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }
}

