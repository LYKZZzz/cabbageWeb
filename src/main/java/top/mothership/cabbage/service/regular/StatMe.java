package top.mothership.cabbage.service.regular;

import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.enums.Parameters;
import top.mothership.cabbage.manager.OsuApiManager;
import top.mothership.cabbage.mapper.PlayerInfoDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.Parameter;
import top.mothership.cabbage.pojo.osu.PlayerInfo;
import top.mothership.cabbage.service.CommandHandler;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class StatMe implements CommandHandler {
    private final UserDAO userDAO;
    private final PlayerInfoDAO playerInfoDAO;
    private final OsuApiManager osuApiManager;
    private List<Parameter> PARAMETER_LIST = Arrays.asList(
            Parameter.builder().index(1).type(Parameters.DAY).required(false).build(),
            Parameter.builder().index(2).type(Parameters.MODE).required(false).build()

    );

    public StatMe(UserDAO userDAO, PlayerInfoDAO playerInfoDAO, OsuApiManager osuApiManager) {
        this.userDAO = userDAO;
        this.playerInfoDAO = playerInfoDAO;
        this.osuApiManager = osuApiManager;
    }

    @Override
    public List<Parameter> getParameters() {
        return PARAMETER_LIST;
    }

    @Override
    public List<CommandIdentifier> getIdentifier() {
        return Collections.singletonList(CommandIdentifier.STATME);
    }

    @Override
    public String doService(Argument argument) {
        User user;
        String role;
        PlayerInfo playerInfoFromAPI;
        PlayerInfo playerInfoFromDB;
        //由于statme是对本人的查询，先尝试取出绑定的user，如果没有绑定过给出相应提示
        //2018-4-23 13:20:37根据消息来源进行判断，尝试取出对应的user
        switch (argument.getMessageSource()) {
            case DISCORD:
                //TODO 完成移植业务逻辑后来调试discord
                break;
            case QQ:
                user = userDAO.getUser(argument.getSenderId(), null);
                break;
            default:
                break;
        }
        if (user == null) {
            return TipConsts.USER_NOT_BIND;
        }
        if (argument.getMode() == null) {
            //如果查询没有指定mode，用用户预设的mode覆盖
            argument.setMode(user.getMode());
        }
        //根据绑定的信息从ppy获取一份玩家信息
        playerInfoFromAPI = osuApiManager.getUser(argument.getMode(), user.getUserId());
        role = user.getRole();

        if (user.isBanned()) {
            //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
            playerInfoFromAPI = playerInfoDAO.getNearestPlayerInfo(argument.getMode(), user.getUserId(), LocalDate.now());
            if (playerInfoFromAPI == null) {
                //如果数据库中该玩家该模式没有历史记录……
                return TipConsts.USER_IS_BANNED;
            }
            //尝试补上当前用户名

            //玩家被ban就把日期改成0，因为没有数据进行对比
            argument.setDay(0);
        } else {
            if (playerInfoFromAPI == null) {
                cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, user.getQq(), user.getUserId()));
                cqManager.sendMsg(cqMsg);
                return;
            }
            if (argument.getDay() > 0) {
                if (argument.getDay().equals(1)) {
                    //加一个从redis取数据的设定
                    userInDB = redisDAO.get(playerInfoFromAPI.getUserId(), argument.getMode());
                }
                if (userInDB == null) {
                    userInDB = userInfoDAO.getUserInfo(argument.getMode(), playerInfoFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                    if (userInDB == null) {
                        userInDB = userInfoDAO.getNearestUserInfo(argument.getMode(), playerInfoFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                        approximate = true;
                    }
                }
            }
        }


        return null;
    }
}
