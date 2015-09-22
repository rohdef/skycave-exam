package cloud.cave.doubles;

import cloud.cave.service.IRestRequest;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class RequestSaboteur implements IRestRequest {
    private IRestRequest victim;
    private String garbage;
    private IOException throwNext;
    private RuntimeException throwNextRuntime;
    private boolean nullGarbage = false;

    public RequestSaboteur(IRestRequest victim) {
        if (victim == null)
            throw new NullPointerException("The victim for the saboteur must be set");

        this.victim = victim;
    }

    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException{
        String value = victim.doRequest(url, params);

        if (throwNext != null) {
            IOException e = throwNext;
            throwNext = null;
            throw e;
        }
        if (throwNextRuntime != null) {
            RuntimeException e = throwNextRuntime;
            throwNextRuntime = null;
            throw e;
        }
        if (nullGarbage) {
            value = null;
            garbage = null;
            nullGarbage = false;
        } else if (garbage != null) {
            value = garbage;
            garbage = null;
        }

        return value;
    }

    /**
     * Return this garbage in stead of the actual response. Be aware that exceptions take precedence.
     * @param garbage some random invalid value to return in stead of the JSON
     */
    public void setGarbage(String garbage) {
        this.garbage = garbage;
    }

    /**
     * Throw this exception next time @doRequest is run. Null means not to throw any, be aware that the exception
     * precedes the garbage
     * @param throwNext should either be a ClientProtocolException or IOException
     */
    public void setThrowNext(IOException throwNext) {
        this.throwNext = throwNext;
        throwNextRuntime = null;
    }



    /**
     * Throw this exception next time @doRequest is run. Null means not to throw any, be aware that the exception
     * precedes the garbage
     * @param throwNext should either be a ClientProtocolException or IOException
     */
    public void setThrowNext(RuntimeException throwNext) {
        this.throwNextRuntime = throwNext;
        this.throwNext = null;
    }

    public void reset() {
        this.throwNext = null;
        this.throwNextRuntime = null;
        this.garbage = null;
    }

    public void activateNullGarbage() {
        this.nullGarbage = true;
    }
}
