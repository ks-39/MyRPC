package org.V4.client;


import org.V4.pojo.Blog;
import org.V4.pojo.User;
import org.V4.service.BlogService;
import org.V4.service.UserService;

public class TestClient {
    public static void main(String[] args) {

        //创建客户端Netty服务，客户端不再需要传固定的ip和端口
        RPCClient rpcClient = new NettyRPCClient();

        //交给代理对象
        RPCClientProxy rpcClientProxy = new RPCClientProxy(rpcClient);

        //代理对象获取服务
        UserService userService = rpcClientProxy.getProxy(UserService.class);

        // 调用方法
        User userByUserId = userService.getUserByUserId(10);
        System.out.println("从服务端得到的user为：" + userByUserId);

        User user = User.builder().userName("张三").id(100).sex(true).build();
        Integer integer = userService.insertUserId(user);
        System.out.println("向服务端插入数据："+integer);

        BlogService blogService = rpcClientProxy.getProxy(BlogService.class);

        Blog blogById = blogService.getBlogById(10000);
        System.out.println("从服务端得到的blog为：" + blogById);

        // 测试json调用空参数方法
        System.out.println(userService.hello());
    }
}
