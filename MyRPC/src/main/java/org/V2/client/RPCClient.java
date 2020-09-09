package org.V2.client;


import org.V2.pojo.RPCRequest;
import org.V2.pojo.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
