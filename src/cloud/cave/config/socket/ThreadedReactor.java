package cloud.cave.config.socket;

import cloud.cave.domain.Region;
import cloud.cave.ipc.Invoker;
import cloud.cave.ipc.Marshaling;
import cloud.cave.ipc.Reactor;
import cloud.cave.ipc.StatusCode;
import cloud.cave.server.common.ServerConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ThreadedReactor implements Reactor {
    private int portNumber;
    private Invoker invoker;
    private static final Logger logger = LoggerFactory.getLogger(SocketReactor.class);
    private JSONParser parser;

    private ServerSocket serverSocket = null;

    @Override
    public void initialize(Invoker invoker, ServerConfiguration config) {
        portNumber = config.get(0).getPortNumber();
        this.invoker = invoker;
        parser = new JSONParser();
    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public Region getRegion() {
        return null;
    }

    @Override
    public void run() {
        openServerSocket();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        logger.info(String.format("*** Server socket established on port %1$d ***", this.portNumber));

        boolean isStopped = false;
        while (!isStopped) {
            logger.debug("--> Accepting...");

            try {
                final Socket clientSocket = serverSocket.accept();
                executorService.execute(new ReadMessageAndDispatch(clientSocket));
            } catch (IOException e) {
                if (isStopped) {
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            } catch (Exception e) {
                logger.error("Unexpected exception while accepting client socket. Continueing loop.", e);
                continue;
            }
        }

        executorService.shutdown();
        logger.info("Server Stopped.");
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.portNumber);
        } catch (IOException e) {
            logger.error("Cannot open port " + portNumber, e);
            System.exit(-1);
        }
    }

    public String toString() {
        return "SocketReactor. Assigned to port: " + portNumber;
    }

    private class ReadMessageAndDispatch implements Runnable {
        private final Socket clientSocket;

        public ReadMessageAndDispatch(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                readMessageAndDispatch();
            } catch (IOException e) {
                logger.error("IOException during readMessageAndDispatch", e);
            } catch (RuntimeException e) {
                logger.error("Unexpected exception while reading and writing to client. Continueing loop.", e);
            }
        }


        private void readMessageAndDispatch() throws IOException {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine = in.readLine();
            logger.trace(" --!! ThreadID: "+ Thread.currentThread().getId());
            logger.debug("--> Received " + inputLine);

            JSONObject requestJson, reply;
            try {
                requestJson = (JSONObject) parser.parse(inputLine);
                reply = invoker.handleRequest(requestJson);
            } catch (ParseException e) {
                String errorMsg = "JSON Parse error on input: " + inputLine;
                logger.warn(errorMsg, e);
                reply = Marshaling.createInvalidReplyWithExplantion(
                        StatusCode.SERVER_FAILURE, errorMsg);
            } catch (NullPointerException e) {
                String errorMsg = "NullPointeException when trying to JSON parse error the input: " + inputLine;
                logger.warn(errorMsg, e);
                reply = Marshaling.createInvalidReplyWithExplantion(
                        StatusCode.SERVER_FAILURE, errorMsg);
            } catch (Exception e) {
                String errorMsg = "Error when JSON parsing the input: " + inputLine;
                logger.warn(errorMsg, e);
                reply = Marshaling.createInvalidReplyWithExplantion(
                        StatusCode.SERVER_FAILURE, errorMsg);
            }
            if (reply == null) {
                String errorMsg = "The reply from the invoker was null";
                logger.error(errorMsg);
                reply = Marshaling.createInvalidReplyWithExplantion(
                        StatusCode.SERVER_FAILURE, errorMsg);
            }

            out.println(reply.toString());
            logger.debug("--< !!! replied: " + reply);

            logger.info("Closing socket...");
            in.close();
            out.close();
        }
    }
}
