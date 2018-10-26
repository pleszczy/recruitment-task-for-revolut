package pl.revolut.zadanie.app.store;


import pl.revolut.zadanie.app.model.Account;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AccountInMemoryStore {
    private final Map<String, Account> store;

    public AccountInMemoryStore(int initialCapacity) {
        store = new ConcurrentHashMap<>(initialCapacity);
    }

    public boolean contains(String iban) {
        return store.containsKey(iban);
    }

    public Optional<Account> get(String iban) {
        return Optional.ofNullable(store.get(iban));
    }

    public Account put(String iban, Account bankAccount) {
        return store.put(iban, bankAccount);
    }

    public Account remove(String iban) {
        return store.remove(iban);
    }

    public Set<Account> getAll() {
        return store.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }
}
