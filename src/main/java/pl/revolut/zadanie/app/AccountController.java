package pl.revolut.zadanie.app;

import io.javalin.BadRequestResponse;
import io.javalin.Context;
import io.javalin.NotFoundResponse;
import io.javalin.apibuilder.CrudHandler;

import java.util.Optional;
import java.util.function.LongConsumer;

public class AccountController implements CrudHandler {
    private final AccountInMemoryStore dao;
    private LongConsumer newBalanceValidator;

    public AccountController(AccountInMemoryStore dao, LongConsumer newBalanceValidator) {
        this.dao = dao;
        this.newBalanceValidator = newBalanceValidator;
    }

    @Override
    public void create(Context context) {
        var accountDto = context.bodyAsClass(AccountDto.class);
        var iban = accountDto.iban();
        if (!dao.contains(iban)) {
            Account account = new Account(accountDto.balance());
            dao.put(accountDto.iban(), account);
        } else {
            throw new BadRequestResponse(String.format("Account with iban %s already exists", iban));
        }
    }

    @Override
    public void delete(Context context, String iban) {
        dao.remove(iban);
    }

    @Override
    public void getAll(Context context) {
        context.json(dao.getAll());
    }

    @Override
    public void getOne(Context context, String iban) {
        var account = dao.get(iban);
        if (account.isPresent()) {
            context.json(new AccountDto(iban, account.get().getBalance()));
        } else {
            throw new NotFoundResponse(String.format("Could not find an account with iban %s", iban));
        }
    }

    @Override
    public void update(Context context, String iban) {
        var accountDto = context.bodyAsClass(AccountDto.class);
        Optional<Account> account = dao.get(iban);
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
        var accountFrom = dao.get(ibanFrom);
        var accountTo = dao.get(ibanTo);
        if (accountFrom.isPresent() && accountTo.isPresent()) {
            accountFrom.get().transferTo(accountTo.get(), amount, newBalanceValidator);
        } else {
            throw new NotFoundResponse("At least one account with the given iban does not exist ");
        }
    }

}
