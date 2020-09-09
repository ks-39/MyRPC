package org.V4.client;


import org.V4.pojo.RPCRequest;
import org.V4.pojo.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
