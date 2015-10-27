package taeyo.sample.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * @author Taeyoung, Kim
 */
@Slf4j
@Setter
public class NioMessageSender {

    private static final int RETRY_DELAY_TIME = 10;

    private final Bootstrap bootstrap = new Bootstrap();

    private final String serverIP;
    private final int serverPort;

    private SuccessCallback<Map<String, String>> successCallback;
    private FailureCallback failureCallback;
    private EventLoopGroup workerGroup;
    private int messageTimeoutMilliseconds = 3000;

    public NioMessageSender(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void init() {
        if (workerGroup == null) {
            workerGroup = new NioEventLoopGroup(1);
        }

        bootstrap.group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    log.info("ch : " + ch);
                    ch.pipeline().addLast(
                        new StringEncoder(),
                        new MapToStringEncoder(),
                        new LineBasedFrameDecoder(8192),
                        new ReadTimeoutHandler(messageTimeoutMilliseconds, TimeUnit.MILLISECONDS),
                        new StringDecoder(),
                        new SimpleChannelInboundHandler<String>() {

                            private Map<String, String> map = new HashMap<>();

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                log.debug("Channel has been active.");
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
                                        failureCallback.onFailure(new NotReceivedMessageException("Not received message"));
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
                        });
                }
            });
    }

    public void send(Map<String, String> outMap) {
        log.debug("Attempts a new connection to {} TCP Server.", serverIP);

        ChannelFuture future = bootstrap.connect(serverIP, serverPort).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        future.addListener((ChannelFuture channelFuture) -> {
            if (channelFuture.isSuccess()) {
                log.debug("Success!!!");

                Channel ch = channelFuture.channel();
                ch.closeFuture().addListener((ChannelFuture cf) -> log.debug("Channel Close!!!"));
                ch.writeAndFlush(outMap).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }
        });
    }

    public void addCallback(SuccessCallback<Map<String, String>> successCallback, FailureCallback failureCallback) {
        this.successCallback = successCallback;
        this.failureCallback = failureCallback;
    }

    public static void main(String[] args) throws InterruptedException {
        int WORKER_COUNT = 10;

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        CountDownLatch countDownLatch = new CountDownLatch(WORKER_COUNT);

        NioMessageSender messageSender = new NioMessageSender("127.0.0.1", 9001);
        messageSender.setWorkerGroup(workerGroup);
        messageSender.setMessageTimeoutMilliseconds(5000);
        messageSender.init();
        messageSender.addCallback(inMap -> {
            log.info(inMap.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", ", "[", "]")));
            countDownLatch.countDown();
        }, ex -> {
            log.error("{}", ex.getMessage());
            countDownLatch.countDown();
        });

        for (int i = 0; i < WORKER_COUNT; i++) {
            Map<String, String> outMap = new HashMap<>();
            outMap.put("KEY1", "VALUE1-" + i);
            outMap.put("KEY2", "VALUE2-" + i);
            outMap.put("KEY3", "VALUE3-" + i);
            messageSender.send(outMap);
        }

        countDownLatch.await();
        workerGroup.shutdownGracefully();
    }
}

