package pl.revolut.zadanie.app.model;

import org.multiverse.api.StmUtils;
import org.multiverse.api.references.TxnLong;
import org.multiverse.api.references.TxnRef;

import java.util.Date;
import java.util.function.LongConsumer;

public class Account {
    private final TxnRef<Date> lastModified = StmUtils.newTxnRef(new Date());
    private final TxnLong balance = StmUtils.newTxnLong();
    private final String iban;

    public Account(long balance, String iban) {
        this.iban = iban;
        StmUtils.atomic(() -> this.balance.set(balance));
    }

    public long getBalance() {
        return StmUtils.atomic(() -> balance.get());
    }

    public void setBalance(long balance) {
        StmUtils.atomic(() -> {
            this.balance.set(balance);
            updateLastModified();
        });
    }

    void incrementBalance(long value, LongConsumer newBalanceValidator) {
        StmUtils.atomic(() -> {
            long newBalance = balance.incrementAndGet(value);
            updateLastModified();
            newBalanceValidator.accept(newBalance);
        });
    }

    void decrementBalance(long value, LongConsumer newBalanceValidator) {
        StmUtils.atomic(() -> {
            balance.decrement(value);
            updateLastModified();
            newBalanceValidator.accept(balance.get());
        });
    }

    void incrementBalance(long value) {
        incrementBalance(value, it -> {
        });

    }

    void decrementBalance(long value) {
        decrementBalance(value, it -> {
        });
    }

    private void updateLastModified() {
        this.lastModified.set(new Date());
    }

    public void transferTo(Account accountTo, long amount, LongConsumer newBalanceValidator) {
        StmUtils.atomic(() -> {
            this.decrementBalance(amount, newBalanceValidator);
            accountTo.incrementBalance(amount);
        });
    }

    public String getIban() {
        return iban;
    }
}