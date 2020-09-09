package org.V4.server;


import org.V4.service.BlogService;
import org.V4.service.BlogServiceImpl;
import org.V4.service.UserService;
import org.V4.service.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) {

        //注入服务
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        //暴露服务，顺便在注册中心注册，实际上应分开，每个类做各自独立的事
        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 8899);

        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        //交给Netty执行
        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}