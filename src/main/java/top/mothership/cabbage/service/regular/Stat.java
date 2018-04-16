package top.mothership.cabbage.service.regular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.enums.Parameters;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.OsuApiManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.PlayerInfoDAO;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.Parameter;
import top.mothership.cabbage.pojo.osu.PlayerInfo;
import top.mothership.cabbage.service.CommandHandler;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.ImgUtil;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author QHS
 */
@Component
public class Stat implements CommandHandler {

    private static final List<Parameter> PARAMETER_LIST
            = Arrays.asList(
            Parameter.builder().index(0).type(Parameters.USERNAME).required(true).build(),
            Parameter.builder().index(1).type(Parameters.DAY).required(false).build()
    );
    private final CqManager cqManager;
    private final OsuApiManager osuApiManager;
    private final UserDAO userDAO;
    private final RedisDAO redisDAO;
    private final UserUtil userUtil;
    private final PlayerInfoDAO playerInfoDAO;
    private final ImgUtil imgUtil;
    private final WebPageManager webPageManager;

    @Autowired
    public Stat(CqManager cqManager, OsuApiManager osuApiManager, UserDAO userDAO, RedisDAO redisDAO, UserUtil userUtil, PlayerInfoDAO playerInfoDAO, ImgUtil imgUtil, WebPageManager webPageManager) {
        this.cqManager = cqManager;
        this.osuApiManager = osuApiManager;
        this.userDAO = userDAO;
        this.redisDAO = redisDAO;
        this.userUtil = userUtil;
        this.playerInfoDAO = playerInfoDAO;
        this.imgUtil = imgUtil;
        this.webPageManager = webPageManager;
    }

    @Override
    public List<Parameter> getParameters() {
        return PARAMETER_LIST;
    }

    @Override
    public List<CommandIdentifier> getIdentifier() {
        return Collections.singletonList(CommandIdentifier.STAT);
    }

    @Override
    public String doService(Argument argument) {
        PlayerInfo playerInfoFromAPI;
        PlayerInfo playerInfoFromDB = null;
        String role;
        User user;
        List<String> roles;
        Integer scoreRank = null;
        boolean approximate = false;
        if ("白菜".equals(argument.getUsername())) {
            return "没人疼，没人爱，我是地里一颗小白菜。";
        }
        //直接从api根据参数提供的用户名获取
        playerInfoFromAPI = osuApiManager.getUser(0, argument.getUsername());

        if (playerInfoFromAPI == null) {
            return String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername());
        }


        user = userDAO.getUser(null, playerInfoFromAPI.getUserId());
        if (user == null) {
            //未指定mode的时候改为0
            if (argument.getMode() == null) {
                argument.setMode(0);
            }
            userUtil.registerUser(playerInfoFromAPI.getUserId(), argument.getMode(), 0L, OverallConsts.DEFAULT_ROLE);
            playerInfoFromDB = playerInfoFromAPI;
            role = OverallConsts.DEFAULT_ROLE;
            //初次使用，数据库肯定没有指定天数的数据，直接标为近似数据
            approximate = true;
        } else {
            //未指定mode的时候改为玩家预设的模式
            if (argument.getMode() == null) {
                argument.setMode(user.getMode());
            }
            if (!argument.getMode().equals(0)) {
                //2018-1-22 12:59:06如果这个玩家的模式不是主模式，则取出相应模式
                playerInfoFromAPI = osuApiManager.getUser(argument.getMode(), user.getUserId());
            }
            role = user.getRole();
            if (argument.getDay() > 0) {
                if (argument.getDay().equals(1)) {
                    playerInfoFromDB = redisDAO.get(playerInfoFromAPI.getUserId(), argument.getMode());
                }
                if (playerInfoFromDB == null) {
                    playerInfoFromDB = playerInfoDAO.getPlayerInfo(argument.getMode(), playerInfoFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                    if (playerInfoFromDB == null) {
                        playerInfoFromDB = playerInfoDAO.getNearestPlayerInfo(argument.getMode(), playerInfoFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                        approximate = true;
                    }
                }
            }
        }

        roles = userUtil.sortRoles(role);
        //主模式才获取score rank
        if (argument.getMode().equals(0)) {
            //2018-4-4 11:03:16一阵唏嘘
            //当年5位刷分玩家问我要的特殊对待，在1w名以内就显示他们的score rank（其他人都是2k以内），现在只有一个小白菜还在2k外了……
            if (playerInfoFromAPI.getUserId() == 3995056) {
                scoreRank = webPageManager.getRank(playerInfoFromAPI.getRankedScore(), 1, 10000);
            } else {
                scoreRank = webPageManager.getRank(playerInfoFromAPI.getRankedScore(), 1, 2000);
            }
            //调用绘图类绘图(2017-10-19 14:09:04 roles改为List，排好序后直接取第一个)
        }
        String result = imgUtil.drawUserInfo(playerInfoFromAPI, playerInfoFromDB, roles.get(0), argument.getDay(), approximate, scoreRank, argument.getMode());
        return "[CQ:image,file=base64://" + result + "]";
    }
}
