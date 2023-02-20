package cn.veasion.project.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * AbstractWebSocketAuthenticationHandler
 *
 * @author luozhuowei
 * @date 2022/1/28
 */
@ChannelHandler.Sharable
public abstract class AbstractWebSocketAuthenticationHandler extends ChannelInboundHandlerAdapter {

    protected WebSocketServer webSocketServer;

    void setWebSocketServer(WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            Map<String, String> params = new HashMap<>();
            int idx = uri.indexOf("?");
            if (idx > -1) {
                String[] split = uri.substring(idx + 1).split("&");
                for (String param : split) {
                    String[] p = param.split("=");
                    if (p.length == 1) {
                        continue;
                    }
                    params.put(p[0], URLDecoder.decode(p[1], "UTF-8"));
                }
                request.setUri(uri.substring(0, idx));
            }
            // 认证
            boolean success = handleAuthentication(ctx, request, params);
            if (!success) {
                ctx.disconnect();
                ctx.close();
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    protected abstract boolean handleAuthentication(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, String> params);

}
