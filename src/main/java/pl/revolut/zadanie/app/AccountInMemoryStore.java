package pl.revolut.zadanie.app;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AccountInMemoryStore {
    private final Map<String, Account> store;

    public AccountInMemoryStore(int initialCapacity) {
        store = new HashMap<>(initialCapacity);
    }

    boolean contains(String iban) {
        return store.containsKey(iban);
    }

    Optional<Account> get(String iban) {
        return Optional.ofNullable(store.get(iban));
    }

    Account put(String iban, Account bankAccount) {
        return store.put(iban, bankAccount);
    }

    Account remove(String iban) {
        return store.remove(iban);
    }

    Set<AccountDto> getAll() {
        return store.entrySet()
                .stream()
                .map(entry -> new AccountDto(entry.getKey(), entry.getValue().getBalance()))
                .collect(Collectors.toSet());
    }
}
