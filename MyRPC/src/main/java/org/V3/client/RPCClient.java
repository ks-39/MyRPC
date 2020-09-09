package org.V3.client;


import org.V3.pojo.RPCRequest;
import org.V3.pojo.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
