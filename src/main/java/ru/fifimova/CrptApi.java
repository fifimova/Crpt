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
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final long sleepTimeMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("The request limit cannot be negative");
        }
        this.sleepTimeMillis = timeUnit.toMillis(1) / requestLimit;
    }

    public void sendDocument(String jsonDocument, String signature) throws IOException, InterruptedException {
        Thread.sleep(sleepTimeMillis);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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