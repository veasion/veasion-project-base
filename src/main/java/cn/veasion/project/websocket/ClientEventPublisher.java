package cn.veasion.project.websocket;

import io.netty.channel.Channel;

/**
 * ClientEventPublisher
 *
 * @author luozhuowei
 * @date 2022/1/29
 */
@FunctionalInterface
public interface ClientEventPublisher {

    /**
     * 发布监听
     *
     * @param type    join / remove
     * @param channel channel
     * @param key     key
     */
    void publishEvent(String type, Channel channel, String key);

}
