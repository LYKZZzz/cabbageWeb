package top.mothership.cabbage.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.enums.MessageSource;
import top.mothership.cabbage.factory.CommandHandlerFactory;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.DayLilyManager;
import top.mothership.cabbage.pattern.CQCodePattern;
import top.mothership.cabbage.pattern.RegularPattern;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.service.CommandHandler;
import top.mothership.cabbage.service.CqAdminServiceImpl;
import top.mothership.cabbage.service.CqServiceImpl;
import top.mothership.cabbage.util.qq.SmokeUtil;

import javax.annotation.PostConstruct;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * CQ控制器，用于处理CQ消息
 *
 * @author QHS
 */
@RestController
public class CqController {

    private final CqServiceImpl cqService;
    private final SmokeUtil smokeUtil;
    private final CqAdminServiceImpl cqAdminService;
    private final CqManager cqManager;
    private final DayLilyManager dayLilyManager;


    private Logger logger = LogManager.getLogger(this.getClass());

    public CqController(CqServiceImpl cqService, SmokeUtil smokeUtil, CqAdminServiceImpl cqAdminService, CqManager cqManager, DayLilyManager dayLilyManager) {
        this.cqService = cqService;
        this.smokeUtil = smokeUtil;
        this.cqAdminService = cqAdminService;
        this.cqManager = cqManager;
        this.dayLilyManager = dayLilyManager;
    }


    /**
     * Controller主方法
     *
     * @param cqMsg 传入的QQ消息
     * @throws Exception 抛出异常给AOP检测
     */
    @RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public void cqMsgParse(@RequestBody CqMsg cqMsg) throws Exception {
        String returnMsg = null;
        Argument argument = new Argument();
        //标记为QQ消息
        argument.setMessageSource(MessageSource.QQ);
        //待整理业务逻辑
        switch (cqMsg.getPostType()) {
            case "message":
                //转义
                String msg = cqMsg.getMessage();
                msg = msg.replaceAll("&#91;", "[");
                msg = msg.replaceAll("&#93;", "]");
                msg = msg.replaceAll("&#44;", ",");
                cqMsg.setMessage(msg);
                String msgWithoutImage;
                Matcher imageMatcher = CQCodePattern.SINGLE_IMG.matcher(msg);
                if (imageMatcher.find()) {
                    //替换掉消息内所有图片
                    msgWithoutImage = imageMatcher.replaceAll("");
                } else {
                    msgWithoutImage = msg;
                }
                //识别消息类型，根据是否是群聊，加入禁言消息队列
                switch (cqMsg.getMessageType()) {
                    case "group":
                        smokeUtil.parseSmoke(cqMsg);
                        break;
                    default:
                        break;
                }

                Matcher cmdMatcher = RegularPattern.REG_CMD_REGEX.matcher(msgWithoutImage);

                if (cmdMatcher.find()) {
                    //如果检测到命令，直接把消息中的图片去掉，避免Service层进行后续处理
                    //2018-4-13 15:43:49改为设置Argument参数
                    argument.setRawMessage(msgWithoutImage);
                    switch (cmdMatcher.group(1).toLowerCase(Locale.CHINA)) {
                        //处理命令
                        case "sudo":
                        case "mp":
                            //这些命令需要换一个正则表达式
                            cmdMatcher = RegularPattern.DOUBLE_COMMAND_REGEX.matcher(msg);
                            //执行一次find
                            cmdMatcher.find();
                            //查找有没有命令处理器
                            if (CommandIdentifier.contains(
                                    (cmdMatcher.group(1) + "_" + cmdMatcher.group(2)).toUpperCase())) {
                                CommandHandler ch =
                                        CommandHandlerFactory.build(
                                                CommandIdentifier.valueOf((cmdMatcher.group(1) + "_" + cmdMatcher.group(2)).toUpperCase()));
                                returnMsg = ch.doService(argument);
                                //发送执行结果
                                cqMsg.setMessage(returnMsg);
                                cqManager.sendMsg(cqMsg);
                            }
                            break;
                        default:
                            Matcher recentQianeseMatcher = RegularPattern.QIANESE_RECENT.matcher(cmdMatcher.group(1).toLowerCase(Locale.CHINA));
                            if (recentQianeseMatcher.find()) {
                                CommandHandler ch = CommandHandlerFactory.build(CommandIdentifier.RECENT);
                                returnMsg = ch.doService(argument);
                                cqMsg.setMessage(returnMsg);
                                cqManager.sendMsg(cqMsg);
                            } else {
                                //检测白菜是否能处理这条命令
                                if (CommandIdentifier.contains(cmdMatcher.group(1).toUpperCase())) {
                                    CommandHandler ch = CommandHandlerFactory.build(
                                            CommandIdentifier.valueOf(cmdMatcher.group(1).toUpperCase()));
                                    returnMsg = ch.doService(argument);
                                    cqMsg.setMessage(returnMsg);
                                    cqManager.sendMsg(cqMsg);
                                } else {
                                    //如果找不到对应的处理方法
                                    Matcher sleepMatcher = RegularPattern.SLEEP_REGEX.matcher(msgWithoutImage);
                                    if (sleepMatcher.find()) {
                                        //sleep专用正则，感叹号前面加东西不工作
                                        dayLilyManager.sendMsg(cqMsg);
                                    }
                                }
                            }
                    }

                }
                break;
            case "event":
                if ("group_increase".equals(cqMsg.getEvent())) {
                    //新增人口
                    cqService.welcomeNewsPaper(cqMsg);
                }
                if ("group_decrease".equals(cqMsg.getEvent())) {
                    //有人退群
                    cqService.seeYouNextTime(cqMsg);
                }
                if ("group_admin".equals(cqMsg.getEvent())) {
                    //群管变动
                    smokeUtil.loadGroupAdmins();
                }
                break;
            case "request":
                //只有是加群请求的时候才进入
                if ("group".equals(cqMsg.getRequestType()) && "invite".equals(cqMsg.getSubType())) {
                    cqAdminService.stashInviteRequest(cqMsg);
                }
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTP API插件已经更新");
        }
    }

    @PostConstruct
    private void notifyInitComplete() {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("private");
        cqMsg.setQQ(1335734657L);
        cqMsg.setMessage("初始化完成，欢迎使用");
        cqManager.sendMsg(cqMsg);
    }
}
