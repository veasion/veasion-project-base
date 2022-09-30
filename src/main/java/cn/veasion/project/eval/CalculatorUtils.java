package cn.veasion.project.eval;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CalculatorUtils
 *
 * @author luozhuowei
 * @date 2020/10/28
 */
public class CalculatorUtils {

    private static final int SCALE = 6;
    private static final int ROUNDING_MODE = BigDecimal.ROUND_HALF_UP;
    private static final Pattern OPERATOR = Pattern.compile("[+\\-*/×÷%√^]");
    public static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static void main(String[] args) {
        testCalculate("-2");
        testCalculate("√8");
        testCalculate("2^3");
        testCalculate("10%6");
        testCalculate("1+(-4)");
        testCalculate("√(3*3)");
        testCalculate("2×3÷3");
        testCalculate("1+3√(3*3*3)");
        testCalculate("2+5*2-6/3");
        testCalculate("(1+2+3)^2+4");
        testCalculate("2+5*(2-6*(3+1))/3");
        testCalculate("4.99+(5.99+6.99)*1.06^2+√4");

        System.out.println("======================");
        JSONObject temp1 = JSONObject.parseObject("{\"a\":1,\"b\":2,\"c\":3,\"aa\":-1,\"bb\":-2,\"cc\":-3,\"d\":4.99,\"s\":8}");
        testCalculate(temp1, "-b");
        testCalculate(temp1, "√s");
        testCalculate(temp1, "b^c");
        testCalculate(temp1, "(s+b)%(c*b)");
        testCalculate(temp1, "a+(-4)");
        testCalculate(temp1, "√(c*c)");
        testCalculate(temp1, "b×c÷c");
        testCalculate(temp1, "a+c√(c*c*c)");
        testCalculate(temp1, "b+5*b-6/c");
        testCalculate(temp1, "(a+b+c)^b+4");
        testCalculate(temp1, "b+5*(b-6*(c+a))/c");
        testCalculate(temp1, "d+(5.99+6.99)*1.06^b+√(a+c)");

        System.out.println("======================");
        JSONObject temp2 = JSONObject.parseObject("{\"商品金额\": 10.25,\"销售数量\": 10,\"优惠金额\":2}");
        testCalculate(temp2, "商品金额*销售数量-优惠金额+默认值|5");

        System.out.println("======================");
        JSONObject temp3 = JSONObject.parseObject("{\"order\":{\"product_amt\":10.25,\"num\":10}}");
        Function<String, ?> function = s -> {
            if ("random".equals(s)) {
                return Math.random();
            }
            return temp3.get(s);
        };
        testCalculate(function, "order.product_amt * order.num + random");
        testCalculate(function, "order.product_amt * order.num + random");
    }

    private static BigDecimal testCalculate(String str) {
        return testCalculate(null, str);
    }

    private static BigDecimal testCalculate(Object object, String str) {
        // 计算结果
        BigDecimal result = calculate(object, str);
        // 保留两位小数
        System.out.println(str + "=" + decimalFormat(result, 2));
        return result;
    }

    /**
     * 计算（保留N位小数）
     *
     * @param str 运算式
     * @param n   保留N位小数
     */
    public static String calculate(String str, int n) {
        BigDecimal result = calculate(null, str);
        return decimalFormat(result, n);
    }

    /**
     * 计算（保留N位小数）
     *
     * @param object 对象
     * @param str    运算式
     * @param n      保留N位小数
     */
    public static String calculate(Object object, String str, int n) {
        BigDecimal result = calculate(object, str);
        return decimalFormat(result, n);
    }

    /**
     * 计算
     *
     * @param str 运算式
     */
    public static BigDecimal calculate(String str) {
        return calculate(null, str);
    }

    /**
     * 计算
     *
     * @param object 数据
     * @param str    运算式
     */
    public static BigDecimal calculate(Object object, String str) {
        try {
            return calcGroups(object, SplitGroupUtils.group(str, "(", ")", true));
        } catch (Exception e) {
            throw new RuntimeException(String.format("计算异常：%s => %s", str, e.getMessage()), e);
        }
    }

    /**
     * 格式化
     *
     * @param value 数值
     * @param n     保留N位小数
     */
    public static String decimalFormat(BigDecimal value, int n) {
        if (value == null) {
            return null;
        }
        String pattern = n > 0 ? "#." : "#";
        for (int i = 0; i < n && n <= 10; i++) {
            pattern = pattern.concat("#");
        }
        return new DecimalFormat(pattern).format(value);
    }

    private static BigDecimal calcGroups(Object object, List<SplitGroupUtils.Group> list) {
        StringBuilder sb = new StringBuilder();
        for (SplitGroupUtils.Group group : list) {
            if (group.getType() == 1) {
                BigDecimal decimal;
                if (group.getChildren() != null) {
                    decimal = calcGroups(object, group.getChildren());
                } else {
                    decimal = flatCalculate(object, group.getContext());
                }
                sb.append(decimalFormat(decimal, SCALE));
            } else {
                sb.append(group.getContext());
            }
        }
        return flatCalculate(object, sb.toString());
    }

