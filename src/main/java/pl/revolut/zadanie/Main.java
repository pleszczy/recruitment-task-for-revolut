package pl.revolut.zadanie;

import io.javalin.BadRequestResponse;
import pl.revolut.zadanie.app.AccountController;
import pl.revolut.zadanie.app.store.AccountInMemoryStore;
import pl.revolut.zadanie.app.App;
import pl.revolut.zadanie.app.Container;

import java.util.function.LongConsumer;

public class Main {

    public static void main(String[] args) {
        var app = manualDependencyInjectionApp();
        app.start();
    }

    public static App manualDependencyInjectionApp() {
        var serverPort = 8080;
        var storeInitialCapacity = 128;
        LongConsumer newBalanceValidator = newBalance -> {
            if (newBalance < 0) {
                throw new BadRequestResponse("Not enough founds to transfer from the source account");
            }
        };
        var container = new Container();
        var accountStore = new AccountInMemoryStore(storeInitialCapacity);
        var accountController = new AccountController(accountStore, newBalanceValidator);
        return new App(container, accountController, serverPort);
    }

}
