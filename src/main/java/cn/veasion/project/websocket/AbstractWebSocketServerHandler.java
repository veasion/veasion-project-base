package cn.veasion.project.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractWebSocketServerHandler
 *
 * @author luozhuowei
 * @date 2022/1/28
 */
@ChannelHandler.Sharable
public abstract class AbstractWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected WebSocketServer webSocketServer;

    void setWebSocketServer(WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        webSocketServer.join(ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        webSocketServer.remove(ctx.channel());
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
        if (!(msg instanceof PongWebSocketFrame)) {
            read(ctx, msg);
        }
        if (msg instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame());
        }
    }

    protected abstract void read(ChannelHandlerContext ctx, WebSocketFrame frame);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("连接异常", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // 超时断开连接
                ctx.disconnect();
                ctx.close();
                log.debug("超时断开连接：" + WebSocketServer.channelKey(ctx.channel()));
            } else if (e.state() == IdleState.WRITER_IDLE) {
                // ping 需要客户端回复 pong
                ctx.writeAndFlush(new PingWebSocketFrame());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void clearAll() {
        if (webSocketServer != null) {
            webSocketServer.removeAll();
        }
    }

}
