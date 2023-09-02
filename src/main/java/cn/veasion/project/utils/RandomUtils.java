package cn.veasion.project.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * RandomUtils
 */
public class RandomUtils extends org.apache.commons.lang3.RandomUtils {

    private static final char[] array = {'5', 'v', '3', 'p', 'T', 'E', 'F', 'r', '6', 's', 'R', 'u', 'S', '7', 'g', '8', 'f',
            'h', 'i', 'j', 'k', 'l', 'm', 'C', 'D', 'n', 'o', 'w', 'x', '1', 'y', 'z', 'A', 'B', 'b', 'c',
            '0', 'd', 'G', '2', 'H', 'I', '_', 'J', '4', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 't', 'q',
            'U', '9', 'a', 'e', 'V', 'W', 'X', 'Y', 'Z'};

    private static final Object randomLock = new Object();

    /**
     * 随机一组数据
     *
     * @param start  开始（包含）
     * @param end    结束（不包含）
     * @param length 长度
     * @param unique 随机数据是否保证唯一
     */
    public static int[] randoms(int start, int end, int length, boolean unique, boolean sort) {
        int[] array = new int[length];
        if (unique) {
            int maxUniqueCheck = length * 20;
            for (int i = 0; i < length; i++) {
                flag:
                for (int j = 0; j < maxUniqueCheck; j++) {
                    array[i] = nextInt(start, end);
                    for (int s = i - 1; s >= 0; s--) {
                        if (array[s] == array[i]) {
                            continue flag;
                        }
                    }
                    break;
                }
            }
        } else {
            for (int i = 0; i < length; i++) {
                array[i] = nextInt(start, end);
            }
        }
        if (sort) {
            Arrays.sort(array);
        }
        return array;
    }

    /**
     * 随机字符串
     */
    public static String random(int len) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(array[random.nextInt(array.length)]);
        }
        return sb.toString();
    }

    /**
     * 随机数字
     */
    public static String randomNumber(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    /**
     * 根据数字基数生成字符串
     */
    public static String convertStr(long number) {
        char[] arr = String.valueOf(number).toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : arr) {
            sb.append(array[Integer.parseInt(String.valueOf(c))]);
        }
        return sb.toString();
    }

    /**
     * 18位唯一字符串（单机）
     */
    public static String unique() {
        synchronized (randomLock) {
            int l = array.length / 10;
            Random random = new Random();
            long number = System.currentTimeMillis();
            char[] arr = String.valueOf(number).toCharArray();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                int value = Integer.parseInt(String.valueOf(arr[i]));
                if (i < 6) {
                    sb.append(array[random.nextInt(l) + value * l]);
                } else {
                    sb.append(array[value]);
                }
            }
            sb.append(random(5));
            return sb.toString();
        }
    }

    public static <T> T random(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        }
        return list.get((int) (Math.random() * list.size()));
    }

}