    private static BigDecimal flatCalculate(Object object, final String eval) {
        String str = eval;
        if (str == null || "".equals(str = str.trim())) {
            return null;
        }
        int start = 0, maxLevel = -1;
        Matcher matcher = OPERATOR.matcher(str);
        ValueLink valueLink, pre = null;
        while (matcher.find()) {
            String group = matcher.group().trim();
            String leftVal = str.substring(start, matcher.start()).trim();
            Operator operator = Operator.of(group);
            BigDecimal val = "".equals(leftVal) ? null : toBigDecimal(object, leftVal);
            if (operator == null) {
                throw new RuntimeException("不支持运算符: " + group);
            } else if (val == null && !operator.leftNullable) {
                if (StringUtils.isNotEmpty(leftVal)) {
                    throw new RuntimeException("“" + leftVal + "” 未知");
                } else {
                    throw new RuntimeException("不支持单独运算：" + group);
                }
            }
            valueLink = new ValueLink(val, operator, null);
            valueLink.pre = pre;
            if (pre != null) {
                pre.next = valueLink;
                pre.right = valueLink.left;
            }
            pre = valueLink;
            if (operator.level > maxLevel) {
                maxLevel = operator.level;
            }
            start = matcher.end();
        }
        if (pre == null) {
            return toBigDecimal(object, str);
        }
        if (start < str.length()) {
            String endStr = str.substring(start).trim();
            pre.right = toBigDecimal(object, endStr);
        } else {
            throw new RuntimeException("不能以运算符结尾：" + eval);
        }
        while (true) {
            if (pre.left == null && pre.pre != null && pre.pre.right == null) {
                pre.pre.right = pre.operator.apply(null, pre.right);
                pre.pre.next = pre.next;
            }
            if (pre.pre == null) {
                break;
            }
            pre = pre.pre;
        }
        return simpleCalculate(pre, maxLevel);
    }

    @SuppressWarnings("unchecked")
    private static BigDecimal toBigDecimal(Object object, String str) {
        if (object == null || NUMBER.matcher(str).matches()) {
            return new BigDecimal(str);
        }
        Object result;
        if (object instanceof BiFunction) {
            String defValue = str.contains("|") ? str.substring(str.lastIndexOf("|") + 1).trim() : null;
            result = EvalAnalysisUtils.parse(str, (Function<String, ?>) v -> ((BiFunction<String, String, ?>) object).apply(v, defValue));
        } else {
            result = EvalAnalysisUtils.parse(str, object);
        }
        if (result == null) {
            return null;
        }
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        } else {
            return new BigDecimal(result.toString());
        }
    }

    private static BigDecimal simpleCalculate(ValueLink link, int maxLevel) {
        while (true) {
            while (link.pre != null) {
                link = link.pre;
            }
            if (link.next == null) {
                break;
            }
            int newMaxLevel = -1;
            while (true) {
                if (link.operator.level >= maxLevel) {
                    BigDecimal val = link.operator.apply(link.left, link.right);
                    if (link.pre != null) {
                        link.pre.right = val;
                        link.pre.next = link.next;
                    }
                    if (link.next != null) {
                        link.next.left = val;
                        link.next.pre = link.pre;
                    }
                } else if (link.operator.level > newMaxLevel) {
                    newMaxLevel = link.operator.level;
                }
                if (link.next != null) {
                    link = link.next;
                } else {
                    maxLevel = newMaxLevel;
                    break;
                }
            }
        }
        return link.operator.apply(link.left, link.right);
    }

    enum Operator {

        ADD("+", 1, false, BigDecimal::add),
        SUBTRACT("-", 1, true, (a, b) -> a == null ? b.negate() : a.subtract(b)),
        MULTIPLY("*", "×", 2, false, BigDecimal::multiply),
        DIVIDE("/", "÷", 2, false, (a, b) -> a.divide(b, SCALE, ROUNDING_MODE)),
        DIVIDE_REMAINDER("%", 2, false, (a, b) -> a.divideAndRemainder(b)[1]),
        POW("^", 3, false, (a, b) -> BigDecimal.valueOf(Math.pow(a.doubleValue(), b.doubleValue()))),
        SQRT("√", 3, true, (a, b) -> {
            if (a != null) {
                // 开N次方
                return BigDecimal.valueOf(Math.pow(b.doubleValue(), BigDecimal.ONE.divide(a, SCALE, ROUNDING_MODE).doubleValue()));
            } else {
                // 开平方
                return BigDecimal.valueOf(Math.sqrt(b.doubleValue()));
            }
        });

        private int level;
        private String op1;
        private String op2;
        private boolean leftNullable;
        private BinaryOperator<BigDecimal> function;

        Operator(String op1, int level, boolean leftNullable, BinaryOperator<BigDecimal> function) {
            this(op1, null, level, leftNullable, function);
        }

        Operator(String op1, String op2, int level, boolean leftNullable, BinaryOperator<BigDecimal> function) {
            this.level = level;
            this.op2 = op2;
            this.leftNullable = leftNullable;
            this.op1 = Objects.requireNonNull(op1);
            this.function = Objects.requireNonNull(function);
        }

        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            if (b == null) {
                throw new RuntimeException(String.format("%s 后面必须位数字", op1));
            }
            if (a == null && !leftNullable) {
                throw new RuntimeException(String.format("格式错误：%s %s", op1, String.valueOf(b)));
            }
            return function.apply(a, b);
        }

        public static Operator of(String operator) {
            if (operator == null) {
                return null;
            }
            for (Operator value : values()) {
                if (operator.equals(value.op1) || operator.equals(value.op2)) {
                    return value;
                }
            }
            return null;
        }
    }

    static class ValueLink {
        ValueLink pre;
        BigDecimal left;
        Operator operator;
        BigDecimal right;
        ValueLink next;

        ValueLink(BigDecimal left, Operator operator, BigDecimal right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }
}