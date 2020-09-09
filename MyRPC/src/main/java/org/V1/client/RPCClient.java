package org.V1.client;


import org.V1.pojo.RPCRequest;
import org.V1.pojo.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
