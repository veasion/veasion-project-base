package cn.veasion.project.websocket;

import cn.veasion.project.utils.CountDownLatchExt;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocketClient
 *
 * @author luozhuowei
 * @date 2022/1/27
 */
public class WebSocketClient {

    private int maxContentLength = 5 * 1024 * 1024;
    private int maxFrameSize = 5 * 1024 * 1024;
    private Map<String, Object> headers;
    private boolean checkHeartbeat;
    private Channel channel;

    /**
     * 连接
     *
     * @param url websocket链接，如 wss://www.veasion.cn/ws
     */
    public void connect(String url, AbstractWebSocketClientHandler handler) throws Exception {
        connect(url, handler, null);
    }

    public void connect(String url, AbstractWebSocketClientHandler handler, Consumer<Bootstrap> bootstrapConsumer) throws Exception {
        URI uri = new URI(url);
        final int port;
        final SslContext sslCtx;
        if ("wss".equalsIgnoreCase(uri.getScheme())) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            port = uri.getPort() == -1 ? 443 : uri.getPort();
        } else {
            sslCtx = null;
            port = uri.getPort() == -1 ? 80 : uri.getPort();
        }
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, httpHeaders, maxFrameSize);
        handler.setHandshaker(handshaker);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            if (sslCtx != null) {
                                pipeline.addLast(sslCtx.newHandler(socketChannel.alloc(), uri.getHost(), port));
                            }
                            if (checkHeartbeat) {
                                pipeline.addLast(new IdleStateHandler(0, 5, 0));
                            }
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(maxContentLength));
                            pipeline.addLast(handler);
                        }
                    });
            if (bootstrapConsumer != null) {
                bootstrapConsumer.accept(bootstrap);
            }
            CountDownLatchExt<Future<?>> countDownLatch = new CountDownLatchExt<>();
            bootstrap.connect(uri.getHost(), port).addListener(channelFuture -> {
                countDownLatch.setResult(channelFuture);
                countDownLatch.countDown();
            });
            countDownLatch.await(8, TimeUnit.SECONDS);
            ChannelFuture result = (ChannelFuture) countDownLatch.getResult();
            if (!result.isSuccess()) {
                if (result.cause() != null) {
                    throw new RuntimeException("连接失败", result.cause());
                } else {
                    throw new RuntimeException("连接失败");
                }
            }
            channel = result.channel();
            channel.closeFuture().addListener(future -> group.shutdownGracefully());
            handshaker.handshake(channel);
            handler.getHandshakeFuture().await(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    public void setCheckHeartbeat(boolean checkHeartbeat) {
        this.checkHeartbeat = checkHeartbeat;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, Object value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(key, value);
    }

    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public ChannelFuture writeAndFlush(byte[] bytes) {
        return writeAndFlush(bytes, null);
    }

    public ChannelFuture writeAndFlush(String msg) {
        return writeAndFlush(new TextWebSocketFrame(msg), null);
    }

    public ChannelFuture writeAndFlush(WebSocketFrame webSocketFrame) {
        return writeAndFlush(webSocketFrame, null);
    }

    public ChannelFuture writeAndFlush(byte[] bytes, ChannelFutureListener channelFutureListener) {
        return writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(bytes)), channelFutureListener);
    }

    public ChannelFuture writeAndFlush(WebSocketFrame webSocketFrame, ChannelFutureListener channelFutureListener) {
        if (!channel.isActive()) {
            throw new RuntimeException("连接已断开，发送消息失败");
        }
        ChannelFuture channelFuture = channel.writeAndFlush(webSocketFrame);
        if (channelFutureListener != null) {
            channelFuture.addListener(channelFutureListener);
        }
        return channelFuture;
    }

    public Channel getChannel() {
        return channel;
    }

    public Future<?> close() throws Exception {
        Future<?> future = null;
        if (channel != null) {
            future = channel.close().sync();
        }
        return future;
    }

}
