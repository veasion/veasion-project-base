package cn.veasion.project.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * StringUtils
 *
 * @author luozhuowei
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    private static final char SEPARATOR = '_';

    public static String toCamelCase(String s) {
        if (s == null) {
            return null;
        }

        s = s.toLowerCase();

        StringBuilder sb = new StringBuilder(s.length());
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == SEPARATOR) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 驼峰命名法工具
     *
     * @return toCamelCase(" hello_world ") == "helloWorld"
     * toCapitalizeCamelCase("hello_world") == "HelloWorld"
     * toUnderScoreCase("helloWorld") = "hello_world"
     */
    public static String toCapitalizeCamelCase(String s) {
        if (s == null) {
            return null;
        }
        s = toCamelCase(s);
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * 驼峰命名法工具
     *
     * @return toCamelCase(" hello_world ") == "helloWorld"
     * toCapitalizeCamelCase("hello_world") == "HelloWorld"
     * toUnderScoreCase("helloWorld") = "hello_world"
     */
    static String toUnderScoreCase(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            boolean nextUpperCase = true;

            if (i < (s.length() - 1)) {
                nextUpperCase = Character.isUpperCase(s.charAt(i + 1));
            }

            if ((i > 0) && Character.isUpperCase(c)) {
                if (!upperCase || !nextUpperCase) {
                    sb.append(SEPARATOR);
                }
                upperCase = true;
            } else {
                upperCase = false;
            }

            sb.append(Character.toLowerCase(c));
        }

        return sb.toString();
    }

    /**
     * 编辑算法，计算字符差异个数
     */
    public static int ld(String str1, String str2) {
        int[][] d;
        int n = str1.length();
        int m = str2.length();
        int i, j, temp;
        char ch1, ch2;
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++) {
            ch1 = str1.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                ch2 = str2.charAt(j - 1);
                if (ch1 == ch2) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + temp);
            }
        }
        return d[n][m];
    }

    private static int min(int one, int two, int three) {
        int min = one;
        if (two < min) {
            min = two;
        }
        if (three < min) {
            min = three;
        }
        return min;
    }

    /**
     * 获取ip地址
     */
    public static String getIp(HttpServletRequest request) {
        String UNKNOWN = "unknown";
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        String comma = ",";
        String localhost = "127.0.0.1";
        if (ip.contains(comma)) {
            ip = ip.split(",")[0];
        }
        if (localhost.equals(ip)) {
            // 获取本机真正的ip地址
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ignored) {
            }
        }
        return ip;
    }

    /**
     * 获取当前机器的IP
     *
     * @return /
     */
    public static String getLocalIp() {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface anInterface = interfaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration<InetAddress> inetAddresses = anInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddresses.nextElement();
                    // 排除loopback类型地址
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr.getHostAddress();
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress.getHostAddress();
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                return "";
            }
            return jdkSuppliedAddress.getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 值匹配
     *
     * @param value      值
     * @param matchOper  操作符
     *                   <ul>
     *                      <li>eq 等于</li>
     *                      <li>neq 不等于</li>
     *                      <li>like 包含</li>
     *                      <li>notLike 不包含</li>
     *                      <li>notNull 有值</li>
     *                      <li>isNull 无值</li>
     *                      <li>lt 小于</li>
     *                      <li>lte 小于等于</li>
     *                      <li>gt 大于</li>
     *                      <li>gte 大于等于</li>
     *                      <li>between 介于区间</li>
     *                      <li>all 包含全部</li>
     *                      <li>any 包含任意</li>
     *                      <li>notAll 不包含全部</li>
     *                      <li>notAny 不包含任意</li>
     *                   </ul>
     * @param matchValue 匹配值（string/number/date/array）
     */
    public static boolean matchValue(String value, String matchOper, Object matchValue) {
        try {
            switch (matchOper) {
                case "eq":
                    return String.valueOf(matchValue).equals(value);
                case "neq":
                    return !String.valueOf(matchValue).equals(value);
                case "like":
                    return value != null && value.contains(String.valueOf(matchValue));
                case "notLike":
                    return !(value != null && value.contains(String.valueOf(matchValue)));
                case "notNull":
                    return StringUtils.isNotEmpty(value);
                case "isNull":
                    return StringUtils.isEmpty(value);
                case "lt":
                case "lte":
                case "gt":
                case "gte":
                    if (StringUtils.isEmpty(value) || matchValue == null) {
                        return false;
                    }
                    if (!matchValue.toString().matches("\\d+")) {
                        matchValue = DateUtils.simpleParse(matchValue.toString());
                    }
                    if (matchValue instanceof Date) {
                        matchValue = ((Date) matchValue).getTime();
                        if (!value.matches("\\d+")) {
                            value = String.valueOf(DateUtils.simpleParse(value).getTime());
                        }
                    }
                    int compare = new BigDecimal(value).compareTo(new BigDecimal(matchValue.toString()));
                    switch (matchOper) {
                        case "lt":
                            return compare < 0;
                        case "lte":
                            return compare <= 0;
                        case "gt":
                            return compare > 0;
                        default:
                            return compare >= 0;
                    }
                case "between":
                    if (StringUtils.isEmpty(value) || matchValue == null) {
                        return false;
                    }
                    List<String> _matchValue = parseListValue(matchValue);
                    if (value.matches("\\d+")) {
                        // number
                        BigDecimal _value = new BigDecimal(value);
                        return _value.compareTo(new BigDecimal(_matchValue.get(0))) >= 0 && _value.compareTo(new BigDecimal(_matchValue.get(1))) <= 0;
                    } else {
                        // date
                        long _value = DateUtils.simpleParse(value).getTime();
                        return _value >= DateUtils.simpleParse(_matchValue.get(0)).getTime() && _value <= DateUtils.simpleParse(_matchValue.get(1)).getTime();
                    }
                case "all":
                    List<String> all = parseListValue(matchValue);
                    List<String> _all = parseListValue(value);
                    for (String s : all) {
                        if (!_all.contains(s)) {
                            return false;
                        }
                    }
                    return true;
                case "any":
                    List<String> any = parseListValue(matchValue);
                    List<String> _any = parseListValue(value);
                    for (String s : any) {
                        if (_any.contains(s)) {
                            return true;
                        }
                    }
                    return false;
                case "notAll":
                    List<String> notAll = parseListValue(matchValue);
                    List<String> _notAll = parseListValue(value);
                    for (String s : notAll) {
                        if (_notAll.contains(s)) {
                            return false;
                        }
                    }
                    return true;
                case "notAny":
                    List<String> notAny = parseListValue(matchValue);
                    List<String> _notAny = parseListValue(value);
                    for (String s : notAny) {
                        if (!_notAny.contains(s)) {
                            return true;
                        }
                    }
                    return false;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static List<String> parseListValue(Object matchValue) {
        if (matchValue instanceof String) {
            return JSONArray.parseArray(matchValue.toString(), String.class);
        } else {
            return JSONArray.parseArray(JSON.toJSONString(matchValue), String.class);
        }
    }

}
