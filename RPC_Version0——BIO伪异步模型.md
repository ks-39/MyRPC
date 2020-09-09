# RPC version 0——服务器使用线程池处理
## 1. 知识背景
1. 服务器代码解耦
2. 线程池处理连接



## 2. 解决问题
1. 服务器处理多个接口
2. 服务器使用线程池进行连接(即BIO的伪异步模型)



## 3. 如何解决
1. 服务器代码解耦
    1. 服务器接口RPCService
    2. 线程池实现类ThreadPoolRPCService
    3. 工作任务类处理数据交互WorkThread
    4. 服务器暴露ServiceProvider



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
    1. TestClient
    2. ClientProxy
    3. IOClient

4. Server
    1. RPCServer
    2. ServerProvider
    3. TestServer
    4. ThreadPoolServer
    5. WorkThread


## 5. 各模块代码
### 1. pojo实体类
1. Blog
```
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Blog implements Serializable {
    private Integer id;
    private Integer useId;
    private String title;
}
```

2. User
```
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private Integer id;
    private String userName;
    private Boolean sex;
}
```

3. Request
```
@Data
@Builder
public class RPCRequest implements Serializable {
    //将请求参数抽象出来

    // 服务类名，客户端只知道接口名，在服务端中用接口名指向实现类
    private String interfaceName;
    // 方法名
    private String methodName;
    // 参数列表
    private Object[] params;
    // 参数类型
    private Class<?>[] paramsTypes;

}
```

4. Response
```
@Data
@Builder
public class RPCResponse implements Serializable {
    //将响应参数抽象出来
    
    private int code;
    private String message;
    private Object data;

    public static RPCResponse success(Object data) {
        return RPCResponse.builder().code(200).data(data).build();
    }
    public static RPCResponse fail() {
        return RPCResponse.builder().code(500).message("服务器发生错误").build();
    }
}
```

### 2. service服务
1. UserService
```
public interface UserService {
    User getUserByUserId(Integer id);
    Integer insertUserId(User user);
}

public class UserServiceImpl implements UserService {
    @Override
    public User getUserByUserId(Integer id) {
        User user = User.builder().id(id).userName("he2121").sex(true).build();
        System.out.println("客户端查询了"+id+"用户");
        return user;
    }

    @Override
    public Integer insertUserId(User user) {
        System.out.println("插入数据成功："+user);
        return 1;
    }
}
```

2. BlogService
```
public interface BlogService {
    Blog getBlogById(Integer id);
}

public class BlogServiceImpl implements BlogService {
    @Override
    public Blog getBlogById(Integer id) {
        Blog blog = Blog.builder().id(id).title("我的博客").useId(22).build();
        System.out.println("客户端查询了"+id+"博客");
        return blog;
    }
}
```

### 3. client
1. TestClient
```
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
```

2. ClientProxy代理类
```
@AllArgsConstructor
public class RPCClientProxy implements InvocationHandler {
    
    // 传入参数Service接口的class对象，反射封装成一个request
    private String host;
    private int port;
    
    // jdk 动态代理， 每一次代理对象调用方法，会经过此方法增强（反射获取request对象，socket发送至客户端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        
        RPCRequest request = RPCRequest.builder().interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsTypes(method.getParameterTypes()).build();
        
        //将request传递给IOClient进行数据交互
        RPCResponse response = IOClient.sendRequest(host, port, request);
        
        //返回结果给client
        return response.getData();
    }
    
    //获取代理对象
    <T>T getProxy(Class<T> clazz){
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
```

3. IOClient数据交互类
```
public class IOClient {
    
    // 客户端发起一次请求调用，Socket建立连接，发起请求Request，得到响应Response
    public static RPCResponse sendRequest(String host, int port, RPCRequest request){
        try {
            
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            objectOutputStream.writeObject(request);
            objectOutputStream.flush();

            RPCResponse response = (RPCResponse) objectInputStream.readObject();
            return response;
            
        } catch (IOException | ClassNotFoundException e) {
            System.out.println();
            return null;
        }
    }
}
```

### 4. server
1. RPCClient
```
public interface RPCServer {
    void start(int port);
}
```

2. ServiceProvider服务暴露类
```
public class ServiceProvider {

    //使用Map存放多个服务
    private Map<String, Object> interfaceProvider;

    public ServiceProvider(){
        this.interfaceProvider = new HashMap<>();
    }

    //将service服务存入Map
    public void provideServiceInterface(Object service){
        Class<?>[] interfaces = service.getClass().getInterfaces();
        for(Class clazz : interfaces){
            interfaceProvider.put(clazz.getName(),service);
        }

    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }
}
```

3. TestServer
```
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
```

4. ThreadPoolService线程池处理服务
```
public class ThreadPoolRPCRPCServer implements RPCServer {
    private final ThreadPoolExecutor threadPool;
    private ServiceProvider serviceProvider;

    //默认初始化
    public ThreadPoolRPCRPCServer(ServiceProvider serviceProvider){
        threadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                1000, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        this.serviceProvider = serviceProvider;
    }

    //自定义初始化
    public ThreadPoolRPCRPCServer(ServiceProvider serviceProvider, int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime,
                                  TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue){

        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.serviceProvider = serviceProvider;
    }

    //start
    @Override
    public void start(int port) {
        System.out.println("线程池版服务端启动了");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            //BIO方式
            while(true){
                Socket socket = serverSocket.accept();

                //新建一个WorkThread工作线程处理服务
                threadPool.execute(new WorkThread(socket,serviceProvider));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

5. WorkThread工作线程
```
@AllArgsConstructor
public class WorkThread implements Runnable{

    private Socket socket;
    private ServiceProvider serviceProvider;

    @Override
    public void run() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // 读取客户端传过来的request
            RPCRequest request = (RPCRequest) ois.readObject();

            // 反射调用服务方法获得返回值
            RPCResponse response = getResponse(request);

            //写回到客户端
            oos.writeObject(response);
            oos.flush();

        }catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("从IO中读取数据错误");
        }
    }

    private RPCResponse getResponse(RPCRequest request){

        // 得到服务名
        String interfaceName = request.getInterfaceName();
        // 得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);
        // 反射调用方法
        Method method = null;

        try {
            //反射获取class对象，invoke执行方法，获取返回值
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            return RPCResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RPCResponse.fail();
        }
    }
}
```


## 6. 整体流程
1. 客户端
    1. TestClient客户端向目标ip和端口发起服务请求
    2. ClientProxy代理类通过动态代理将参数封装成request，请求IOClient发送给目标服务器
    3. IOClient将request写入到流，并获取服务器的response

2. 服务器
    1. TestServer注册已有服务，将服务存入ServiceProvider的Map中
    2. ThreadPoolService通过BIO方式监听流，有事件，创建一个WorkThread处理服务
    3. WorkThread从流中读取request，通过服务名和参数，通过反射invoke执行服务，获取返回response，返回给客户端


## 7. 存在问题
1. BIO方式效率低(使用Netty)