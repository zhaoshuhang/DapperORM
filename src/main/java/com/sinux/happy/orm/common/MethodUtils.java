package com.sinux.happy.orm.common;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MethodUtils {
    public static <T, A1> Method getReferencedMethod(Class<T> clazz, MethodRefWith1Arg<T> methodRef) {
        return findReferencedMethod(clazz, t -> methodRef.call(t));
    }

    @SuppressWarnings("unchecked")
    private static <T> Method findReferencedMethod(Class<T> clazz, Consumer<T> invoker) {
        AtomicReference<Method> ref = new AtomicReference<>();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            ref.set(method);
            return null;
        });
        try {
            invoker.accept((T) enhancer.create());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format("Invalid method reference on class [%s]", clazz));
        }

        Method method = ref.get();
        if (method == null) {
            throw new IllegalArgumentException(String.format("Invalid method reference on class [%s]", clazz));
        }

        return method;
    }
}
