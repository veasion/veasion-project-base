package cn.veasion.project.eval;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * CondAnalysisUtils
 *
 * @author luozhuowei
 * @date 2020/11/20
 */
public class CondAnalysisUtils {

    private static final String SPLIT_OR = "\\|\\|", SPLIT_AND = "&&";
    private static final String TRUE = "true", FALSE = "false", _OR = "||", _AND = "&&";

    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderSource", 0);
        map.put("orderStatus", 1010);
        map.put("orderPaymentType", 1);

        java.util.function.BiConsumer<String, Object> eval = (str, object) -> System.out.println(str + "\n==> " + eval(str, object));
        eval.accept("!false", map);
        eval.accept("1==1", map);
        eval.accept("'abc' == 'abc'", map);
        eval.accept("orderType == null", map);
        eval.accept("orderPaymentType not in(2,3)", map);
        eval.accept("orderStatus in (1999,9000) && orderSource != 5", map);
        eval.accept("orderStatus in (1010,1060,1070) && !(orderSource in (1,5))", map);
        eval.accept("orderPaymentType == 1 && orderStatus == 1010 && orderSource in (0,1,6,7,11,13)", map);
        eval.accept("orderPaymentType == 2 && orderStatus == 1010 && orderSource in (0,1,6,11,13)", map);
        eval.accept("orderPaymentType == 0 && orderStatus == 1010 || !(orderPaymentType in(1,2) && orderStatus != 9000) || orderStatus > 0", map);
    }

    public static boolean eval(String str, Object object) {
        return groupAnalysis(SplitGroupUtils.group(str, "(", ")", true), object);
    }

    private static boolean groupAnalysis(List<SplitGroupUtils.Group> list, Object object) {
        StringBuilder sb = new StringBuilder();
        for (SplitGroupUtils.Group group : list) {
            String context = group.getContext();
            if (group.getType() == 1) {
                if (group.getChildren() != null) {
                    sb.append(groupAnalysis(group.getChildren(), object));
                } else if (context.contains(_AND) || context.contains(_OR)) {
                    sb.append(orAnalysis(context, object));
                } else {
                    sb.append(group.getValue());
                }
            } else {
                sb.append(context);
            }
        }
        return orAnalysis(sb.toString(), object);
    }

    private static boolean orAnalysis(String str, Object object) {
        String[] orSplits = str.split(SPLIT_OR);
        for (String orSplit : orSplits) {
            if (andAnalysis(orSplit, object)) {
                return true;
            }
        }
        return false;
    }

    private static boolean andAnalysis(String str, Object object) {
        String[] andSplits = str.split(SPLIT_AND);
        for (String andSplit : andSplits) {
            if (!oneAnalysis(andSplit, object)) {
                return false;
            }
        }
        return true;
    }

    private static boolean oneAnalysis(final String str, Object object) {
        String text = str.trim();
        boolean non = text.startsWith("!");
        if (non) {
            text = text.substring(text.indexOf("!") + 1);
        }
        if (TRUE.equals(text)) {
            return !non;
        } else if (FALSE.equals(text)) {
            return non;
        }
        Operator operator = null;
        String eval = null, value = null;
        char[] chars = text.toCharArray();
        to:
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case '=':
                case '!':
                case '>':
                case '<':
                case ' ':
                    operator = matchesOperator(chars, i);
                    if (operator != null) {
                        eval = text.substring(0, i).trim();
                        value = text.substring(i + operator.opt.length()).trim();
                        break to;
                    }
                    break;
            }
        }
        if (operator == null) {
            throw new RuntimeException(String.format("表达式格式错误: %s", str));
        }
        Object result = null;
        try {
            if ("".equals(eval) || isString(eval)) {
                return operator.test(getString(eval), value);
            } else if (isNumber(eval) && isNumber(value)) {
                return operator.test(eval, value);
            }
            result = EvalAnalysisUtils.parse(eval, object);
            return operator.test(result, value);
        } catch (Exception e) {
            if (result != null) {
                throw new RuntimeException(String.format("表达式错误：%s ==> %s %s %s", str, String.valueOf(result), operator.opt, value), e);
            } else {
                throw new RuntimeException(String.format("表达式错误：%s", str), e);
            }
        }
    }

    private static boolean isNumber(String str) {
        return str != null && str.matches("\\d+");
    }

    private static boolean isString(String str) {
        return str != null && (str.startsWith("'") && str.endsWith("'") || str.startsWith("\"") && str.endsWith("\""));
    }

    private static String getString(String str) {
        if (isString(str)) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static Operator matchesOperator(char[] chars, int i) {
        int len = chars.length;
        List<Operator> operators = Operator.START_OPERATOR.get(chars[i]);
        if (operators == null) {
            return null;
        }
        for (Operator operator : operators) {
            int optLen = operator.opt.length();
            if (i + optLen > len) {
                continue;
            }
            boolean eq = true;
            for (int j = 0; j < optLen; j++) {
                if (chars[i + j] != operator.opt.charAt(j)) {
                    eq = false;
                    break;
                }
            }
            if (!eq) {
                continue;
            }
            if (operator.isWord) {
                if (i + optLen == len) {
                    return operator;
                }
                switch (chars[i + optLen]) {
                    case ' ':
                    case '(':
                    case '\'':
                    case '"':
                        return operator;
                }
            } else {
                return operator;
            }
        }
        return null;
    }

    enum Operator {

        EQ("==", (object, s) -> s.equalsIgnoreCase(String.valueOf(object))),
        NEQ("!=", (object, s) -> !Operator.EQ.test(object, s)),
        GT(">", (object, s) -> Optional.ofNullable(object).map(String::valueOf).map(o -> new BigDecimal(o).compareTo(new BigDecimal(s)) > 0).orElse(false)),
        LT("<", (object, s) -> Optional.ofNullable(object).map(String::valueOf).map(o -> new BigDecimal(o).compareTo(new BigDecimal(s)) < 0).orElse(false)),
        GTE(">=", (object, s) -> Optional.ofNullable(object).map(String::valueOf).map(o -> new BigDecimal(o).compareTo(new BigDecimal(s)) >= 0).orElse(false)),
        LTE("<=", (object, s) -> Optional.ofNullable(object).map(String::valueOf).map(o -> new BigDecimal(o).compareTo(new BigDecimal(s)) <= 0).orElse(false)),
        IN(" in", true, (object, s) -> {
            if (object == null) {
                return false;
            }
            if (s.startsWith("(") && s.endsWith(")")) {
                s = s.substring(1, s.length() - 1);
            }
            Set<String> set = new HashSet<>();
            for (String str : s.split(",")) {
                set.add(getString(str.trim()));
            }
            return set.contains(String.valueOf(object));
        }),
        NOT_IN(" not in", true, (object, s) -> !Operator.IN.test(object, s));

        private String opt;
        private boolean isWord;
        private BiFunction<Object, String, Boolean> function;
        public static final Map<Character, List<Operator>> START_OPERATOR = new HashMap<>(values().length);

        static {
            for (Operator value : values()) {
                START_OPERATOR.compute(value.opt.charAt(0), (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(value);
                    return v;
                });
            }
            START_OPERATOR.values().forEach(list -> list.sort(Collections.reverseOrder(Comparator.comparingInt((o) -> o.opt.length()))));
        }

        Operator(String opt, BiFunction<Object, String, Boolean> function) {
            this(opt, false, function);
        }

        Operator(String opt, boolean isWord, BiFunction<Object, String, Boolean> function) {
            this.opt = opt;
            this.isWord = isWord;
            this.function = function;
            if (isWord && !opt.startsWith(" ")) {
                throw new RuntimeException("isWord操作符必须以空格开头: " + opt);
            }
        }

        public boolean test(Object object, String s) {
            return Boolean.TRUE.equals(this.function.apply(object, getString(s)));
        }
    }
}
