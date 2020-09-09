package org.V0.client;


import lombok.AllArgsConstructor;
import org.V0.pojo.RPCRequest;
import org.V0.pojo.RPCResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@AllArgsConstructor
public class RPCClientProxy implements InvocationHandler {

    // 传入参数Service接口的class对象，反射封装成一个request
    private String host;
    private int port;

    // jdk 动态代理， 每一次代理对象调用方法，会经过此方法增强（反射获取request对象，socket发送至客户端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RPCRequest request = RPCRequest.builder().interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsTypes(method.getParameterTypes()).build();

        //将request传递给IOClient进行数据交互
        RPCResponse response = IOClient.sendRequest(host, port, request);

        //返回结果给client
        return response.getData();
    }

    //获取代理对象
    <T>T getProxy(Class<T> clazz){
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
