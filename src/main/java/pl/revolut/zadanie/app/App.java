package pl.revolut.zadanie.app;

import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.get;


public class App implements AutoCloseable {
    private final Container container;
    private final AccountController accountController;
    private int serverPort;

    public App(Container container, AccountController accountController, int serverPort) {
        this.container = container;
        this.accountController = accountController;
        this.serverPort = serverPort;
    }

    public void start() {
        container.configureRouting(
                () -> {
                    crud("accounts/:iban", accountController);
                    get("accounts/transfer/:amount/from/:ibanfrom/to/:ibanto",
                            ctx -> accountController.transferFoundsBetweenAccounts(ctx.pathParam("ibanfrom"), ctx.pathParam("ibanto"), Long.valueOf(ctx.pathParam("amount"))));
                });
        container.start(serverPort);
    }

    @Override
    public void close() {
        container.close();
    }
}
