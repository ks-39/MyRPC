package org.V1.server;

import org.V1.service.BlogService;
import org.V1.service.BlogServiceImpl;
import org.V1.service.UserService;
import org.V1.service.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) {
        //注册服务
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        //将服务存入Map中
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        //使用Netty处理
        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}