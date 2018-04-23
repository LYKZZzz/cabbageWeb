package top.mothership.cabbage.enums;

/**
 * 与命令字符串一一对应的枚举，Controller中根据解析出的命令字符串来这个枚举取对象，再根据对象取出对应的命令处理器
 *
 * @author QHS
 */
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

    STAT, STATU, STATME,
    BP, BPU, BPS, BPUS, BPME, MYBP, BPMES, MYBPS,
    RECENT, RS,
    SETID, SLEEP,
    ADD, DEL,
    ME, SEARCH,
    COST, COSTME, MYCOST,
    PR, PRS,
    MYBNS, BNSME, BNS,
    ROLL, TIME,
    MODE, HELP,

    MP_RS, MP_MAKE, MP_INVITE, MP_LIST, MP_ABORT, MP_JOIN, MP_ADDMAP, MP_DELMAP, MP_LISTMAP, MP_HELP,

    ADDMAP, DELMAP,

    UPDUSER, GETCODE;

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
