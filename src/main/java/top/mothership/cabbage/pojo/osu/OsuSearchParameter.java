package top.mothership.cabbage.pojo.osu;

import lombok.Data;

@Data
public class OsuSearchParameter {
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
    private Integer mode;
}
