package cloud.cave.config;

import cloud.cave.ipc.ClientRequestHandler;
import cloud.cave.server.common.ServerConfiguration;

/**
 * Concrete ClientFactory that reads environment variables to configure create
 * methods for the client side. After creation, each service delegate
 * is configured through their 'initialize' method with their service end point
 * configuration, again based upon reading their respective environment
 * variable.
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 * @see Config
 */
public class EnvironmentClientFactory implements CaveClientFactory {

    private EnvironmentReaderStrategy environmentReader;

    public EnvironmentClientFactory(EnvironmentReaderStrategy envReader) {
        environmentReader = envReader;
    }

    @Override
    public ClientRequestHandler createClientRequestHandler() {
        ClientRequestHandler crh;
        crh = Config.loadAndInstantiate(environmentReader,
                Config.SKYCAVE_CLIENTREQUESTHANDLER_IMPLEMENTATION, null);

        // Read in the server configuration
        ServerConfiguration config =
                new ServerConfiguration(environmentReader, Config.SKYCAVE_APPSERVER);
        crh.initialize(config);

        return crh;
    }
}
