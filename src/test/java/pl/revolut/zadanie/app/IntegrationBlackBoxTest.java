package pl.revolut.zadanie.app;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.revolut.zadanie.Main;
import pl.revolut.zadanie.utils.HttpTestClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationBlackBoxTest {
    private static App app;
    private static HttpTestClient httpClient;
    private static ExecutorService cachedThreadPool;
    private static ExecutorService fixedThreadPool;

    @BeforeAll
    public static void beforeAll() {
        app = Main.manualDependencyInjectionApp();
        app.start();
        cachedThreadPool = Executors.newCachedThreadPool();
        fixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
        httpClient = new HttpTestClient(cachedThreadPool);
    }

    @AfterAll
    public static void afterAll() {
        cachedThreadPool.shutdownNow();
        fixedThreadPool.shutdownNow();
        app.close();
    }

    @AfterEach
    public void afterEach() throws IOException, InterruptedException {
        httpClient.delete("http://localhost:8080/accounts/A");
        httpClient.delete("http://localhost:8080/accounts/B");
    }

    @Test
    public void should_correctly_transfer_founds_between_accounts() throws IOException, InterruptedException {
        httpClient.post("http://localhost:8080/accounts", "{\"iban\":\"A\",\"balance\":\"75\"}");
        httpClient.post("http://localhost:8080/accounts", "{\"iban\":\"B\",\"balance\":\"75\"}");

        httpClient.get("http://localhost:8080/accounts/transfer/75/from/A/to/B");

        AccountDto accountA = httpClient.get("http://localhost:8080/accounts/A", AccountDto.class);
        AccountDto accountB = httpClient.get("http://localhost:8080/accounts/B", AccountDto.class);
        assertEquals(0, accountA.balance());
        assertEquals(150, accountB.balance());
    }

    @Test
    public void should_fail_to_transfer_founds_between_accounts_if_there_are_not_enough_founds() throws IOException, InterruptedException {
        httpClient.post("http://localhost:8080/accounts", "{\"iban\":\"A\",\"balance\":\"75\"}");
        httpClient.post("http://localhost:8080/accounts", "{\"iban\":\"B\",\"balance\":\"75\"}");

        httpClient.get("http://localhost:8080/accounts/transfer/150/from/A/to/B");

        AccountDto accountA = httpClient.get("http://localhost:8080/accounts/A", AccountDto.class);
        AccountDto accountB = httpClient.get("http://localhost:8080/accounts/B", AccountDto.class);
        assertEquals(75, accountA.balance());
        assertEquals(75, accountB.balance());
    }

    @Test
    public void should_transfer_between_accounts_atomically_and_with_no_deadlocks() throws ExecutionException, InterruptedException, IOException {
        httpClient.post("http://localhost:8080/accounts", "{\"iban\":\"A\",\"balance\":\"1001\"}");
        httpClient.post("http://localhost:8080/accounts", "{\"iban\":\"B\",\"balance\":\"1001\"}");

        CompletableFuture.allOf(IntStream.range(0, 10_000)
                .mapToObj(
                        value -> List.of(
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        httpClient.get("http://localhost:8080/accounts/transfer/9/from/A/to/B");
                                        httpClient.get("http://localhost:8080/accounts/transfer/9/from/B/to/A");
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }, fixedThreadPool)
                                , CompletableFuture.runAsync(() -> {
                                    try {
                                        httpClient.get("http://localhost:8080/accounts/transfer/9/from/B/to/A");
                                        httpClient.get("http://localhost:8080/accounts/transfer/9/from/A/to/B");
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                }, fixedThreadPool))
                )
                .flatMap(Collection::stream)
                .toArray(CompletableFuture[]::new)).get();

        AccountDto accountA = httpClient.get("http://localhost:8080/accounts/A", AccountDto.class);
        AccountDto accountB = httpClient.get("http://localhost:8080/accounts/B", AccountDto.class);
        assertEquals(1001, accountA.balance());
        assertEquals(1001, accountB.balance());
    }

}