# RPC version 4——负载均衡
## 1. 知识背景
1. 负载均衡



## 2. 解决问题
1. 服务器进行负载均衡分担服务器压力



## 3. 如何解决
1. 配置LoadBalance负载均衡接口
2. 随机负载均衡RandomLoadBalance
3. 轮询负载均衡RoundLoadBalance



## 4. 整体项目目录
1. pojo
    1. Blog
    2. User
    3. Request
    4. Response

2. Service
    1. BlogService、BlogServiceImpl
    2. UserService、UserServiceimpl

3. Client
    1. RPCClient接口
    2. PRCClientProxy
    3. TestClient
    4. NettyClient
    5. NettyClientHandler
    6. NettyClientInit

4. Server
    1. RPCServer接口
    2. ServerProvider
    3. TestServer
    4. NettyServer
    5. NettyServerHandler
    6. NettyServerInit

5. Coder
    1. Serizlizer接口
    2. MessageType
    3. ObjectSeriazlizer
    4. JsonSerializer
    5. MyDecoder
    6. MyEncoder

6. Register
    1. Register接口
    2. zkRegister实现类



## 5. 各模块代码
### 1. pojo：同V3


### 2. service服务：同V3


### 3. coder编码解码：同V3


### 4. client客户端：同V3


### 5. server服务器
1. TestServer1：8899端口
```
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
```

2. TestServer2：8900端口
```
public class TestServer2 {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 8900);
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);
        RPCServer RPCServer = new NettyRPCServer(serviceProvider);

        RPCServer.start(8900);
    }
}
```


### 6. register注册中心
1. zkRegister：设置负载均衡策略
```
public class ZkServiceRegister implements ServiceRegister{

    ....

    // 初始化负载均衡器， 这里用的是随机， 一般通过构造函数传入
    private LoadBalance loadBalance = new RandomLoadBalance();

    ....


    // 根据服务名返回地址
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        try {
            List<String> strings = client.getChildren().forPath("/" + serviceName);

            // 负载均衡选择器，选择一个
            String string = loadBalance.balance(strings);

            return parseAddress(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    ....


}

```


### 7. loadBalance负载均衡
1. loadBalance接口
```
public interface LoadBalance {
    String balance(List<String> addressList);
}
```

2. RandomLoadBalance随机负载均衡
```
public class RandomLoadBalance implements  LoadBalance{
    @Override
    public String balance(List<String> addressList) {
        Random random = new Random();
        int choose = random.nextInt(addressList.size());
        System.out.println("负载均衡选择了" + choose + "服务器");
        return addressList.get(choose);
    }
}
```

3. RoundLoadBalance轮询负载均衡
```
public class RoundLoadBalance implements LoadBalance{
    private int choose = -1;
    @Override
    public String balance(List<String> addressList) {
        choose++;
        choose = choose%addressList.size();
        return addressList.get(choose);
    }
}
```



## 6. 整体流程
1. 客户端
    1. TestClient利用NettyClient连接到zookeeper注册中心
    2. ClientProxy代理类将服务名和参数封装成request，传递给NettyClient进行发送
    3. NettyClient连接到zookeeper注册中心，根据服务名查找服务器
    4. NettyHandler再从channel中读取返回的response
    5. NettyClientInti使用自定义的编码和解码器

2. 服务器
    1. TestServer1和TestServer2注册已有的服务，将服务存入ServiceProvider的Map中，将服务器注册到zookeeper注册中心
    2. NettyServer监听channel，有事件就拿走，调用NettyHandler进行处理
    3. NettyHandler通过反射获取request的服务器名和参数，invoke执行服务方法，拿到response，返回给Client
    4. NettyServerInit使用自定义的编码和解码器

3. 编码解码
    1. Serializer序列化接口
    2. MessageType
    3. ObjectSerializer
    4. JsonSerializer
    5. MyDecoder
    6. MyEncoder

4. 注册中心
    1. Register接口
    2. zkRegister注册中心将服务器注册到zookeeper注册中心，根据客户端request的服务名查找注册中心的服务器，使用负责均衡策略，通过反射调用方法，返回response

5. 负载均衡
    1. loadBalance接口
    2. RandomBalance随机负载均衡
    3. RoundBalance轮询负载均衡