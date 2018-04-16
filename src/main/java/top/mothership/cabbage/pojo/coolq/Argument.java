package top.mothership.cabbage.pojo.coolq;

import lombok.Data;
import top.mothership.cabbage.enums.MessageSource;
import top.mothership.cabbage.pojo.shadowsocks.ShadowSocksRequest;

import java.util.List;

/**
 * 从命令中解析出的参数
 */
@Data
public class Argument {
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
    private ShadowSocksRequest ssr;
    private String artist;
    private String title;
    private String diffName;
    private String mapper;
    private Double ar;
    private Double od;
    private Double cs;
    private Double hp;
    private Integer mods;
    private String modsString;
    private Integer beatmapId;
    private Integer countMiss;
    private Integer count100;
    private Integer count50;
    private Integer maxCombo;
    private Double acc;
}
