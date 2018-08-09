package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.ForeignKey;
import com.sinux.happy.orm.common.Logger;
import com.sinux.happy.orm.common.Reflections;
import com.sinux.happy.orm.common.Utils;
import com.sinux.happy.orm.exceptions.WrongDataTypeException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CommonDalUtils {


    /**
     * 将Map转化为Class Instance
     *
     * @param cls 目标Class
     * @param map 要转化的map（可以是嵌套的）
     * @param <E>
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected static <E> E setMeta(Class<E> cls, Map<String, Object> map)
            throws InstantiationException, IllegalAccessException {
        // TODO Auto-generated method stub
        E e = (E) cls.newInstance();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (null == value) {
                continue;
            }
            try {
                Class<?> clsFieldType = getMetaType(cls, key);
                if(null == clsFieldType){
                    return null;
                }

                if (clsFieldType.isEnum()) {
                    if (value instanceof CharSequence) {
                        Enum enumVal = Enum.valueOf((Class) clsFieldType, value.toString());
                        Reflections.invokeSetter(e, key, enumVal);
                    } else {
                        throw new WrongDataTypeException("枚举类型的值必须是String");
                    }
                } else if (Utils.isBasicType(value)) {
                    try {
                        Reflections.invokeSetter(e, key, value);
                    } catch (IllegalArgumentException ex) {
                        Object o = clsFieldType.newInstance();
                        Method[] innerMethods = clsFieldType.getMethods();
                        for (Method innerMethod : innerMethods) {
                            ForeignKey fk = innerMethod.getAnnotation(ForeignKey.class);
                            if (null != fk) {
                                Column innerColumn = innerMethod.getAnnotation(Column.class);
                                if (null != innerColumn) {
                                    Reflections.invokeSetter(o, Utils.getFieldName(innerMethod), value);
                                    Reflections.invokeSetter(e, key, o);
                                    break;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Logger.logError(ex);
                    }
                } else {
                    // 若实体的属性类型是自定义对象
                    if (value instanceof Map) {
                        if (null != clsFieldType) {
                            Object innerValue = setMeta(clsFieldType, (Map) value);
                            Reflections.invokeSetter(e, key, innerValue);
                        }
                    }
                }
            } catch (java.lang.IllegalArgumentException ex) {
                Logger.logError("invokeSetter value.class=" + value.getClass().getName() + " Key=" + key, ex);
            }
        }
        return e;
    }

    /**
     * 将一组Map转化为一组Class Instance
     *
     * @param cls
     * @param mapList
     * @param <E>
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <E> List<E> setMeta(Class<E> cls, List<Map<String, Object>> mapList)
            throws InstantiationException, IllegalAccessException {
        List<E> metaList = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            metaList.add(setMeta(cls, map));
        }
        return metaList;
    }

    /**
     * 返回指定Class中指定Field的return type
     *
     * @param cls 指定的Class
     * @param key 指定的Field Name
     * @param <E>
     * @return
     */
    private static <E> Class<?> getMetaType(Class<E> cls, String key) {
        Class<?> clsFieldType = null;
        try {
            Field clsField = cls.getDeclaredField(key);
            if(null == clsField){
                Logger.logError("在" + cls.getName()+"中找不到field:"+key);
                return null;
            }
            clsField.setAccessible(true);
            clsFieldType = clsField.getType();
        } catch (NoSuchFieldException e1) {
            String getterName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
            try {
                Method m = cls.getMethod(getterName);
                clsFieldType = m.getReturnType();
            } catch (NoSuchMethodException e2) {
                Logger.inst.info("因访问权限问题无法找到Field:" + cls.getName() + "." + key + "或"
                        + cls.getName() + "." + getterName + ": " + e1.getMessage());

            }
        }
        return clsFieldType;
    }

    /**
     * 将普通单层Map变成新的map树
     *
     * @return 新的Map树
     */
    protected static Map<String, Object> processMapToMapTree(Map<String, Object> map) {
        Map<String, Object> newMap = new HashMap<>();
        Map<String, Map<String, Object>> tempMap = new HashMap<>();
        for (String key : map.keySet()) {
            if (key.contains(".")) {
                int dotIndex = key.indexOf(".");
                String methodName = key.substring(0, dotIndex);
                String newKey = key.substring(dotIndex + 1);
                if (!tempMap.containsKey(methodName)) {
                    tempMap.put(methodName, new HashMap<>());
                }

                tempMap.get(methodName).put(newKey, map.get(key));
            } else {
                newMap.put(key, map.get(key));
            }
        }

        for (String key : tempMap.keySet()) {
            newMap.put(key, processMapToMapTree(tempMap.get(key)));
        }

        return newMap;
    }
}