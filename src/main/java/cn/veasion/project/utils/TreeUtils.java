package cn.veasion.project.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TreeUtils
 *
 * @author luozhuowei
 * @date 2022/8/20
 */
public class TreeUtils {

    public static <T, ID> List<T> buildTree(List<T> list, Function<T, ID> getId, Function<T, ID> getPid, BiConsumer<T, T> addChildren) {
        return buildTree(list, (t) -> getPid.apply(t) == null, getId, getPid, addChildren);
    }

    public static <T, ID> List<T> buildTree(List<T> list, Function<T, Boolean> isRootParent, Function<T, ID> getId, Function<T, ID> getPid, BiConsumer<T, T> addChildren) {
        List<T> treeList = new ArrayList<>();
        Set<ID> ids = new HashSet<>(list.size());
        for (T t : list) {
            if (isRootParent.apply(t)) {
                treeList.add(t);
            }
            ID id = getId.apply(t);
            for (T o : list) {
                if (Objects.equals(getPid.apply(o), id)) {
                    addChildren.accept(t, o);
                    ids.add(getId.apply(o));
                }
            }
        }
        if (treeList.size() == 0) {
            treeList = list.stream().filter(s -> !ids.contains(getId.apply(s))).collect(Collectors.toList());
        }
        return treeList;
    }

}
