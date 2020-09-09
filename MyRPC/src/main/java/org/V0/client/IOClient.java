package org.V0.client;

import org.V0.pojo.RPCRequest;
import org.V0.pojo.RPCResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
