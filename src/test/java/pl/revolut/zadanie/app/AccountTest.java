package pl.revolut.zadanie.app;

import org.junit.jupiter.api.AfterAll;
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
    // Want to use at least 3 threads even on a single core CPU
    private final static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    private final LongConsumer positiveBalanceValidator = newBalance -> {
        if (newBalance < 0) {
            throw new IllegalArgumentException();
        }
    };
    private LongConsumer emptyValidator = (newBalance) -> {
    };

    @AfterAll
    public static void afterAll() {
        fixedThreadPool.shutdownNow();
    }

    @Test
    public void should_correctly_decrement_balance() {
        Account account = new Account(100);

        account.decrementBalance(100);

        assertEquals(0, account.getBalance());
    }

    @Test
    public void should_correctly_increment_balance() {
        Account account = new Account(100);

        account.incrementBalance(100);

        assertEquals(200, account.getBalance());
    }

    @Test
    public void should_correctly_transfer_between_two_accounts() {
        Account accountA = new Account(100);
        Account accountB = new Account(100);

        accountA.transferTo(accountB, 100, positiveBalanceValidator);

        assertEquals(200, accountB.getBalance());
    }

    @Test
    void should_atomically_decrement_balance() throws ExecutionException, InterruptedException {
        Account account = new Account(200_000);

        var completableFutures = IntStream.range(0, 200_000)
                .mapToObj(value -> CompletableFuture.runAsync(() -> account.decrementBalance(1), fixedThreadPool))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).get();

        assertEquals(0, account.getBalance());
    }

    @Test
    public void should_atomically_increment_balance() throws ExecutionException, InterruptedException {
        Account account = new Account(0);

        var completableFutures = IntStream.range(0, 200_000)
                .mapToObj(value -> CompletableFuture.runAsync(() -> account.incrementBalance(1), fixedThreadPool))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).get();

        assertEquals(200_000, account.getBalance(), "");
    }

    @Test
    public void should_transfer_between_accounts_atomically_and_with_no_deadlocks() throws ExecutionException, InterruptedException {
        Account accountA = new Account(31);
        Account accountB = new Account(31);

        CompletableFuture.allOf(IntStream.range(0, 200_000)
                .mapToObj(value -> List.of(
                        CompletableFuture.runAsync(() -> {
                            accountA.transferTo(accountB, 99, emptyValidator);
                            accountB.transferTo(accountA, 99, emptyValidator);
                        }, fixedThreadPool)
                        , CompletableFuture.runAsync(() -> {
                            accountB.transferTo(accountA, 99, emptyValidator);
                            accountA.transferTo(accountB, 99, emptyValidator);
                        }, fixedThreadPool)))
                .flatMap(Collection::stream)
                .toArray(CompletableFuture[]::new)).get();

        assertEquals(31, accountA.getBalance());
        assertEquals(31, accountB.getBalance());
    }

    @Test
    public void should_validate_new_balance_after_decrementing() {
        Account account = new Account(100);

        assertThrows(IllegalArgumentException.class, () -> account.decrementBalance(200, positiveBalanceValidator), "Expected to throw an exception if the new balance fails validation");
    }

    @Test
    public void should_validate_new_balance_after_incrementing() {
        LongConsumer cappedBalanceValidator = newBalance -> {
            if (newBalance > 200) {
                throw new IllegalArgumentException();
            }
        };
        Account account = new Account(100);

        assertThrows(IllegalArgumentException.class, () -> account.incrementBalance(200, cappedBalanceValidator), "Expected to throw an exception if the new balance fails validation");
    }
}