package org.V0.client;


import org.V0.pojo.Blog;
import org.V0.pojo.User;
import org.V0.service.BlogService;
import org.V0.service.UserService;

public class RPCClient {
    public static void main(String[] args) {

        //客户端访问多服务

        RPCClientProxy rpcClientProxy = new RPCClientProxy("127.0.0.1", 8899);
        UserService userService = rpcClientProxy.getProxy(UserService.class);

        User userByUserId = userService.getUserByUserId(10);
        System.out.println("从服务端得到的user为：" + userByUserId);

        User user = User.builder().userName("张三").id(100).sex(true).build();
        Integer integer = userService.insertUserId(user);
        System.out.println("向服务端插入数据："+integer);

        BlogService blogService = rpcClientProxy.getProxy(BlogService.class);
        Blog blogById = blogService.getBlogById(10000);
        System.out.println("从服务端得到的blog为：" + blogById);
    }
}
