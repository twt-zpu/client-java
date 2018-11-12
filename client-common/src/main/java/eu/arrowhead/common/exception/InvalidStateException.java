package eu.arrowhead.common.exception;

public class InvalidStateException extends ArrowheadRuntimeException {
    public InvalidStateException(String msg, int errorCode, String origin, Throwable cause) {
        super(msg, errorCode, origin, cause);
    }

    public InvalidStateException(String msg, int errorCode, String origin) {
        super(msg, errorCode, origin);
    }

    public InvalidStateException(String msg, int errorCode, Throwable cause) {
        super(msg, errorCode, cause);
    }

    public InvalidStateException(String msg, int errorCode) {
        super(msg, errorCode);
    }

    public InvalidStateException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public InvalidStateException(String msg) {
        super(msg);
    }
}
