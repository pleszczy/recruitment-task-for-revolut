package pl.revolut.zadanie.app;

import io.javalin.BadRequestResponse;
import io.javalin.Context;
import io.javalin.NotFoundResponse;
import io.javalin.apibuilder.CrudHandler;
import pl.revolut.zadanie.app.dto.AccountDto;
import pl.revolut.zadanie.app.model.Account;
import pl.revolut.zadanie.app.store.AccountInMemoryStore;

import java.util.Optional;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

public class AccountController implements CrudHandler {
    private final AccountInMemoryStore store;
    private final LongConsumer newBalanceValidator;

    public AccountController(AccountInMemoryStore store, LongConsumer newBalanceValidator) {
        this.store = store;
        this.newBalanceValidator = newBalanceValidator;
    }

    @Override
    public void create(Context context) {
        var accountDto = context.bodyAsClass(AccountDto.class);
        var iban = accountDto.iban();
        if (!store.contains(iban)) {
            Account account = new Account(accountDto.balance(), iban);
            store.put(accountDto.iban(), account);
        } else {
            throw new BadRequestResponse(String.format("Account with iban %s already exists", iban));
        }
    }

    @Override
    public void delete(Context context, String iban) {
        store.remove(iban);
    }

    @Override
    public void getAll(Context context) {
        Set<AccountDto> accounts = store.getAll()
                .stream()
                .map(account -> new AccountDto(account.getIban(), account.getBalance()))
                .collect(Collectors.toSet());
        context.json(accounts);
    }

    @Override
    public void getOne(Context context, String iban) {
        var account = store.get(iban);
        if (account.isPresent()) {
            context.json(new AccountDto(iban, account.get().getBalance()));
        } else {
            throw new NotFoundResponse(String.format("Could not find an account with iban %s", iban));
        }
    }

    @Override
    public void update(Context context, String iban) {
        var accountDto = context.bodyAsClass(AccountDto.class);
        Optional<Account> account = store.get(iban);
        if (account.isPresent()) {
            account.get().setBalance(accountDto.balance());
        } else {
            throw new NotFoundResponse(String.format("Could not find an account with iban %s", iban));
        }
    }

    public void transferFoundsBetweenAccounts(String ibanFrom, String ibanTo, long amount) {
        if (amount < 0) {
            throw new BadRequestResponse("Cant transfer negative founds between accounts");
        }
        var accountFrom = store.get(ibanFrom);
        var accountTo = store.get(ibanTo);
        if (accountFrom.isPresent() && accountTo.isPresent()) {
            accountFrom.get().transferTo(accountTo.get(), amount, newBalanceValidator);
        } else {
            throw new NotFoundResponse("At least one account with the given iban does not exist ");
        }
    }

}
