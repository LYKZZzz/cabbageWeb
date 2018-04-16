package top.mothership.cabbage.consts;

public class TipConsts {
    public static final String USER_NOT_BIND = "你没有绑定osu!id。请使用!setid 你的osuid 命令。";
    public static final String USER_IS_BANNED = "……期待你回来的那一天。";
    public static final String USER_GET_FAILED = "没有从osu!api获取到QQ为%d的用户绑定的uid为%d的玩家信息。";
    public static final String BEATMAP_GET_FAILED = "没有从osu!api获取到bid为%d的谱面信息。";
    public static final String USERNAME_GET_FAILED = "没有从osu!api获取到名为%s的玩家信息。";
    public static final String USERID_GET_FAILED = "没有从osu!api获取到uid为%d的玩家信息。";
    public static final String USER_ID_GET_FAILED_AND_NOT_USED = "没有从osu!api获取到uid为%d的玩家信息。";
    public static final String FORMAT_ERROR = "“%s”不是合法的%s。我们不再支持在命令后添加其他内容。";
    public static final String ARGUMENT_LACK = "该命令在位置%d缺少必需参数：%s，";
    public static final String QUERY_BANCHO_BOT = "你们总是想查BanchoBot。" +
            "\n可是BanchoBot已经很累了，她不想被查。" +
            "\n她想念自己的小ppy，而不是被逼着查PP。" +
            "\n你有考虑过这些吗？没有！你只考虑过你自己。";
    public static final String BEATMAP_NO_SCORE = "没有从osu!api获取到谱面%d在模式%s的排行榜。";
    public static final String THIS_USER_BEATMAP_NO_SCORE = "没有从osu!api获取到玩家%s在谱面%d、模式%s的分数。";
    public static final String NO_RECENT_RECORD = "玩家%s在模式%s最近没有游戏记录。";
    public static final String NO_RECENT_RECORD_PASSED = "玩家%s在模式%s最近没有Pass的游戏记录。";
}
