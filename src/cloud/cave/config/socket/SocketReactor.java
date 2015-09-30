package cloud.cave.config.socket;

import java.io.*;
import java.net.*;

import cloud.cave.domain.Region;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.slf4j.*;

import cloud.cave.ipc.*;
import cloud.cave.server.common.ServerConfiguration;

/**
 * The Reactor that binds to the OS and forwards any incoming requests on a
 * server socket to the given server invoker.
 * <p/>
 * The connection style is similar to HTTP/1.0 in that a separate
 * connection is made for every request. This is very simple
 * and easy to implement but gives a lot of connection overhead.
 * <p/>
 * As this reactor is meant as a case study, it is abnormally
 * talkative for a server and prints a lot on the console
 * instead of logging stuff.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class SocketReactor implements Reactor {
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

        logger.info(String.format("*** Server socket established on port %1$d ***", this.portNumber));

        boolean isStopped = false;
        while (!isStopped) {
            logger.debug("--> Accepting...");

            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                if (isStopped) {
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            } catch (Exception e) {
                logger.error("Unexpected exception while accepting client socket. Continueing loop.", e);
                continue;
            }

            logger.debug("--> AcceptED!");

            try {
                readMessageAndDispatch(clientSocket);
            } catch (IOException e) {
                logger.error("IOException during readMessageAndDispatch", e);
            } catch (Exception e) {
                logger.error("Unexpected exception while reading and writing to client. Continueing loop.", e);
            }
        }

        logger.info("Server Stopped.");
    }

    private void readMessageAndDispatch(Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()));

        String inputLine;
        inputLine = in.readLine();
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

}