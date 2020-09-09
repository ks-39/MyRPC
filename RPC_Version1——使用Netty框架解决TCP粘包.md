# RPC version 3——使用Netty
## 1. 知识背景
1. netty框架入门



## 2. 解决问题
1. 从BIO到NIO
2. 解决TCP粘包问题
3. 多客户端服务拓展



## 3. 如何解决
1. BIO到NIO——使用Netty框架
2. 解决TCP粘包——自定义字节流长度
3. 多客户拓展——定义客户端接口



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
    1. RPCClient
    2. PRCClientProxy
    3. TestClient
    4. NettyClient
    5. NettyClientHandler
    6. NettyClientInit

4. Server
    1. RPCServer
    2. ServerProvider
    3. TestServer
    4. NettyServer
    5. NettyServerHandler
    6. NettyServerInit



## 5. 各模块代码
### 1. pojo：同V0


### 2. service服务：同V0


### 3. Client客户端:
1. RPCClient接口
```
public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
```

2. RPCClientProxy动态代理类
    1. 封装request
    2. 将request发送给Netty
    3. 接收response
```
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
```

3. NettyRPCClient：客户端Netty服务，可深挖
    1. 定义初始化参数
    2. 静态代码块初始化Netty
    3. 连接到目标端口，将request写入channel
    4. 读取指定名称的response
```
public class NettyRPCClient implements RPCClient {

    //启动参数
    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;
    private String host;
    private int port;

    //构造函数
    public NettyRPCClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    //静态代码块初始化Netty，每次加载客户端服务时都执行
    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }

    /**
     * 这里需要操作一下，因为netty的传输都是异步的，你发送request，会立刻返回一个值， 而不是想要的相应的response
     */
    @Override
    public RPCResponse sendRequest(RPCRequest request) {
        try {
            //启动Netty，连接到目标端口
            ChannelFuture channelFuture  = bootstrap.connect(host, port).sync();
            //创建channel
            Channel channel = channelFuture.channel();

            //发送request到channel中
            channel.writeAndFlush(request);
            channel.closeFuture().sync();

            //阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
            //AttributeKey是，线程隔离的，不会产生线程安全问题。
            //实际上不应通过阻塞，可通过回调函数
            AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
            RPCResponse response = channel.attr(key).get();
            return response;
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

4. NettyClientHandler：客户端Netty处理器
    1. 读取channel中的response，返回给NettyRPCClient，关闭channel
```
public class NettyClientHandler extends SimpleChannelInboundHandler<RPCResponse> {
    
    //读取channel中的指定名称response，给NettyRPCClient读取，关闭channel
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCResponse msg) throws Exception {
        AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
        ctx.channel().attr(key).set(msg);
        ctx.channel().close();
    }
    
    //异常捕获
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

5. NettyClientInit：客户端Netty初始化，编码和解码格式
```
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //管道
        ChannelPipeline pipeline = ch.pipeline();

        //消息格式 [长度][消息体]
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));

        //写入到前4个字节中
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new ObjectEncoder());

        //解析类型
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));

        pipeline.addLast(new NettyClientHandler());
    }
}
```

6. TestClient
```
public class TestClient {
    public static void main(String[] args) {

        //创建客户端Netty服务
        RPCClient rpcClient = new NettyRPCClient("127.0.0.1", 8899);

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
    }
}
```


### 4. Server服务器
1. RPCServer：服务器接口
```
public interface RPCServer {
    void start(int port);
    void stop();
}
```

2. ServicePrivoder：服务暴露类
```
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个接口
     */
    private Map<String, Object> interfaceProvider;

    public ServiceProvider(){
        this.interfaceProvider = new HashMap<>();
    }

    public void provideServiceInterface(Object service){
        //获取Service目录下的service
        Class<?>[] interfaces = service.getClass().getInterfaces();
        //遍历存入map中
        for(Class clazz : interfaces){
            interfaceProvider.put(clazz.getName(),service);
        }
    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }
}
```

3. NettyRPCServer：服务器Netty服务，可深挖
```
/**
 * 实现RPCServer接口，负责监听与发送数据
 */
@AllArgsConstructor
public class NettyRPCServer implements RPCServer {

    //注入
    private ServiceProvider serviceProvider;

    //实现方法
    @Override
    public void start(int port) {

        // netty服务线程组boss负责建立连接， work负责具体的请求
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        System.out.println("Netty服务端启动了...");

        try {
            // 启动netty服务器
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 初始化Netty
            serverBootstrap.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer(serviceProvider));
            
            // 同步阻塞
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            // 死循环监听
            channelFuture.channel().closeFuture().sync();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
    }
}
```

4. NettyRPCServerHandler：服务器Netty处理器
```
@AllArgsConstructor
public class NettyRPCServerHandler extends SimpleChannelInboundHandler<RPCRequest> {
    //注入
    private ServiceProvider serviceProvider;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCRequest msg) throws Exception {
        //提取response，写入到channel
        RPCResponse response = getResponse(msg);
        ctx.writeAndFlush(response);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    RPCResponse getResponse(RPCRequest request) {
        
        // 得到服务名
        String interfaceName = request.getInterfaceName();
        
        // 得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);
        
        // 反射调用方法
        Method method = null;
        try {
            //调用对应方法，传入参数
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            
            //获取方法返回
            return RPCResponse.success(invoke);
            
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RPCResponse.fail();
        }
    }
}

```

5. NettyServerInit：服务器Netty初始化，编码和解码
```
@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ChannelPipeline pipeline = ch.pipeline();

        // 消息格式 [长度][消息体], 解决粘包问题
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));

        // 计算当前待发送消息的长度，写入到前4个字节中
        pipeline.addLast(new LengthFieldPrepender(4));

        // 这里使用的还是java 序列化方式， netty的自带的解码编码支持传输这种结构
        pipeline.addLast(new ObjectEncoder());
        
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));

        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }
}
```

6. TestServer
```
public class TestServer {
    public static void main(String[] args) {
        
        //注入服务
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();
        
        //暴露服务
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);
        
        //交给Netty执行
        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}
```


## 6. 整体流程
1. 客户端
    1. TestClient利用NettyClient连接到目标ip和端口
    2. ClientProxy代理类将服务名和参数封装成request，传递给NettyClient进行发送
    3. NettyClient将request发送到channel
    4. NettyHandler再从channel中读取返回的response
    5. NettyClientInti规定解码和编码格式

2. 服务器
    1. TestServer注册已有的服务，将服务存入ServiceProvider的Map中，使用NettyServer处理服务
    2. NettyServer监听channel，有事件就拿走，调用NettyHandler进行处理
    3. NettyHandler通过反射获取request的服务器名和参数，invoke执行服务方法，拿到response，返回给Client
    4. NettyServerInit规定解码和编码格式



## 7. 存在问题
1. 只能使用Java自带的Serialiable序列化方式