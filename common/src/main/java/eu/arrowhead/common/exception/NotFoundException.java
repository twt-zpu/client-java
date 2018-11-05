package eu.arrowhead.common.exception;

public class NotFoundException extends ArrowheadException {
    public NotFoundException(String msg, int errorCode, String origin, Throwable cause) {
        super(msg, errorCode, origin, cause);
    }

    public NotFoundException(String msg, int errorCode, String origin) {
        super(msg, errorCode, origin);
    }

    public NotFoundException(String msg, int errorCode, Throwable cause) {
        super(msg, errorCode, cause);
    }

    public NotFoundException(String msg, int errorCode) {
        super(msg, errorCode);
    }

    public NotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public NotFoundException(String msg) {
        super(msg);
    }
}
