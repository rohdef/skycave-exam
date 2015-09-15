package cloud.cave.doubles;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.ipc.*;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.server.service.ServerSubscriptionService;
import cloud.cave.server.service.ServerWeatherService;
import cloud.cave.service.*;

/**
 * Concrete factory for making making delegates that are all test doubles.
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class AllTestDoubleFactory implements CaveServerFactory {
    private WeatherService weatherService;

    @Override
    public CaveStorage createCaveStorage() {
        CaveStorage storage = new FakeCaveStorage();
        storage.initialize(null); // the fake storage needs no server configuration object
        return storage;
    }

    @Override
    public SubscriptionService createSubscriptionServiceConnector() {
        String host = "is-there-anybody-out-there";
        int port = 57005;
        ServerConfiguration serverConfiguration = new ServerConfiguration(host, port);

        SubscriptionService service = new ServerSubscriptionService();
        service.setRestRequester(new SubscriptionServiceRequestFake());
        service.initialize(serverConfiguration);
        return service;
    }

    @Override
    public IRestRequest createRestRequester() {
        return null;
    }

    @Override
    public WeatherService createWeatherServiceConnector() {
        String host = "is-there-anybody-out-there";
        int port = 57005;
        ServerConfiguration serverConfiguration = new ServerConfiguration(host, port);

        WeatherService service = new ServerWeatherService();
        service.initialize(serverConfiguration); // no config object required
        service.setRestRequester(new WeatherServiceRequestFake());
        service.setSecondsDelay(1);

        weatherService = service;
        return service;
    }

    public WeatherService getLastWeatherService() {
        return this.weatherService;
    }

    @Override
    public Reactor createReactor(Invoker invoker) {
        // The reactor is not presently used in the test cases...
        return null;
    }
}
