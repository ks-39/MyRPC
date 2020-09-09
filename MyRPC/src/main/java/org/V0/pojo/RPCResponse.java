package org.V0.pojo;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 上个例子中response传输的是User对象，显然在一个应用中我们不可能只传输一种类型的对象
 * 由此需要传输对象抽象成为Object
 * Rpc需要经过网络传输，有可能失败，类似于http，引入状态码和状态信息表示服务调用成功还是失败
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RPCResponse implements Serializable {

    //Response解耦

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
