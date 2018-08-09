package com.sinux.happy.orm.common;

import net.sf.json.JSONObject;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 一个将对象变成Map的包装类
 * 支持嵌套对象
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class ObjMap {

    // 1=Map, 2=Class instance, 3=JSON String
    int objType;
    Object object;

    public ObjMap(Object obj) {
        if (null == obj) {
            throw new NullArgumentException("obj");
        }

        this.object = obj;

        if (obj instanceof ObjMap) {
            // 保证ObjMap的幂等性，
            // 即new ObjMap(new ObjMap(obj)) 等于 new ObjMap(obj)
            ObjMap objMap = (ObjMap) obj;
            this.objType = objMap.objType;
            this.object = objMap.object;
        } else if (obj instanceof Map) {
            objType = 1;
        } else if (obj instanceof CharSequence) {
            this.object = JSONObject.fromObject(obj);
            objType = 3; // JSON
        } else {
            objType = 2;
        }
    }

    /**
     * 将一个包含分隔符的字符串分割后作为map
     * 例如ObjMap("a is 1 and b is 2 and c is3"," and "," is ")将得到 {a=1,b=2,c=3}
     *
     * @param str            要处理的字符串
     * @param separatorRegex 分割符的正则表达式
     * @param symbolOfEquals key和value的分隔符
     */
    public ObjMap(String str, String separatorRegex, String symbolOfEquals) {
        String[] parts = str.split(separatorRegex);
        HashMap<String, String> map = new HashMap<>();
        for (String part : parts) {
            int indexOfEqual = part.indexOf(symbolOfEquals);
            if (indexOfEqual < 0) {
                map.put(part, null);
            } else if (indexOfEqual == 0) {
                continue;
            } else {
                map.put(part.substring(0, indexOfEqual), part.substring(indexOfEqual + symbolOfEquals.length()));
            }
        }

        this.object = map;
        this.objType = 1;
    }

    public Object get(String key) {
        switch (this.objType) {
            case 1:
                return getValueFromMap(key);
            case 2:
                return getValueFromClassInstance(key, this.object);
            case 3:
                return getValueFromJsonObj(key);
            default:
                return null;
        }
    }

    private Object getValueFromJsonObj(String key) {
        return ((JSONObject) this.object).get(key);
    }

    private Object getValueFromClassInstance(
            String variable, Object parameter) {
        String subVariableName = null;
        int dotIndex = variable.indexOf('.');
        if (dotIndex > 0) {
            subVariableName = variable.substring(dotIndex + 1);
            variable = variable.substring(0, dotIndex);
        }

        try {
            Field field = parameter.getClass().getDeclaredField(variable);
            PropertyDescriptor pd = new PropertyDescriptor(field.getName(), parameter.getClass());
            Method readMethod = pd.getReadMethod();//获得读方法

            Object o = readMethod.invoke(parameter);
            if (StringUtils.isNotBlank(subVariableName)) {
                return getValueFromClassInstance(subVariableName, o);
            }

            return o;
        } catch (NoSuchFieldException e) {
            Logger.logError("解析对象时找不到名为" + variable + "的成员");
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            Logger.logError("解析对象时出错", e);
        }

        return null;
    }

    private Object getValueFromMap(String key) {
        Map map = (Map) this.object;
        if (map.containsKey(key)) {
            return map.get(key);
        }

        Logger.logError("找不到key=" + key);
        return null;
    }

}