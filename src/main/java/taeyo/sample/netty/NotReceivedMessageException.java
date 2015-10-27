package taeyo.sample.netty;

/**
 *
 * @author Taeyoung, Kim
 */
public class NotReceivedMessageException extends RuntimeException {
    public NotReceivedMessageException(String msg) {
        super(msg);
    }
}
