package cn.veasion.project.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketServer
 *
 * @author luozhuowei
 * @date 2022/1/28
 */
public class WebSocketServer {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private boolean checkHeartbeat = true;
    private int pingOfSeconds = 2 * 60;
    private int checkHeartbeatOfSeconds = 3 * 60;
    private int maxContentLength = 5 * 1024 * 1024;
    private int maxFrameSize = 5 * 1024 * 1024;

    private SSLContext sslContext;
    private ClientEventPublisher clientEventPublisher;
    private AbstractWebSocketAuthenticationHandler authenticationHandler;
    private static CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();

    // ????????????
    private transient ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private transient Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public ChannelFuture start(String websocketPath, int port, int workThread, AbstractWebSocketServerHandler handler) throws Exception {
        return start(websocketPath, port, 1, workThread, handler);
    }

    public ChannelFuture start(String websocketPath, int port, int bossThread, int workThread, AbstractWebSocketServerHandler handler) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("bossGroup"));
        EventLoopGroup workerGroup = new NioEventLoopGroup(workThread <= 0 ? NettyRuntime.availableProcessors() * 2 : workThread, new DefaultThreadFactory("workerGroup"));
        return start(websocketPath, port, bossGroup, workerGroup, handler);
    }

    public ChannelFuture start(String websocketPath, int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup, AbstractWebSocketServerHandler handler) throws Exception {
        handler.setWebSocketServer(this);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // .handler(new LoggingHandler(LogLevel.ERROR))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            if (sslContext != null) {
                                // ssl
                                SSLEngine sslEngine = sslContext.createSSLEngine();
                                sslEngine.setUseClientMode(false);
                                pipeline.addLast(new SslHandler(sslEngine));
                            }
                            if (checkHeartbeat) {
                                // ????????????
                                pipeline.addLast(new IdleStateHandler(checkHeartbeatOfSeconds, pingOfSeconds, 0));
                            }
                            // websocket?????????????????????Http????????????????????????Http?????????
                            pipeline.addLast(new HttpServerCodec());
                            // ?????????????????????????????????
                            pipeline.addLast(new ChunkedWriteHandler());
                            // netty???????????????????????????HttpObjectAggregator????????????????????????????????????,????????????????????????????????????
                            pipeline.addLast(new HttpObjectAggregator(maxContentLength));
                            // cors
                            pipeline.addLast(new CorsHandler(corsConfig));
                            if (authenticationHandler != null) {
                                // auth
                                authenticationHandler.setWebSocketServer(WebSocketServer.this);
                                pipeline.addLast(authenticationHandler);
                            }
                            // ?????????websocket???handler??????netty??????????????????????????????????????????????????????
                            pipeline.addLast(new WebSocketServerProtocolHandler(websocketPath, null, true, maxFrameSize, 10000L));
                            // ????????????handler?????????????????????????????????
                            pipeline.addLast(handler);
                        }
                    });
            log.info("????????????Netty...");
            ChannelFuture channelFuture = bootstrap.bind(new InetSocketAddress(port)).sync();
            String protocol = sslContext != null ? "wss" : "ws";
            log.info("Netty started on port: {} ({}) with path '{}', example => {}://127.0.0.1:{}{}?token=", port, protocol, websocketPath, protocol, port, websocketPath);
            // channelFuture.channel().closeFuture().sync();
            return channelFuture;
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                log.info("Netty exit.");
            }));
        }
    }

    public void join(Channel channel) {
        Objects.requireNonNull(channel, "channel????????????");
        channelGroup.add(channel);
        String key = WebSocketServer.channelKey(channel);
        if (key != null) {
            channelMap.put(key, channel);
        }
        if (log.isDebugEnabled()) {
            log.debug("??????????????????key={}, remoteAddress={}", key, channel.remoteAddress());
        }
        if (clientEventPublisher != null) {
            clientEventPublisher.publishEvent("join", channel, key);
        }
    }

    public void remove(Channel channel) {
        Objects.requireNonNull(channel, "channel????????????");
        channelGroup.remove(channel);
        String key = WebSocketServer.channelKey(channel);
        if (key != null) {
            channel = channelMap.remove(key);
            if (channel != null) {
                channelGroup.remove(channel);
            }
        }
        // if (log.isDebugEnabled()) {
        //     log.debug("??????????????????key={}, remoteAddress={}", key, channel != null ? channel.remoteAddress() : null);
        // }
        if (clientEventPublisher != null) {
            clientEventPublisher.publishEvent("remove", channel, key);
        }
    }

    public int getSize() {
        return channelMap.size();
    }

    public Channel getChannel(String key) {
        return channelMap.get(key);
    }

    public static String channelKey(Channel channel) {
        if (channel != null) {
            return channel.id().asShortText();
        } else {
            return null;
        }
    }

    public void removeAll() {
        channelGroup.clear();
        channelMap.clear();
        log.debug("?????????????????????");
    }

    /**
     * ???????????????????????????
     */
    public ChannelFuture sendTo(String key, WebSocketFrame msg) {
        Channel channel = channelMap.get(key);
        if (channel != null) {
            if (log.isDebugEnabled()) {
                log.debug("???????????? To {} ?????????: {}", key, msg instanceof TextWebSocketFrame ? ((TextWebSocketFrame) msg).text() : msg);
            }
            return channel.writeAndFlush(msg);
        } else {
            log.debug("???????????? To {} ??????????????????????????????????????????", key);
        }
        return null;
    }

    /**
     * ??????????????????????????????
     */
    public ChannelGroupFuture sendAll(WebSocketFrame msg) {
        if (log.isDebugEnabled()) {
            log.debug("??????????????????????????????: {}", msg instanceof TextWebSocketFrame ? ((TextWebSocketFrame) msg).text() : msg);
        }
        return channelGroup.writeAndFlush(msg);
    }

    public void setClientEventPublisher(ClientEventPublisher clientEventPublisher) {
        this.clientEventPublisher = clientEventPublisher;
    }

    public void setCheckHeartbeat(boolean checkHeartbeat, int pingOfSeconds, int checkHeartbeatOfSeconds) {
        this.checkHeartbeat = checkHeartbeat;
        this.pingOfSeconds = pingOfSeconds;
        this.checkHeartbeatOfSeconds = checkHeartbeatOfSeconds;
    }

    public void setAuthenticationHandler(AbstractWebSocketAuthenticationHandler authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public void setSslContext(String type, String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type); // JKS
        InputStream inputStream = new FileInputStream(path); // ??????????????????
        keyStore.load(inputStream, password.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        setSslContext(sslContext);
    }

    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }
}
