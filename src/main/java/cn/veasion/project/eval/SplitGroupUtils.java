package cn.veasion.project.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * SplitGroupUtils
 *
 * @author luozhuowei
 * @date 2020/10/15
 */
public class SplitGroupUtils {

    public static void main(String[] args) {
        String text1 = "list[list[0][var]].name";
        List<Group> list1 = group(text1, "[", "]", true);
        System.out.println(text1);
        System.out.println(list1);
        String text2 = "1+(2+3*(1+3))*5";
        List<Group> list2 = group(text2, "(", ")", true);
        System.out.println(text2);
        System.out.println(list2);
        String text3 = "create_time >= date_sub(now(), interval 1 day) and age > 0 and (name like 'xx2' or name like 'xx1' and (sex = 1 and username = '123'))";
        List<Group> list3 = group(text3, "(", ")", true, s -> s.contains(" and ") || s.contains(" AND ") || s.contains(" or ") || s.contains(" OR "));
        System.out.println(text3);
        System.out.println(list3);
    }

    public static List<Group> group(String text, String left, String right, boolean fill) {
        return group(text, left, right, fill, false, null);
    }

    public static List<Group> group(String text, String left, String right, boolean fill, Function<String, Boolean> filter) {
        return group(text, left, right, fill, false, filter);
    }

    public static List<Group> group(String text, String left, String right, boolean fill, boolean checkLR) {
        return group(text, left, right, fill, checkLR, null);
    }

    public static List<Group> group(String text, String left, String right, boolean fill, boolean checkLR, Function<String, Boolean> filter) {
        List<Integer> leftList = findIndex(text, left, filter);
        List<Integer> rightList = findIndex(text, right, filter);
        if (checkLR && leftList.size() != rightList.size()) {
            throw new RuntimeException("语法错误，缺少 => " + (leftList.size() > rightList.size() ? right : left));
        }
        List<Group> list = new ArrayList<>(leftList.size());
        for (int i = leftList.size() - 1; i >= 0; i--) {
            int leftIdx = leftList.get(i);
            for (int j = 0; j < rightList.size(); j++) {
                int rightIdx = rightList.get(j);
                if (leftIdx < rightIdx) {
                    list.add(new Group(leftIdx, rightIdx, 1, text, left, right));
                    rightList.remove(j);
                    break;
                }
            }
        }
        list.sort(Comparator.comparingInt(Group::getLeft));
        list = buildGroupTree(list, -1, Integer.MAX_VALUE);
        if (fill) {
            if (list == null) {
                list = new ArrayList<>();
            }
            fillValues(list, text, 0, text.length() - 1);
        }
        return list;
    }

    private static void fillValues(List<Group> list, String text, int start, int end) {
        if (list.isEmpty()) {
            if (start <= end) {
                list.add(new Group(start, end, 0, text, null, null));
            }
            return;
        }
        Group group = null;
        for (int i = 0; i < list.size(); i++) {
            group = list.get(i);
            if (group.left > start) {
                list.add(i++, new Group(start, group.left - 1, 0, text, null, null));
            }
            start = group.right + 1;
            if (group.children != null) {
                fillValues(group.children, text, group.left + 1, group.right - 1);
            }
        }
        if (group.right < end) {
            list.add(new Group(group.right + 1, end, 0, text, null, null));
        }
    }

    private static List<Group> buildGroupTree(List<Group> list, int left, int right) {
        List<Group> trees = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Group iGroup = list.get(i);
            if (iGroup.left > left && iGroup.right < right) {
                if (i + 1 <= list.size() - 1) {
                    Group group_1 = list.get(i + 1);
                    if (iGroup.left < group_1.left && iGroup.right > group_1.right) {
                        iGroup.children = buildGroupTree(list, iGroup.left, iGroup.right);
                        left = iGroup.right;
                    }
                }
                trees.add(iGroup);
            }
        }
        return trees.isEmpty() ? null : trees;
    }

    private static List<Integer> findIndex(String text, String s, Function<String, Boolean> filter) {
        int index = -1, last = 0;
        List<Integer> list = new ArrayList<>();
        while ((index = text.indexOf(s, index + 1)) != -1) {
            if (filter == null || Boolean.TRUE.equals(filter.apply(text.substring(last, index)))) {
                list.add(index);
            }
        }
        return list;
    }

    public static class Group {
        private int left;
        private int right;
        private int type;
        private String value;
        private String context;
        private List<Group> children;

        public Group() {
        }

        Group(int left, int right, int type, String sub, String l, String r) {
            this.left = left;
            this.right = right;
            this.type = type;
            this.value = sub.substring(left, right + 1);
            this.context = sub.substring(left + (l != null ? l.length() : 0), right - (r != null ? r.length() : 0) + 1);
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public List<Group> getChildren() {
            return children;
        }

        public void setChildren(List<Group> children) {
            this.children = children;
        }

        @Override
        public String toString() {
            return "{ left: " + left +
                    ", right: " + right +
                    ", type: " + type +
                    ", value: " + value +
                    ", context: " + context +
                    ", children: " + children +
                    " }";
        }
    }
}
