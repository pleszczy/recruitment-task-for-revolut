package pl.revolut.zadanie.app;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.beans.ConstructorProperties;
import java.util.StringJoiner;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AccountDto {
    private final long balance;
    private final String iban;

    @ConstructorProperties({"iban", "balance"})
    AccountDto(String iban, long balance) {
        this.iban = iban;
        this.balance = balance;
    }

    String iban() {
        return iban;
    }

    public long balance() {
        return balance;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AccountDto.class.getSimpleName() + "[", "]")
                .add("balance=" + balance)
                .add("iban='" + iban + "'")
                .toString();
    }
}
