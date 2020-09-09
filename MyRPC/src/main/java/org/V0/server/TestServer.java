package org.V0.server;

import org.V0.service.BlogService;
import org.V0.service.BlogServiceImpl;
import org.V0.service.UserService;
import org.V0.service.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) {
        //注册服务
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        //将服务存入Map中
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        //使用线程池处理服务
        RPCServer RPCServer = new ThreadPoolRPCRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}