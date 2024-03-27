package cn.veasion.project.utils;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * SseUtils
 *
 * @author luozhuowei
 * @date 2024/3/2
 */
public class SseUtils {

    public static SseEmitter create(ExecutorService executorService, Consumer<SseEmitter> consumer) {
        return create(executorService, consumer, null);
    }

    public static SseEmitter create(ExecutorService executorService, Consumer<SseEmitter> consumer, Long timeout) {
        SseEmitter sseEmitter = new SseEmitter(timeout);
        try {
            Future<?> future = executorService.submit(() -> {
                try {
                    consumer.accept(sseEmitter);
                    sseEmitter.complete();
                } catch (Exception e) {
                    sseEmitter.completeWithError(e);
                }
            });
            sseEmitter.onError(e -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            });
            sseEmitter.onTimeout(() -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            });
        } catch (Exception e) {
            sseEmitter.completeWithError(e);
        }
        return sseEmitter;
    }

}
