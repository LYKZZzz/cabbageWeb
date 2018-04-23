package top.mothership.cabbage.service.regular;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.enums.Parameters;
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

@Component
public class StatByUid implements CommandHandler {

    private static final List<Parameter> PARAMETER_LIST
            = Arrays.asList(
            Parameter.builder().index(0).type(Parameters.USER_ID).required(true).build(),
            Parameter.builder().index(1).type(Parameters.DAY).required(false).build()
    );
    private final OsuApiManager osuApiManager;
    private final UserDAO userDAO;
    private final RedisDAO redisDAO;
    private final UserUtil userUtil;
    private final PlayerInfoDAO playerInfoDAO;
    private final ImgUtil imgUtil;
    private final WebPageManager webPageManager;

    public StatByUid(OsuApiManager osuApiManager, UserDAO userDAO, RedisDAO redisDAO, UserUtil userUtil, PlayerInfoDAO playerInfoDAO, ImgUtil imgUtil, WebPageManager webPageManager) {
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
        return Collections.singletonList(CommandIdentifier.STATU);
    }

    @Override
    public String doService(Argument argument) {
        PlayerInfo playerInfoFromAPI;
        PlayerInfo playerInfoFromDB = null;
        String role;
        User user;
        boolean approximate;
        //先尝试根据提供的uid从数据库取出数据
        user = userDAO.getUser(null, argument.getUserId());
        playerInfoFromAPI = osuApiManager.getUser(0, argument.getUserId());

        if (user == null) {
            if (playerInfoFromAPI == null) {
                return String.format(TipConsts.USER_ID_GET_FAILED_AND_NOT_USED, argument.getUserId());
            } else {
                //构造User对象和4条playerInfo写入数据库，如果指定了mode就使用指定mode
                if (argument.getMode() == null) {
                    argument.setMode(0);
                }
                userUtil.registerUser(playerInfoFromAPI.getUserId(), argument.getMode(), 0L, OverallConsts.DEFAULT_ROLE);
                playerInfoFromDB = playerInfoFromAPI;
                //初次使用，数据库肯定没有指定天数的数据
                approximate = true;
            }
            role = OverallConsts.DEFAULT_ROLE;
        } else if (user.isBanned()) {
            //只有在确定user不是null的时候，如果参数没有提供mode，用user预设的覆盖
            if (argument.getMode() == null) {
                argument.setMode(user.getMode());
            }
            //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份playerInfo，作为要展现的数据传给绘图类
            playerInfoFromAPI = playerInfoDAO.getNearestPlayerInfo(argument.getMode(), user.getUserId(), LocalDate.now());
            if (playerInfoFromAPI == null) {
                //如果数据库中该玩家该模式没有历史记录……
                return TipConsts.USER_IS_BANNED;
            }
            //尝试补上当前用户名
            if (user.getCurrentUname() != null) {
                playerInfoFromAPI.setUserName(user.getCurrentUname());
            } else {
                List<String> list = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                }.getType());
                if (list.size() > 0) {
                    playerInfoFromAPI.setUserName(list.get(0));
                } else {
                    playerInfoFromAPI.setUserName(String.valueOf(user.getUserId()));
                }
            }
            argument.setDay(0);
            role = user.getRole();
        } else {
            if (argument.getMode() == null) {
                argument.setMode(user.getMode());
            }
            role = user.getRole();
            if (argument.getDay() > 0) {
                if (argument.getDay().equals(1)) {
                    //加一个从redis取数据的设定
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


        return null;
    }
}
