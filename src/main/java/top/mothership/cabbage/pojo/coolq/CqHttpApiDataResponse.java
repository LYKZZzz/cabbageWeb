package top.mothership.cabbage.pojo.coolq;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * HTTP API的查询返回结果（2018-4-10 09:50:56目前仅用于查询Bot加入的群）
 */
@Data
public class CqHttpApiDataResponse {
    private Integer id;
    private String nickname;
    @SerializedName("group_name")
    private String groupName;
    @SerializedName("group_id")
    private Long groupId;
    private String cookies;
    private Long token;
    @SerializedName("coolq_edition")
    private String coolqEdition;
    @SerializedName("plugin_version")
    private String pluginVersion;

}