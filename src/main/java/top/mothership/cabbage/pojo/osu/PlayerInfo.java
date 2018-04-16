package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.LocalDate;
@Data
public class PlayerInfo {
    /**
     * 这个字段不写入数据库
     */
    @SerializedName("username")
    private String userName;
    private Integer mode;
    private Integer userId;
    private Integer count300;
    private Integer count100;
    private Integer count50;
    @SerializedName("playcount")
    private Integer playCount;
    private Double accuracy;
    private Double ppRaw;
    private Long rankedScore;
    private Long totalScore;
    private Double level;
    private Integer ppRank;
    private Integer countRankSs;
    private Integer countRankSsh;
    private Integer countRankS;
    private Integer countRankSh;
    private Integer countRankA;
    private LocalDate queryDate;

}
