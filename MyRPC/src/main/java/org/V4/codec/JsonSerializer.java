package org.V4.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.V4.pojo.RPCRequest;
import org.V4.pojo.RPCResponse;

/**
 * 由于json序列化的方式是通过把对象转化成字符串，丢失了Data对象的类信息，所以deserialize需要
 * 了解对象对象的类信息，根据类信息把JsonObject -> 对应的对象
 */
public class JsonSerializer implements Serializer{

    //编码
    //将对象的JsonObject转换为字符数组
    @Override
    public byte[] serialize(Object obj) {
        byte[] bytes = JSONObject.toJSONBytes(obj);
        return bytes;
    }

    //解码
    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        Object obj = null;

        switch (messageType){
            case 0:
                RPCRequest request = JSON.parseObject(bytes, RPCRequest.class);
                // 修bug 参数为空 直接返回
                if(request.getParams() == null)
                    return request;

                Object[] objects = new Object[request.getParams().length];

                // 把json字串转化成对应的对象， fastjson可以读出基本数据类型，不用转化
                for(int i = 0; i < objects.length; i++){
                    Class<?> paramsType = request.getParamsTypes()[i];
                    if (!paramsType.isAssignableFrom(request.getParams()[i].getClass())){
                        objects[i] = JSONObject.toJavaObject((JSONObject) request.getParams()[i],request.getParamsTypes()[i]);
                    }else{
                        objects[i] = request.getParams()[i];
                    }

                }
                request.setParams(objects);
                obj = request;
                break;
            case 1:
                RPCResponse response = JSON.parseObject(bytes, RPCResponse.class);
                Class<?> dataType = response.getDataType();
                if(! dataType.isAssignableFrom(response.getData().getClass())){
                    response.setData(JSONObject.toJavaObject((JSONObject) response.getData(),dataType));
                }
                obj = response;
                break;
            default:
                System.out.println("暂时不支持此种消息");
                throw new RuntimeException();
        }
        return obj;
    }

    // 1 代表着json序列化方式
    @Override
    public int getType() {
        return 1;
    }
}
