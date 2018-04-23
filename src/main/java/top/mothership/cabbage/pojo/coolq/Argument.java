package top.mothership.cabbage.pojo.coolq;

import lombok.Data;
import top.mothership.cabbage.enums.MessageSource;
import top.mothership.cabbage.pojo.osu.OsuSearchParameter;
import top.mothership.cabbage.pojo.shadowsocks.ShadowSocksRequest;

import java.util.List;

/**
 * 从命令中解析出的参数
 */
@Data
public class Argument {
    /**
     * 在QQ消息里表示QQ号
     */
    private Long senderId;
    private String rawMessage;
    private String subCommandLowCase;
    private MessageSource messageSource;
    private Integer mode;
    private Integer day;
    private Integer num;
    private boolean text;
    private String username;
    private Integer userId;
    private Long QQ;
    private Long hour;
    private List<String> usernames;
    private Long groupId;
    private String role;
    private String flag;
    private String fileName;
    private String url;
    private Integer second;
    private Integer beatmapId;
    private ShadowSocksRequest ssr;
    private OsuSearchParameter osuSearchParameter;
}
