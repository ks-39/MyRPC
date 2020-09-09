package org.V4.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.V4.pojo.RPCRequest;
import org.V4.pojo.RPCResponse;
import org.V4.register.ServiceRegister;
import org.V4.register.ZkServiceRegister;

import java.net.InetSocketAddress;

/**
 * 实现RPCClient接口
 */
public class NettyRPCClient implements RPCClient {

    //启动参数
    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;
    private String host;
    private int port;

    //zk注册中心
    private ServiceRegister serviceRegister;
    public NettyRPCClient() {
        this.serviceRegister = new ZkServiceRegister();
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
        InetSocketAddress address = serviceRegister.serviceDiscovery(request.getInterfaceName());
        host = address.getHostName();
        port = address.getPort();

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

            System.out.println(response);
            return response;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
