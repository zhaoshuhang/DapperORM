package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.common.FilterConsumer;
import com.sinux.happy.orm.common.JsonDateValueProcessor;
import com.sinux.happy.orm.common.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JsonConfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class QueryResult<T> {
    private List<Map<String, Object>> mapList;
    private List<Map<String, Object>> mapTree;
    private List<T> objList;
    private Class<T> clazz;

    protected QueryResult(Class<T> clazz, List<Map<String, Object>> map) {
        this.mapList = map;
        this.clazz = clazz;
    }

    /**
     * 执行一个查询，并将结果存为一个单层的Map对象。
     * 例如执行结果是 name=1, id=2,a.b.name=3, a.b.id=4
     * {
     * name:1,
     * id:2,
     * a.b.name:3,
     * a.b.id:4
     * }
     */
    public List<Map<String, Object>> toSimpleMap() {
        return this.mapList;
    }

    /**
     * 将结果存为一个Map对象，支持Map嵌套。
     * 例如执行结果是 name=1, id=2,a.b.name=3, a.b.id=4
     * {
     * name:1,
     * id:2,
     * a:{
     * b:{
     * name:3,
     * id:4
     * }
     * }
     * }
     *
     * @return
     */
    public List<Map<String, Object>> toMapTree() {
        if ((null == this.mapTree && null != this.mapList) || (this.mapTree.isEmpty() && !this.mapList.isEmpty())) {
            if (null == this.mapList) {
                return null;
            }

            List<Map<String, Object>> newList = new ArrayList<>();
            for (Map<String, Object> m : this.mapList) {
                newList.add(CommonDalUtils.processMapToMapTree(m));
            }

            this.mapTree = newList;
        }

        return this.mapTree;
    }

    public String toJSON() {
        List<Map<String, Object>> r = toSimpleMap();

        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(Date.class, new JsonDateValueProcessor());

        String json = JSONArray.fromObject(r, jsonConfig).toString();

        return json;
    }

    public List<Object> getColumn(String columnName) {
        if (null == this.mapList || this.mapList.isEmpty()) {
            return null;
        }

        if (!mapList.get(0).containsKey(columnName)) {
            return null;
        }

        List<Object> list = new ArrayList<>();
        for (Map<String, Object> m : this.mapList) {
            list.add(m.get(columnName));
        }

        return list;
    }

    public List<T> toList() {
        if ((null == this.objList && null != this.mapList) ||
                (this.objList.isEmpty() && !this.mapList.isEmpty())) {
            try {
                this.objList = CommonDalUtils.setMeta(this.clazz, this.toMapTree());
            } catch (Exception e) {
                Logger.logError(e);
            }
        }

        return this.objList;
    }

    public List<T> filter(FilterConsumer<T> consumer) {
        if (null == this.mapList || this.mapList.isEmpty()) {
            return this.toList();
        }

        List<T> list = this.toList();
        List<T> newList = new ArrayList<>();
        for (T t : list) {
            if (consumer.accept(t)) {
                newList.add(t);
            }
        }

        return newList;
    }

    public boolean isEmpty() {
        return null == this.mapList || this.mapList.isEmpty();
    }

    public int size() {
        return null == this.mapList ? 0 : this.mapList.size();
    }

    public T firstOrDefault() {

        if (null == this.mapList || this.mapList.isEmpty()) {
            return null;
        }

        Map<String, Object> map = this.toMapTree().get(0);
        try {
            return CommonDalUtils.setMeta(this.clazz, map);
        } catch (Exception e) {
            Logger.inst.log(Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public String toString() {
        return this.toJSON();
    }
}
