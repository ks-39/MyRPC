package org.V2.server;

import org.V2.service.BlogService;
import org.V2.service.BlogServiceImpl;
import org.V2.service.UserService;
import org.V2.service.UserServiceImpl;

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