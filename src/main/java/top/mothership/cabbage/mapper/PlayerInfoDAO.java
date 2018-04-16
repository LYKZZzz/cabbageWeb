package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.osu.PlayerInfo;

import java.time.LocalDate;
import java.util.List;
@Mapper
@Repository
public interface PlayerInfoDAO {

    @Insert("INSERT INTO `player_info` VALUES(null," +
            "#{userinfo.mode},#{userinfo.userId}," +
            "#{userinfo.count300},#{userinfo.count100}," +
            "#{userinfo.count50},#{userinfo.playCount}," +
            "#{userinfo.accuracy},#{userinfo.ppRaw}," +
            "#{userinfo.rankedScore},#{userinfo.totalScore}," +
            "#{userinfo.level},#{userinfo.ppRank}," +
            "#{userinfo.countRankSs},#{userinfo.countRankS}," +
            "#{userinfo.countRankA},#{userinfo.queryDate}" +
            ")")
    Integer addPlayerInfo(@Param("userinfo") PlayerInfo playerInfo);

    @Select("SELECT * FROM `player_info` WHERE `user_id` = #{userId}")
    List<PlayerInfo> listPlayerInfoByUserId(@Param("userId") Integer userId);

    @Select("SELECT * , abs(UNIX_TIMESTAMP(queryDate) - UNIX_TIMESTAMP(#{queryDate})) AS ds " +
            "FROM `player_info`  WHERE `user_id` = #{userId} AND `mode` = #{mode} ORDER BY ds ASC LIMIT 1")
    PlayerInfo getNearestPlayerInfo(@Param("mode") Integer mode, @Param("userId") Integer userId, @Param("queryDate") LocalDate queryDate);

    @Select("SELECT * FROM `player_info` WHERE `user_id` = #{userId} AND `queryDate` = #{queryDate} AND `mode` = #{mode}")
    PlayerInfo getPlayerInfo(@Param("mode") Integer mode, @Param("userId") Integer userId, @Param("queryDate") LocalDate queryDate);

    @Delete("DELETE FROM `player_info` WHERE `queryDate` = #{queryDate}")
    void clearTodayInfo(@Param("queryDate") LocalDate queryDate);
}


