package cloud.cave.config.socket;

import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import cloud.cave.ipc.*;
import cloud.cave.server.common.ServerConfiguration;

/**
 * The actual client request handler based upon socket communication. The
 * request handler here uses the HTTP way of using sockets, that is, for each
 * request a client socket is created, the payload sent, and then the connection
 * is closed.
 *
 * @author Henrik Baerbak Christensen, University of Aarhus
 */
public class SocketClientRequestHandler implements ClientRequestHandler {
    private int portNumber;
    private String hostName;
    private PrintWriter out;
    private BufferedReader in;

    public SocketClientRequestHandler() {
        hostName = null;
        portNumber = -1;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        hostName = config.get(0).getHostName();
        portNumber = config.get(0).getPortNumber();
    }

    @Override
    public JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson)
            throws CaveIPCException {
        JSONObject replyJson = null;
        Socket clientSocket = null;
        // Create the socket to the host
        try {
            clientSocket = new Socket(hostName, portNumber);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));

            // Send the JSON request as a string to the app server
            out.println(requestJson.toString());

            // Block until a reply is received, and parse it into JSON
            String reply;


            reply = in.readLine();
            // System.out.println("--< reply: "+ reply.toString());

            JSONParser parser = new JSONParser();
            replyJson = (JSONObject) parser.parse(reply);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            throw new CaveIPCException("Disconnected", e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return replyJson;
    }

    public String toString() {
        return "SocketClientRequestHandler. AppServer Cfg: " + hostName + ":" + portNumber + ".";
    }
}