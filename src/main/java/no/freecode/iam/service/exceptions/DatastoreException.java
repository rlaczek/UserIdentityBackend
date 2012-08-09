package no.obos.iam.service.exceptions;

public class DatastoreException extends RuntimeException {
    public DatastoreException() {
        super();
    }

    public DatastoreException(String message) {
        super(message);
    }

    public DatastoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatastoreException(Throwable cause) {
        super(cause);
    }
}
