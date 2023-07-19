package cn.veasion.project.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AbstractWebSocketClientHandler
 *
 * @author luozhuowei
 * @date 2022/1/27
 */
public abstract class AbstractWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    protected CountDownLatch closeCountDownLatch;

    void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
        if (log.isDebugEnabled()) {
            log.debug("建立连接");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
        closeCountDownLatch = new CountDownLatch(1);
        if (log.isDebugEnabled()) {
            log.debug("连接成功");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
        if (closeCountDownLatch != null) {
            closeCountDownLatch.countDown();
        }
        if (log.isDebugEnabled()) {
            log.debug("连接断开");
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();
        FullHttpResponse response;
        if (!this.handshaker.isHandshakeComplete()) {
            try {
                response = (FullHttpResponse) msg;
                // 完成握手
                this.handshaker.finishHandshake(channel, response);
                this.handshakeFuture.setSuccess();
                if (log.isDebugEnabled()) {
                    log.debug("握手成功");
                }
            } catch (Exception e) {
                log.error("连接失败", e);
                this.handshakeFuture.setFailure(e);
            }
        } else {
            if (msg instanceof PingWebSocketFrame) {
                ctx.writeAndFlush(new PongWebSocketFrame());
            } else if (msg instanceof CloseWebSocketFrame) {
                channel.close();
                handleCloseCode(((CloseWebSocketFrame) msg).statusCode());
                if (log.isDebugEnabled()) {
                    log.debug("连接关闭");
                }
            } else if (msg instanceof WebSocketFrame) {
                read(ctx, (WebSocketFrame) msg);
            } else {
                log.info("未知数据类型：" + msg);
            }
        }
    }

    protected void handleCloseCode(int statusCode) {
        if (log.isDebugEnabled()) {
            log.debug("连接关闭，statusCode: " + statusCode);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new PingWebSocketFrame());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    protected abstract void read(ChannelHandlerContext ctx, WebSocketFrame msg);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error("连接异常", e);
        ctx.close();
    }

    public void waitClose(long timeout, TimeUnit unit) throws InterruptedException {
        if (closeCountDownLatch != null) {
            closeCountDownLatch.await(timeout, unit);
        }
    }

}
