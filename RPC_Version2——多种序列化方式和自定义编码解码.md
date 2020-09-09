# RPC version 2——多种序列化方式和自定义协议
## 1. 知识背景
1. 各种序列化方式以及比较
2. 自定义解码器和编码器



## 2. 解决问题
1. 使用多种序列化方式
2. 使用自定义的解码器和编码器



## 3. 如何解决
1. 定义Serializer接口进行拓展



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

5. Coder
    1. Serizlizer
    2. MessageType
    3. ObjectSeriazlizer
    4. JsonSerializer
    5. MyDecoder
    6. MyEncoder



## 5. 各模块代码
### 1. pojo：
1. response
```
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RPCResponse implements Serializable {

    //Response解耦

    private int code;
    private String message;
    private Object data;

    // 更新,这里我们需要加入这个，不然用其它序列化方式（除了java Serialize）得不到data的type
    private Class<?> dataType;

    public static RPCResponse success(Object data) {
        return RPCResponse.builder().code(200).data(data).build();
    }
    public static RPCResponse fail() {
        return RPCResponse.builder().code(500).message("服务器发生错误").build();
    }
}
```


### 2. service服务：同V1


### 3. Client客户端
1. NettyClientInit：使用自定义的编码和解码
```
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 使用自定义的编解码器
        pipeline.addLast(new MyDecode());
        // 编码需要传入序列化器，这里是json，还支持ObjectSerializer，也可以自己实现其他的
        pipeline.addLast(new MyEncode(new JsonSerializer()));
        pipeline.addLast(new NettyClientHandler());
    }
}
```


### 4. Server服务器：同V1
1. NettyServerInit：使用自定义的编码和解码
```
@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 使用自定义的编解码器
        pipeline.addLast(new MyDecode());
        // 编码需要传入序列化器，这里是json，还支持ObjectSerializer，也可以自己实现其他的
        pipeline.addLast(new MyEncode(new JsonSerializer()));
        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }
}
```



## 6. 整体流程
1. 客户端
    1. TestClient利用NettyClient连接到目标ip和端口
    2. ClientProxy代理类将服务名和参数封装成request，传递给NettyClient进行发送
    3. NettyClient将request发送到channel
    4. NettyHandler再从channel中读取返回的response
    5. NettyClientInti使用自定义的编码和解码器

2. 服务器
    1. TestServer注册已有的服务，将服务存入ServiceProvider的Map中，使用NettyServer处理服务
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



## 7. 存在问题
1. 服务端与客户端通信的host与port预先就必须知道的，每一个客户端都必须知道对应服务的ip与端口号，扩展性不强
