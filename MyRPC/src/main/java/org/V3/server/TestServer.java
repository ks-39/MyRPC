package org.V3.server;

import org.V3.service.BlogService;
import org.V3.service.BlogServiceImpl;
import org.V3.service.UserService;
import org.V3.service.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) {
        //注册服务
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        //将服务器注册到zookeeper
        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 8899);
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        //使用Netty处理
        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}