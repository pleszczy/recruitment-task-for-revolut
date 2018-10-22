package pl.revolut.zadanie.app;

import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Container implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Container.class);
    private final Javalin javelin;

    public Container() {
        javelin = createContainer();
    }

    void start(int port){
        javelin.start(port);
    }

    void configureRouting(EndpointGroup endpointGroup) {
        javelin.routes(endpointGroup);
    }

    private Javalin createContainer() {
        return Javalin.create()
                .enableCorsForAllOrigins()
                .requestLogger((ctx, timeMs) -> LOG.trace(" {} {} {} {} {} ms", ctx.method(), ctx.path(), ctx.body(), ctx.headerMap(), timeMs));
    }

    @Override
    public void close() {
        javelin.stop();
    }
}
