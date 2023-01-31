package cn.veasion.project.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class PageUtil extends cn.hutool.core.util.PageUtil {

    @SuppressWarnings("unchecked")
    public static <T> void batch(Consumer<T[]> consumer, T[] array, int maxSize) {
        if (array == null || array.length == 0) {
            return;
        }
        if (array.length <= maxSize) {
            consumer.accept(array);
            return;
        }
        int s = 0;
        T[] newArray = (T[]) Array.newInstance(array[0].getClass(), maxSize);
        for (T value : array) {
            newArray[s++] = value;
            if (s >= maxSize) {
                s = 0;
                consumer.accept(newArray);
            }
        }
        if (s > 0) {
            consumer.accept(Arrays.copyOfRange(newArray, 0, s));
        }
    }

    public static <T> void batch(Consumer<List<T>> consumer, List<T> entityList, int maxBatchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return;
        }
        if (entityList.size() <= maxBatchSize) {
            consumer.accept(entityList);
        } else {
            int num = entityList.size() / maxBatchSize;
            for (int i = 0; i < num; i++) {
                consumer.accept(entityList.subList(i * maxBatchSize, (i + 1) * maxBatchSize));
            }
            int last = num * maxBatchSize;
            if (entityList.size() > last) {
                consumer.accept(entityList.subList(last, entityList.size()));
            }
        }
    }

    public static <T> List<T> toPage(int page, int size, List<T> list) {
        int fromIndex = (page - 1) * size;
        int toIndex = fromIndex + size;
        if (fromIndex > list.size()) {
            return new ArrayList<>();
        } else if (toIndex >= list.size()) {
            return list.subList(fromIndex, list.size());
        } else {
            return list.subList(fromIndex, toIndex);
        }
    }

}
