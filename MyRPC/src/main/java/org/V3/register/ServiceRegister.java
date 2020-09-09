package org.V3.register;

import java.net.InetSocketAddress;

public interface ServiceRegister {

    //注册并保存地址
    void register(String serviceName, InetSocketAddress serverAddress);

    //根据服务名查询地址
    InetSocketAddress serviceDiscovery(String serviceName);
}
