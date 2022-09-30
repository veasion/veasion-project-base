package cn.veasion.project.utils;

import java.util.concurrent.CountDownLatch;

/**
 * CountDownLatchExt
 *
 * @author luozhuowei
 * @date 2022/5/21
 */
public class CountDownLatchExt<T> extends CountDownLatch {

    private T result;

    public CountDownLatchExt() {
        this(1);
    }

    public CountDownLatchExt(int count) {
        super(count);
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
