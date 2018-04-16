package top.mothership.cabbage.consts;

import java.util.ResourceBundle;

/**
 * 全局常量
 *
 * @author QHS
 */
public class OverallConsts {
    /**
     * 指定配置文件
     */
    public final static ResourceBundle CABBAGE_CONFIG = ResourceBundle.getBundle("cabbage");

    public final static long[] ADMIN_LIST = {2307282906L,1335734657L,2643555740L,992931505L};

    public final static String DEFAULT_ROLE = "creep";

    public final static String CHANGELOG = "2018-4-8 15:38:32\n" +
            "*修正 新的参数校验：明确指定参数是否必须、以及应该出现的位置。\n" +
            "*新增 !stat命令分离金/银SS。\n" +
            "*删除 去掉给某个时雨厨的报时。\n" +
            "*新增 !sudo score命令。\n" +
            "*恢复 黄花菜对接相关。\n";
}

