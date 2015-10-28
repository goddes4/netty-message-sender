package taeyo.sample.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Taeyoung, Kim
 */
class KeyValueStringToMapHandler extends SimpleChannelInboundHandler<String> {

    private final SuccessCallback<Map<String, String>> successCallback;
    private final FailureCallback failureCallback;
    private final Map<String, String> map = new HashMap<>();

    public KeyValueStringToMapHandler(SuccessCallback<Map<String, String>> successCallback, FailureCallback failureCallback) {
        this.successCallback = successCallback;
        this.failureCallback = failureCallback;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            if (!map.isEmpty()) {
                try {
                    successCallback.onSuccess(map);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            } else {
                failureCallback.onFailure(new NotReceivedMessageException("Message has not received"));
            }
        } else {
            failureCallback.onFailure(cause);
        }
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        String[] split = msg.split("=");
        map.put(split[0], split[1]);
    }
}
