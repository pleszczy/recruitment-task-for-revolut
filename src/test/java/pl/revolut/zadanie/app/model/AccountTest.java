package pl.revolut.zadanie.app.model;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountTest {
    private static ExecutorService fixedThreadPool ;
    private final LongConsumer positiveBalanceValidator = newBalance -> {
        if (newBalance < 0) {
            throw new IllegalArgumentException();
        }
    };

    @BeforeAll
    public static void beforeAll() {
        fixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    }

    @AfterAll
    public static void afterAll() {
        fixedThreadPool.shutdownNow();
    }

    @Test
    public void should_correctly_decrement_balance() {
        Account account = new Account(100, "A");

        account.decrementBalance(100);

        assertEquals(0, account.getBalance(), "Expected account A to have 0");
    }

    @Test
    public void should_correctly_increment_balance() {
        Account account = new Account(100, "A");

        account.incrementBalance(100);

        assertEquals(200, account.getBalance(), "Expected account A to have 200");
    }

    @Test
    public void should_correctly_transfer_between_two_accounts() {
        Account accountA = new Account(100, "A");
        Account accountB = new Account(100, "B");

        accountA.transferTo(accountB, 100, positiveBalanceValidator);

        Assertions.assertAll(
                () -> assertEquals(0, accountA.getBalance(), "Expected account A to have 0"),
                () -> assertEquals(200, accountB.getBalance(), "Expected account B to have 200")
        );
    }

    @Test
    void should_atomically_decrement_balance() throws ExecutionException, InterruptedException {
        Account account = new Account(200_000, "A");

        CompletableFuture.allOf(IntStream
                .range(0, 200_000)
                .parallel()
                .mapToObj(value -> CompletableFuture.runAsync(() -> account.decrementBalance(1), fixedThreadPool))
                .toArray(CompletableFuture[]::new)).get();

        assertEquals(0, account.getBalance(), "Expected account A to have 0");
    }

    @Test
    public void should_atomically_increment_balance() throws ExecutionException, InterruptedException {
        Account account = new Account(0, "A");

        CompletableFuture.allOf(IntStream
                .range(0, 200_000)
                .parallel()
                .mapToObj(value -> CompletableFuture.runAsync(() -> account.incrementBalance(1), fixedThreadPool))
                .toArray(CompletableFuture[]::new)).get();

        assertEquals(200_000, account.getBalance(), "Expected account A to have 200 000");
    }

    @Test
    public void should_transfer_between_accounts_atomically_and_with_no_deadlocks() throws ExecutionException, InterruptedException {
        Account accountA = new Account(1_000_000, "A");
        Account accountB = new Account(1_000_000, "B");

        CompletableFuture.allOf(IntStream
                .range(0, 200_000)
                .parallel()
                .mapToObj(value -> List.of(
                        CompletableFuture.runAsync(() -> {
                            accountA.transferTo(accountB, 99, positiveBalanceValidator);
                            accountB.transferTo(accountA, 99, positiveBalanceValidator);
                        }, fixedThreadPool)
                        , CompletableFuture.runAsync(() -> {
                            accountB.transferTo(accountA, 99, positiveBalanceValidator);
                            accountA.transferTo(accountB, 99, positiveBalanceValidator);
                        }, fixedThreadPool)))
                .flatMap(Collection::stream)
                .toArray(CompletableFuture[]::new)).get();

        Assertions.assertAll(
                () -> assertEquals(1_000_000, accountA.getBalance(), "Expected account A to have 1 000 000"),
                () ->  assertEquals(1_000_000, accountB.getBalance(), "Expected account B to have 1 000 000")
        );
    }

    @Test
    public void should_validate_new_balance_after_decrementing() {
        Account account = new Account(100, "A");

        assertThrows(IllegalArgumentException.class, () -> account.decrementBalance(200, positiveBalanceValidator), "Expected to throw an exception if the new balance fails validation");
    }

    @Test
    public void should_validate_new_balance_after_incrementing() {
        LongConsumer cappedBalanceValidator = newBalance -> {
            if (newBalance > 200) {
                throw new IllegalArgumentException();
            }
        };
        Account account = new Account(100, "A");

        assertThrows(IllegalArgumentException.class, () -> account.incrementBalance(200, cappedBalanceValidator), "Expected to throw an exception if the new balance fails validation");
    }
}