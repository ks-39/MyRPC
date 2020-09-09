# RPC version 3——多种序列化方式和自定义协议
## 1. 知识背景
1. 安装zookeeper，使用zookeeper



## 2. 解决问题
1. 服务器只需要连接到注册中心就可以实现与服务器连接



## 3. 如何解决
1. 创建zookeeper注册中心
2. 将服务器注册到zookeeper
3. 客户端直接连接到zookeeper



## 4. 项目完整目录
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
### 1. pojo：同V2


### 2. service服务：同V2


### 3. coder编码解码：同V2


### 4. Client客户端
1. TestClient
```
public class TestClient {
    public static void main(String[] args) {

        //客户端不再需要传入ip和端口
        RPCClient rpcClient = new NettyRPCClient();

        .....
    }
}
```

2. NettyClient：连接到zookeeper注册中心
```
public class NettyRPCClient implements RPCClient {

    ....

    private String host;
    private int port;

    //zk注册中心
    private ServiceRegister serviceRegister;
    public NettyRPCClient() {
        this.serviceRegister = new ZkServiceRegister();
    }

    ....


    @Override
    public RPCResponse sendRequest(RPCRequest request) {

        //获取zookeeper的ip和端口
        InetSocketAddress address = serviceRegister.serviceDiscovery(request.getInterfaceName());
        host = address.getHostName();
        port = address.getPort();

        try {
            //连接到zookeeper注册中心
            ChannelFuture channelFuture  = bootstrap.connect(host, port).sync();

            ....

            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
```


### 5. Server服务器
1. TestServer
```
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
```

2. ServerProvider：将Server注册到zookeeper
```
public class ServiceProvider {

    //使用Map存放多个服务
    private Map<String, Object> interfaceProvider;


    //将服务器注册到zookeeper
    private ServiceRegister serviceRegister;
    private String host;
    private int port;
    public ServiceProvider(String host, int port){
        this.host = host;
        this.port = port;
        this.interfaceProvider = new HashMap<>();
        this.serviceRegister = new ZkServiceRegister();
    }

    .....

}
```


### 6. Register注册
1. Register接口
```
public interface ServiceRegister {
    
    //注册并保存地址
    void register(String serviceName, InetSocketAddress serverAddress);
    
    //根据服务名查询地址
    InetSocketAddress serviceDiscovery(String serviceName);
}
```

2. ZKRegister实现类
```
public class ZkServiceRegister implements ServiceRegister {

    //注册zookeeper
    private CuratorFramework client;

    //zookeeper根路径节点
    private static final String ROOT_PATH = "MyRPC";

    //zookeeper客户端的初始化，并与zookeeper服务端建立连接
    public ZkServiceRegister(){
        // 指数时间重试
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);

        // 地址和端口：zookeeper的地址固定，不管是服务提供者还是，消费者都要与之建立连接
        // 心跳时间：sessionTimeoutMs 与 zoo.cfg中的tickTime 有关系，
        // 超时：zk还会根据minSessionTimeout与maxSessionTimeout两个参数重新调整最后的超时值。默认分别为tickTime 的2倍和20倍
        // 使用心跳监听状态
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();

        //启动zookeeper
        this.client.start();
        System.out.println("zookeeper 连接成功");
    }

    //注册curator到zookeeper
    @Override
    public void register(String serviceName, InetSocketAddress serverAddress){
        try {
            
            //永久节点，服务提供者下线时，不删服务名，只删地址
            if(client.checkExists().forPath("/" + serviceName) == null){
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/" + serviceName);
            }

            //路径地址，一个/代表一个节点
            String path = "/" + serviceName +"/"+ getServiceAddress(serverAddress);

            // 临时节点，服务器下线就删除节点
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

        } catch (Exception e) {
            System.out.println("此服务已存在");
        }
    }

    //根据服务名查找对应服务器
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        try {
            List<String> strings = client.getChildren().forPath("/" + serviceName);
            // 这里默认用的第一个，后面加负载均衡
            String string = strings.get(0);
            return parseAddress(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    
    //解析地址
    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() +
                ":" +
                serverAddress.getPort();
    }
    //字符串解析为地址
    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
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
    1. TestServer注册已有的服务，将服务存入ServiceProvider的Map中，将服务器注册到zookeeper注册中心
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
    2. zkRegister注册中心将服务器注册到zookeeper注册中心，根据客户端request的服务名查找注册中心的服务器，通过反射调用方法，返回response


## 7. 存在问题
1. 根据服务名查询地址时，我们返回的总是第一个IP，导致这个提供者压力巨大，而其它提供者调用不到
