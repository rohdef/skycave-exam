package cloud.cave.domain;

/**
 * Created by mark on 9/25/15.
 */
public class ThreadCrashExeption extends  RuntimeException{
    public ThreadCrashExeption() {
    }

    public ThreadCrashExeption(String message) {
        super(message);
    }

    public ThreadCrashExeption(String message, Throwable cause) {
        super(message, cause);
    }

    public ThreadCrashExeption(Throwable cause) {
        super(cause);
    }

    public ThreadCrashExeption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
