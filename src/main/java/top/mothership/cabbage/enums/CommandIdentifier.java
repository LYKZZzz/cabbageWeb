package top.mothership.cabbage.enums;

public enum CommandIdentifier {
    //用户组信息管理
    SUDO_ADD, SUDO_DEL, SUDO_AFK, SUDO_ROLEINFO,
    //群内管理
    SUDO_SMOKE, SUDO_LISTMSG, SUDO_GROUPINFO, SUDO_REPEATSTAR,
    //群邀请管理
    SUDO_LISTINVITE, SUDO_CLEARINVITE, SUDO_HANDLEINVITE,
    //特权命令
    SUDO_BG, SUDO_RECENT, SUDO_RS, SUDO_FP, SUDO_HELP, SUDO_SCORE,
    //玩家信息管理
    SUDO_SEARCHPLAYER, SUDO_UNBIND, SUDO_钦点,

    STAT, STATME, BP, RECENT;

    /**
     * 自己实现一个判断是否有某个名字的方法
     *
     * @param name
     * @return 枚举中是否包含某个项
     */
    public static boolean contains(String name) {

        CommandIdentifier[] season = values();
        //遍历查找
        for (CommandIdentifier s : season) {
            if (s.name().equals(name)) {
                return true;
            }
        }
        return false;
    }


}
