package cloud.cave.common;

/**
 * Created by rohdef on 10/10/15.
 */
public class CaveStorageException extends RuntimeException {
    public CaveStorageException() {
    }

    public CaveStorageException(String message) {
        super(message);
    }

    public CaveStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaveStorageException(Throwable cause) {
        super(cause);
    }

    public CaveStorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
