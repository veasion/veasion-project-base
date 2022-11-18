package cn.veasion.project.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * StringComparator
 *
 * @author luozhuowei
 * @date 2022/11/18
 */
public class StringComparator implements Comparator<String> {

    private char[] str1, str2;
    private int pos1, pos2, len1, len2;
    private static final Map<Character, Character> convertMap;

    static {
        convertMap = new HashMap<>();
        convertMap.put('零', '0');
        convertMap.put('一', '1');
        convertMap.put('二', '2');
        convertMap.put('三', '3');
        convertMap.put('四', '4');
        convertMap.put('五', '5');
        convertMap.put('六', '6');
        convertMap.put('七', '7');
        convertMap.put('八', '8');
        convertMap.put('九', '9');
    }

    public static void addConvert(Character a, Character b) {
        convertMap.put(a, b);
    }

    @Override
    public int compare(String s1, String s2) {
        if (s1 == null) {
            return -1;
        } else if (s2 == null) {
            return 1;
        }
        str1 = toChars(s1);
        str2 = toChars(s2);
        len1 = str1.length;
        len2 = str2.length;
        pos1 = pos2 = 0;
        int result = 0;
        while (result == 0 && pos1 < len1 && pos2 < len2) {
            char ch1 = str1[pos1];
            char ch2 = str2[pos2];
            if (Character.isDigit(ch1)) {
                result = Character.isDigit(ch2) ? compareNumbers() : -1;
            } else if (Character.isLetter(ch1)) {
                result = Character.isLetter(ch2) ? compareOther(true) : 1;
            } else {
                result = Character.isDigit(ch2) ? 1
                        : Character.isLetter(ch2) ? -1
                        : compareOther(false);
            }
            pos1++;
            pos2++;
        }
        return result == 0 ? len1 - len2 : result;
    }

    private char[] toChars(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            chars[i] = convertMap.getOrDefault(c, c);
        }
        return chars;
    }

    private int compareNumbers() {
        int end1 = pos1 + 1;
        while (end1 < len1 && Character.isDigit(str1[end1])) {
            end1++;
        }
        int fullLen1 = end1 - pos1;
        while (pos1 < end1 && str1[pos1] == '0') {
            pos1++;
        }
        int end2 = pos2 + 1;
        while (end2 < len2 && Character.isDigit(str2[end2])) {
            end2++;
        }
        int fullLen2 = end2 - pos2;
        while (pos2 < end2 && str2[pos2] == '0') {
            pos2++;
        }
        int delta = (end1 - pos1) - (end2 - pos2);
        if (delta != 0) {
            return delta;
        }
        while (pos1 < end1 && pos2 < end2) {
            delta = str1[pos1++] - str2[pos2++];
            if (delta != 0) {
                return delta;
            }
        }
        pos1--;
        pos2--;
        return fullLen2 - fullLen1;
    }

    private int compareOther(boolean isLetters) {
        char ch1 = str1[pos1];
        char ch2 = str2[pos2];
        if (ch1 == ch2) {
            return 0;
        }
        if (isLetters) {
            ch1 = Character.toUpperCase(ch1);
            ch2 = Character.toUpperCase(ch2);
            if (ch1 != ch2) {
                ch1 = Character.toLowerCase(ch1);
                ch2 = Character.toLowerCase(ch2);
            }
        }
        return ch1 - ch2;
    }

}
