package eu.arrowhead.common.exception;

public class KeystoreException extends ArrowheadException {
    public KeystoreException(String msg, int errorCode, String origin, Throwable cause) {
        super(msg, errorCode, origin, cause);
    }

    public KeystoreException(String msg, int errorCode, String origin) {
        super(msg, errorCode, origin);
    }

    public KeystoreException(String msg, int errorCode, Throwable cause) {
        super(msg, errorCode, cause);
    }

    public KeystoreException(String msg, int errorCode) {
        super(msg, errorCode);
    }

    public KeystoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public KeystoreException(String msg) {
        super(msg);
    }
}
