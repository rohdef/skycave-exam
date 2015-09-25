package cloud.cave.main;

import cloud.cave.config.*;
import cloud.cave.domain.Cave;
import cloud.cave.domain.ThreadCrashExeption;
import cloud.cave.ipc.Invoker;
import cloud.cave.ipc.Reactor;
import cloud.cave.server.StandardInvoker;
import cloud.cave.server.StandardServerCave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * The 'main' daemon to run on the server side. It uses a ServerFactory that
 * reads all relevant parameters to define the server side delegates
 * (subscription service, database connector, reactor implementation, IPs and
 * ports of connections...).
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 * @see Config
 */
public class CaveDaemon {
    @SuppressWarnings("FieldCanBeLocal")
    private static Thread daemon;
    private static int crashCounter;

    public static void main(String[] args) throws InterruptedException {
        // Create the logging
        Logger logger = LoggerFactory.getLogger(CaveDaemon.class);

        // Create the abstract factory to create delegates using dependency injection.
        // Here all injected delegates are defined by a set of predefined environment
        // variables.
        CaveServerFactory factory;
        EnvironmentReaderStrategy envReader;
        envReader = new OSEnvironmentReaderStrategy();
        factory = new EnvironmentServerFactory(envReader);

        // Create the server side cave instance
        Cave caveServer = new StandardServerCave(factory);

        // Create the invoker on the server side, and bind it to the cave
        Invoker serverInvoker = new StandardInvoker(caveServer);

        // Create the server side reactor...
        Reactor reactor = factory.createReactor(serverInvoker);

        // Make a section in the log file, marking the new session
        logger.info("=== SkyCave Reactor starting...");
        logger.info("Cave Configuration =" + caveServer.describeConfiguration());

        // Kept just in case the logger fails
        System.out.println("Use ctrl-c to terminate!");

        // and start the daemon...
        try{
        daemon = new Thread(reactor);
        daemon.start();

        // Ensure that its lifetime follows that of the main process
        daemon.join();
        }catch (InterruptedException e){
            throw e;
        }catch (ThreadCrashExeption e){
            crashCounter++;
            if(crashCounter < 3){
                main(args);
                e.printStackTrace();
            }else{
                logger.error("The thread has crashed more than 3 times", e);
                System.exit(1);
            }
        }

    }
}
