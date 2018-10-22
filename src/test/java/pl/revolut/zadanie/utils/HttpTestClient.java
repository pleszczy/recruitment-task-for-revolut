package pl.revolut.zadanie.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class HttpTestClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpTestClient(ExecutorService executorService) {
        httpClient = createHttpClient(executorService);
        objectMapper = new ObjectMapper();
    }

    public String get(String url) throws IOException, InterruptedException {
        return httpClient.send(getRequest(url), BodyHandlers.ofString()).body();
    }

    public <T> T get(String url, Class<T> clazz) throws IOException, InterruptedException {
        String response = httpClient.send(getRequest(url), BodyHandlers.ofString()).body();
        return objectMapper.readValue(response, clazz);
    }

    public String post(String url, String body) throws IOException, InterruptedException {
        return httpClient.send(postRequest(url, body), BodyHandlers.ofString()).body();
    }

    public String delete(String url) throws IOException, InterruptedException {
        return httpClient.send(deleteRequest(url), BodyHandlers.ofString()).body();
    }

    private HttpRequest getRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();
    }

    private HttpRequest deleteRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
    }

    private HttpRequest postRequest(String url, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
    }

    private HttpClient createHttpClient(Executor executor) {
        return HttpClient.newBuilder()
                .executor(executor)
                .build();
    }
}
