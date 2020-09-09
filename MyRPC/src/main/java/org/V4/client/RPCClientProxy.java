package org.V4.client;


import lombok.AllArgsConstructor;
import org.V4.pojo.RPCRequest;
import org.V4.pojo.RPCResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@AllArgsConstructor
public class RPCClientProxy implements InvocationHandler {

    private RPCClient client;

    //代理对象调用方法后执行invoke
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //封装request
        RPCRequest request = RPCRequest.builder().interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsTypes(method.getParameterTypes()).build();

        //将request发送给Netty，发送到channel中，接收服务器返回的response
        RPCResponse response = client.sendRequest(request);
        //返回response
        return response.getData();
    }

    //获取代理对象
    <T>T getProxy(Class<T> clazz){
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
