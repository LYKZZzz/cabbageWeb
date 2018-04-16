package top.mothership.cabbage.pojo.coolq;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * 调用HTTP API发送消息之后的返回体，使用泛型封装了HTTP API的几种返回对象
 *
 * @param <T>
 * @author QHS
 */
@Data
public class CqHttpApiGenericResponse<T>{

    private String status;
    @SerializedName("retcode")
    private int retCode;
    private T data;


}