package cloud.cave.extension;

import cloud.cave.config.socket.RestRequester;
import cloud.cave.ipc.Marshaling;
import cloud.cave.server.common.Command;
import cloud.cave.service.IRestRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An implementation of a command that 'flies the player home' to the entry room
 * (0,0,0).
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class QuoteCommand extends AbstractCommand implements Command {
    private IRestRequest restRequest;
    private static final Logger logger = LoggerFactory.getLogger(QuoteCommand.class);
    private String host;
    private int port;

    @Override
    public JSONObject execute(String... parameters) {
        this.restRequest = new RestRequester();
        this.host = "winston";
        this.port = 6745;

        String quote, id;
        id = parameters[0];

        try {
            int idInt = Integer.parseInt(id);
            quote = getQuote(idInt);
        } catch (Exception e) {
            quote = getQuote();
        }

        JSONObject reply;
        reply = Marshaling.createValidReplyWithReturnValue(quote);
        return reply;
    }

    private String getQuote() {
        return getQuote(-1);
    }

    private String getQuote(int number) {
        String quoteString;
        try {
            String url;
            if (number < 0) {
                url = String.format("http://%s:%s/exam/winston",
                        host,
                        port);
            } else {
                url = String.format("http://%s:%s/exam/winston/%s",
                        host,
                        port,
                        number);
            }

            quoteString = restRequest.doRequest(url, null);
        } catch (IOException e) {
            throw new RuntimeException("An error occured in the connection to the Winston quote REST service.", e);
        } catch (Exception e) {
            logger.error("Fatal error in the Winston quote service while requesting the service", e);
            throw new RuntimeException("An error occured in the connection to the Winston quote REST service.", e);
        }

        JSONObject quoteJson;
        JSONParser parser = new JSONParser();
        try {
            quoteJson = (JSONObject) parser.parse(quoteString);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JSON returned from the service", e);
        } catch (Exception e) {
            logger.error("Fatal error in the weather service while parsin the JSON", e);
            throw new RuntimeException("Invalid JSON returned from the service", e);
        }

        return (String) quoteJson.get("quote");
    }
}
