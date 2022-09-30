package cn.veasion.project.eval.syntax;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DialogContext
 *
 * @author luozhuowei
 * @date 2022/9/3
 */
public class DialogContext {

    private String dialog;
    private List<Syntax> templates;
    private Map<String, Object> entityMap;
    private boolean exact = true; // 精准匹配

    public String getDialog() {
        return dialog;
    }

    public void setDialog(String dialog) {
        this.dialog = dialog;
    }

    public List<Syntax> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Syntax> templates) {
        this.templates = templates;
    }

    public Map<String, Object> getEntityMap() {
        return entityMap;
    }

    public void setEntityMap(Map<String, Object> entityMap) {
        this.entityMap = entityMap;
    }

    public void bindEntity(String var, Object object) {
        if (entityMap == null) {
            entityMap = new HashMap<>();
        }
        entityMap.put(var, object);
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }

}
