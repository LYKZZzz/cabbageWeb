package top.mothership.cabbage.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.annotation.SearchParameter;
import top.mothership.cabbage.annotation.UserAuthorityControl;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.pattern.CQCodePattern;
import top.mothership.cabbage.pattern.RegularPattern;
import top.mothership.cabbage.pattern.SearchKeywordPattern;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.Parameter;
import top.mothership.cabbage.pojo.shadowsocks.ShadowSocksRequest;
import top.mothership.cabbage.service.CommandHandler;
import top.mothership.cabbage.util.osu.ScoreUtil;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;

/**
 * 验证参数的切面
 *
 * @author QHS
 */
@Component
@Aspect
@Order(2)
public class ParameterVerifyAspect {
    private final CqManager cqManager;
    private final ScoreUtil scoreUtil;
    private final ResDAO resDAO;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Spring自动注入
     *
     * @param cqManager the cq manager
     * @param scoreUtil the score util
     * @param resDAO    the res dao
     */
    public ParameterVerifyAspect(CqManager cqManager, ScoreUtil scoreUtil, ResDAO resDAO) {
        this.cqManager = cqManager;
        this.scoreUtil = scoreUtil;
        this.resDAO = resDAO;
    }

    /**
     * 当时为啥要指定CQ开头来着…
     * 2018-4-4 10:30:48重构为每个命令一个类，废弃原先的cqService
     */
    @Pointcut("execution(* top.mothership.cabbage.service.*.*.doService(top.mothership.cabbage.pojo.coolq.CqMsg,..))")
    private void service() {
    }


    /**
     * 统一处理命令的参数
     *
     * @param pjp      the pjp
     * @param argument 抽象出的命令体
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("service() && args(argument,..)")
    public Object verify(ProceedingJoinPoint pjp, Argument argument) throws Throwable {

        CommandHandler ch = (CommandHandler) pjp.getTarget();
        List<Parameter> parameterList = ch.getParameters();
        List<String> argumentList;
        String rawArgument;

        Matcher m = RegularPattern.REG_CMD_REGEX.matcher(argument.getRawMessage());
        m.find();
        //2018-4-4 17:29:41改用switch
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            //首先把命令和所有参数字符串切割开来
            case "sudo":
            case "mp":
                m = RegularPattern.DOUBLE_COMMAND_REGEX.matcher(argument.getRawMessage());
                m.find();
                //这条正则表达式第二个匹配的才是命令
                argument.setSubCommandLowCase(m.group(2).toLowerCase(Locale.CHINA));
                rawArgument = m.group(3);
                break;
            default:
                argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                rawArgument = m.group(2);
                break;
        }
        if (parameterList == null) {
            //如果命令不接受任何参数
            //2018-2-27 16:09:47漏掉了argument，在这里也需要一个argument（
            //2018-4-13 10:25:53切面改为处理argument
            return pjp.proceed();
        }


        //根据是否需要osu!search参数、以及是否使用Shell格式来进行分割

        SearchParameter searchParameter = null;
        Annotation[] a = pjp.getTarget().getClass().getAnnotations();
        for (Annotation aList : a) {
            if (aList.annotationType().equals(UserAuthorityControl.class)) {
                searchParameter = (SearchParameter) aList;
            }

        }

        //如果Class上的注解不是null
        if (searchParameter != null) {
            //需要osu!search参数
            argumentList = splitSearchParameter(rawArgument);
        } else {
            argumentList = splitRegularParameter(rawArgument);
        }


        //预定义变量，day默认为1，这样才能默认和昨天的比较
        Integer day = 1;
        //mode预设为null
        //2018-2-27 09:42:45 由于各个命令 未指定Mode的时候表现不同，所以不能预设为0
        Integer mode = null;
        Integer num = null;


        Matcher legalParamMatcher;

        for (Parameter p : parameterList) {
            //先问是不是，再问为什么，首先检测这个参数是否存在
            //直接复用rawArgument，下同
            rawArgument = argumentList.get(p.getIndex());
            if (rawArgument == null) {
                if (p.isRequired()) {
                    //2018-4-13 11:46:03对啊 现在我可以直接返回字符串了……
                    return String.format(TipConsts.ARGUMENT_LACK, p.getIndex(), p.getType());
                } else {
                    //如果某个参数不是必须的，同时也不存在（例如钦点命令不必须要有QQ），直接跳出本次循环，也避免了接下来反复书写判空
                    //TODO sleep命令的6小时移到命令内部去判定
                    continue;
                }
            }
            //彩蛋统一处理

            //然后按类型检测是否合法
            switch (p.getType()) {
                case QQ:
                    legalParamMatcher = RegularPattern.QQ.matcher(rawArgument);
                    if (legalParamMatcher.find()) {
                        argument.setQQ(Long.valueOf(rawArgument));
                    } else {
                        return String.format(TipConsts.FORMAT_ERROR, rawArgument, "QQ号");
                    }
                    break;
                case USER_ID:
                    legalParamMatcher = RegularPattern.OSU_USER_ID.matcher(rawArgument);
                    if (legalParamMatcher.find()) {
                        argument.setUserId(Integer.valueOf(rawArgument));
                    } else {
                        return String.format(TipConsts.FORMAT_ERROR, rawArgument, "osu!uid");
                    }
                    break;
                case USERNAME:
                    legalParamMatcher = RegularPattern.OSU_USER_NAME.matcher(rawArgument);
                    if (legalParamMatcher.find() || "白菜".equals(rawArgument)) {
                        //2018-2-27 09:40:11这里把彩蛋放过去，在各个命令的方法里具体处理
                        argument.setUsername(rawArgument);
                    } else {
                        return String.format(TipConsts.FORMAT_ERROR, rawArgument, "osu!用户名");
                    }
                    break;
                case MODE:
                    mode = convertModeStrToInteger(rawArgument);
                    if (mode == null) {
                        return String.format(TipConsts.FORMAT_ERROR, rawArgument, "osu!游戏模式");
                    }
                    argument.setMode(mode);
                    break;
                case FILENAME:
                    //没必要做验证
                    argument.setFileName(rawArgument);
                    break;
                case URL:
                    //重要！URL必须是冒号后面的"第三"参数（否则会将一部分的URL解析为第三参数）
                    legalParamMatcher = RegularPattern.URL.matcher(rawArgument);
                    if (legalParamMatcher.find()) {
                        argument.setUrl(rawArgument);
                    } else {
                        return String.format(TipConsts.FORMAT_ERROR, rawArgument, "URL");
                    }
                    break;
                case ROLE:
                    argument.setRole(rawArgument);
                    break;
                //以下为谱面搜索参数，和其他不一样
                case ARTIST:
                    argument.setArtist(rawArgument);
                    break;
                case TITIE:
                    argument.setTitle(rawArgument);
                    break;
                case DIFFNAME:
                    argument.setDiffName(rawArgument);
                    break;
                case MAPPER:
                    argument.setMapper(rawArgument);
                    break;
                case AR:
                    argument.setAr(Double.valueOf(rawArgument));
                    break;
                case OD:
                    argument.setOd(Double.valueOf(rawArgument));
                    break;
                case CS:
                    argument.setCs(Double.valueOf(rawArgument));
                    break;
                case HP:
                    argument.setHp(Double.valueOf(rawArgument));
                    break;
                case MODS:
                    argument.setMods(Integer.valueOf(rawArgument));
                    break;
                case MODS_STRING:
                    argument.setModsString(rawArgument);
                    break;
                case COUNT_MISS:
                    argument.setCountMiss(Integer.valueOf(rawArgument));
                    break;
                case COUNT_100:
                    argument.setCount100(Integer.valueOf(rawArgument));
                    break;
                case COUNT_50:
                    argument.setCount50(Integer.valueOf(rawArgument));
                    break;
                case MAXCOMBO:
                    argument.setMaxCombo(Integer.valueOf(rawArgument));
                    break;
                case USERNAME_LIST:
                    //由于用户名列表是必须参数，不必加null判定（后续可选参数需要加判定）
                    String[] usernameList = rawArgument.split(",");
                    argument.setUsernames(Arrays.asList(usernameList));
                    break;
                case AT:

                    //由于!sudo smoke命令后面会自动插入一个空格，所以这里显然不能用冒号……如果使用了AT，得手动重解析一次参数
                    //后面的second放在第0号参数，然后继续手动解析（对用户使用习惯和历史遗留问题的妥协）
                    rawArgument = argumentList.get(0) + ":" + argumentList.get(2);

                    legalParamMatcher = CQCodePattern.AT.matcher(rawArgument);
                    if (legalParamMatcher.find()) {
                        //如果是艾特qq
                        if ("all".equals(legalParamMatcher.group(1))) {
                            //艾特全员改成-1
                            argument.setQQ(-1L);
                        } else {
                            argument.setQQ(Long.valueOf(legalParamMatcher.group(1)));
                        }
                    } else {
                        //也兼容直接输入qq
                        rawArgument = argumentList.get(0);
                        legalParamMatcher = RegularPattern.QQ.matcher(rawArgument);
                        if (legalParamMatcher.find()) {
                            argument.setQQ(Long.valueOf(rawArgument));
                        } else {
                            return String.format(TipConsts.FORMAT_ERROR, rawArgument, "QQ号");

                        }
                    }
                    break;
                case FLAG:
                    //懒得验证了
                    argument.setFlag(rawArgument);
                    break;
                case SHADOWSOCKS_USER:
                    ShadowSocksRequest ssr = new ShadowSocksRequest();
                    ssr.setUser(rawArgument);
                    argument.setSsr(ssr);
                    break;
                case SHADOWSOCKS_NUMBER:
                    ssr = argument.getSsr();
                    if (ssr == null) {
                        //getCode命令Number是第一个参数，所以此时ssr还是null。
                        ssr = new ShadowSocksRequest();
                    }
                    ssr.setNumber(Integer.valueOf(rawArgument.replaceAll("[gm]", "")));
                    switch (rawArgument.substring(rawArgument.length() - 1)) {
                        case "m":
                            ssr.setType("time");
                            ssr.setMonthly(1);
                            break;
                        case "g":
                            ssr.setType("traffic");
                            break;
                        default:
                            break;
                    }
                    argument.setSsr(ssr);
                    break;
                case BEATMAP_ID:
                    argument.setBeatmapId(Integer.valueOf(rawArgument));
                    break;
                case DAY:
                    legalParamMatcher = RegularPattern.DAY.matcher(rawArgument);
                    if (legalParamMatcher.find()) {
                        argument.setDay(Integer.valueOf(rawArgument));
                    } else {
                        return "假使这些完全……不能用的参数，你再给他传一遍，你等于……你也等于……你也有泽任吧？";
                    }
                    argument.setDay(day);
                    break;
                case HOUR:
                    Matcher sleepMatcher = RegularPattern.SLEEP_REGEX.matcher(rawArgument);
                    if (!sleepMatcher.find()) {
                        //sleep专用正则，sleep前面加东西不工作
                        return null;
                    }

                    Long hour;
                    try {
                        hour = Long.valueOf(rawArgument);
                    } catch (java.lang.Exception e) {
                        hour = 6L;
                    }
                    if (hour > 13) {
                        hour = 13L;
                    }
                    //TODO 化学式的禁言移到命令处理器
//                    if (hour == 0) {
//                        if (cqMsg.getQQ() == 546748348) {
//                            hour = 720L;
//                        } else {
//                            hour = 6L;
//                        }
//                    }

                    if (hour < 0) {
                        hour = 6L;
                    }
                    argument.setHour(hour);
                    //TODO 2018-4-12 15:42:29提高代码重用，判空并设为6小时移到命令处理器里去做
                    break;
                case NUM:

                    Matcher bpNumMatcher = RegularPattern.BPNUM.matcher(rawArgument);
                    if (bpNumMatcher.find()) {
                        num = Integer.valueOf(rawArgument);

                    } else {
                        switch (argument.getMessageSource()) {
                            case QQ:
                                return "[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("ay_ay_ay.wav")) + "]";
                            case DISCORD:
                                return "Ай-ай-ай-ай-ай, что сейчас произошло!";
                            default:
                                break;
                        }
                    }
                    argument.setNum(num);
                    break;

                case SECOND:
                    //我这个参数比较特殊（获取冒号后的部分，用空格分割）
                    String[] args = argumentList.get(2).split(" ");
                    if (args.length > 1) {
                        argument.setSecond(Integer.valueOf(args[1]));
                    } else {
                        argument.setSecond(600);
                    }
                    break;
                case GROUPID:
                    //群号和QQ试用同一个正则
                    legalParamMatcher = RegularPattern.QQ.matcher(rawArgument);
                    if (legalParamMatcher.find()) {
                        argument.setGroupId(Long.valueOf(rawArgument));
                    } else {
                        return String.format(TipConsts.FORMAT_ERROR, rawArgument, "群号");
                    }
                    break;

                case SHADOWSOCKS_CONFIRM:
                    ssr = argument.getSsr();
                    ssr.setConfirm(Integer.valueOf(rawArgument));
                    argument.setSsr(ssr);
                    break;
                case SHADOWSOCKS_COUNT:
                    ssr = argument.getSsr();
                    ssr.setCount(Integer.valueOf(rawArgument));
                    argument.setSsr(ssr);
                    break;
                default:
                    break;
            }
        }
        //此处编译器警告可以无视：因为只有在正则表达式不匹配的时候这个值才是null
        //统一处理彩蛋
        if (num <= 0 || num > 100) {
            return "其他人看不到的东西，白菜也看不到啦。";
        }
        if (day < 0) {
            return "白菜不会预知未来。";
        }
        if (LocalDate.now().minusDays(day).isBefore(LocalDate.of(2007, 9, 16))) {
            return "你要找史前时代的数据吗。";
        }
        if (Integer.valueOf(3).equals(argument.getUserId())) {
            return TipConsts.QUERY_BANCHO_BOT;
        }
        if ("banchobot".equals(argument.getUsername().toLowerCase())) {
            return TipConsts.QUERY_BANCHO_BOT;
        }
        //我不知道这里直接让它执行可不可行……
        return pjp.proceed();
    }


    private List<String> splitRegularParameter(String rawArgument) {
        List<String> result = new ArrayList<>(3);
        if ("".equals(rawArgument)) {
            //一个参数都没有给
            result.add(null);
            result.add(null);
            result.add(null);
            return result;
        } else {
            //因为正则表达式取出的是空字符串而不是null，所以需要一个if块
            //取出第一个（←重要！）井号和冒号的位置（兼容全半角冒号）
            int indexOfSharp = rawArgument.indexOf("#");
            int indexOfColon = -1;
            if (rawArgument.contains(":")) {
                indexOfColon = rawArgument.indexOf(":");
            }
            if (rawArgument.contains("：")) {
                indexOfColon = rawArgument.indexOf("：");
            }
            //2018-4-8 10:47:54从spilt改为手动切割，避免URL问题
            //首先，如果冒号井号都没有，那将全文视作参数0，直接返回；
            if (indexOfColon == -1 && indexOfSharp == -1) {
                result.add(rawArgument);
                return result;
            }
            //如果没有冒号
            if (indexOfColon == -1) {
                result.add(rawArgument.substring(0, indexOfSharp));
                result.add(rawArgument.substring(indexOfSharp + 1));
                result.add(null);
                //小知识：arraylist接受null值，并且会占集合的容量
            } else if (indexOfSharp == -1) {
                //如果没有井号
                result.add(rawArgument.substring(0, indexOfColon));
                result.add(null);
                result.add(rawArgument.substring(indexOfColon + 1));
            } else {
                //两个符号都有，就需要考虑参数的先后顺序问题了
                if (indexOfColon < indexOfSharp) {
                    //冒号在前
                    result.add(rawArgument.substring(0, indexOfColon));
                    result.add(rawArgument.substring(indexOfColon + 1, indexOfSharp));
                    result.add(rawArgument.substring(indexOfSharp + 1));
                } else {
                    result.add(rawArgument.substring(0, indexOfSharp));
                    result.add(rawArgument.substring(indexOfSharp + 1, indexOfColon));
                    result.add(rawArgument.substring(indexOfColon + 1));

                }

            }
            return result;
        }
    }

    private List<String> splitSearchParameter(String rawArgument) {
        ArrayList<String> list = new ArrayList<>(13);
        //改一改设计思路：参数处理和错误判断分开做
        Integer modsNum = null;
        String mods = "None";
        Integer mode;
        String keyword = null;
        String scoreString = null;
        Double ar = null;
        Double od = null;
        Double cs = null;
        Double hp = null;
        boolean keywordFound = false;
        //先从字符串结尾的mod开始检测
        Matcher getKeyWordAndMod = SearchKeywordPattern.MOD.matcher(rawArgument);
        if (getKeyWordAndMod.find()) {
            //如果包含了Mod
            keyword = getKeyWordAndMod.group(1);
            //标志找到了mod和关键字
            keywordFound = true;

            mods = getKeyWordAndMod.group(2);
            modsNum = scoreUtil.reverseConvertMod(mods);
            ///如果字符串解析出错，会返回null，因此这里用null值来判断输入格式 TODO 这块移到命令处理器里做，或者直接废除
//            if (modsNum == null) {
//                cqMsg.setMessage("请使用MOD的双字母缩写，不需要任何分隔符。" +
//                        "\n接受的Mod有：NF EZ TD HD HR SD DT HT NC FL SO PF。");
//                cqManager.sendMsg(cqMsg);
//                return null;
//            }
            //如果检测出来就去掉
            rawArgument = keyword;
        }
        //再检测是否指定了成绩字符串

        //兼容koohii 加默认值

        getKeyWordAndMod = SearchKeywordPattern.PP_CALC.matcher(rawArgument);
        if (getKeyWordAndMod.find()) {
            if (!keywordFound) {
                keyword = getKeyWordAndMod.group(2);
                keywordFound = true;
            }

            scoreString = getKeyWordAndMod.group(3);
            String[] scoreParams = scoreString.split(" ");
            for (String s : scoreParams) {
                Matcher getScoreParams = SearchKeywordPattern.KEYWORD_ACC.matcher(s);
                if (getScoreParams.find()) {
                    searchParam.setAcc(Double.valueOf(getScoreParams.group(1)));
                }
                getScoreParams = SearchKeywordPattern.KEYWORD_COMBO.matcher(s);
                if (getScoreParams.find()) {
                    searchParam.setMaxCombo(Integer.valueOf(getScoreParams.group(1)));
                }
                getScoreParams = SearchKeywordPattern.KEYWORD_COUNT_50.matcher(s);
                if (getScoreParams.find()) {
                    searchParam.setCount50(Integer.valueOf(getScoreParams.group(1)));
                }
                getScoreParams = SearchKeywordPattern.KEYWORD_COUNT_100.matcher(s);
                if (getScoreParams.find()) {
                    searchParam.setCount100(Integer.valueOf(getScoreParams.group(1)));
                }
                getScoreParams = SearchKeywordPattern.KEYWORD_MISS.matcher(s);
                if (getScoreParams.find()) {
                    searchParam.setCountMiss(Integer.valueOf(getScoreParams.group(1)));
                }
            }
            msg = msg.replace(scoreString, "");
            msg = msg.replaceAll("[《<>》]", "");
        }
        //最后检测是否指定了模式（如果先检测，会把后面的文字也计算进去）
        getKeyWordAndMod = SearchKeywordPattern.MODE.matcher(msg);
        if (getKeyWordAndMod.find()) {
            keyword = getKeyWordAndMod.group(2);
            keywordFound = true;
            mode = convertModeStrToInteger(getKeyWordAndMod.group(3));
            if (mode == null) {
                logger.debug(getKeyWordAndMod.group(3));
                cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, getKeyWordAndMod.group(3), "osu!游戏模式"));
                cqManager.sendMsg(cqMsg);
                return null;
            }
            argument.setMode(mode);
        } else {
            argument.setMode(0);
        }


        if (!keywordFound) {
            //这种情况，三个参数都没有指定
            Matcher m = RegularPattern.REG_CMD_REGEX.matcher(msg);
            m.find();
            if ("sudo".equals(argument.getSubCommandLowCase())) {
                m = RegularPattern.ADMIN_CMD_REGEX.matcher(msg);
                m.find();
            }
            keyword = m.group(2);
        }

        //如果mode不是主模式，而且命令是search
        if (!argument.getMode().equals(0) && "search".equals(argument.getSubCommandLowCase())) {
            cqMsg.setMessage("由于oppai不支持其他模式，因此白菜也只有主模式支持!search命令。");
            cqManager.sendMsg(cqMsg);
            return null;
        }

        searchParam.setMods(modsNum);
        searchParam.setModsString(mods);


        if (keyword.endsWith(" ")) {
            keyword = keyword.substring(0, keyword.length() - 1);
        }
        Matcher allNumberKeyword = SearchKeywordPattern.ALL_NUMBER_SEARCH_KEYWORD.matcher(keyword);
        if (allNumberKeyword.find()) {
            searchParam.setBeatmapId(Integer.valueOf(allNumberKeyword.group(1)));
            return searchParam;
        }
        //新格式(咕)

        //比较菜，手动补齐参数
        if (!keyword.contains("-")) {
            //如果没有横杠，手动补齐
            keyword = "-" + keyword;
        }
        if (!(keyword.endsWith("]") || keyword.endsWith(")") || keyword.endsWith("}")
                || keyword.endsWith("】") || keyword.endsWith("）")
        )) {
            //如果圆括号 方括号 花括号都没有
            keyword += "[](){}";
        }
        if (keyword.endsWith("]") || keyword.endsWith("】")) {
            //如果有方括号
            keyword += "(){}";
        }
        if (keyword.endsWith(")") || keyword.endsWith("）")) {
            //如果有圆括号
            keyword += "{}";
        }
        Matcher getArtistTitleEtc = SearchKeywordPattern.KETWORD.matcher(keyword);
        if (!getArtistTitleEtc.find()) {
            cqMsg.setMessage("搜索格式：艺术家-歌曲标题[难度名](麻婆名){AR9.0OD9.0CS9.0HP9.0}:osu!std<98acc 1x100 2x50 3xmiss 4cb> +MOD双字母简称。\n" +
                    "所有参数都可以省略(但横线、方括号和圆括号不能省略)，方括号 圆括号和四维的小数点支持全/半角；四维顺序必须按AR OD CS HP排列。");
            cqManager.sendMsg(cqMsg);
            return null;
        } else {
            //没啥办法……手动处理吧，这个正则管不了了，去掉可能存在的空格
            String artist = getArtistTitleEtc.group(1);
            if (artist.endsWith(" ")) {
                artist = artist.substring(0, artist.length() - 1);
            }
            String title = getArtistTitleEtc.group(2);
            if (title.startsWith(" ")) {
                title = title.substring(1);
            }
            if (title.endsWith(" ")) {
                title = title.substring(0, title.length() - 1);
            }
            searchParam.setArtist(artist);
            searchParam.setTitle(title);
            searchParam.setDiffName(getArtistTitleEtc.group(3));
            searchParam.setMapper(getArtistTitleEtc.group(4));
            //处理四维字符串
            String fourDimensions = getArtistTitleEtc.group(5);
            if (!"".equals(fourDimensions)) {
                Matcher getFourDimens = SearchKeywordPattern.FOUR_DIMENSIONS_REGEX.matcher(fourDimensions);
                getFourDimens.find();
                if (getFourDimens.group(1) != null) {
                    ar = Double.valueOf(getFourDimens.group(1));
                }
                if (getFourDimens.group(2) != null) {
                    od = Double.valueOf(getFourDimens.group(2));
                }
                if (getFourDimens.group(3) != null) {
                    cs = Double.valueOf(getFourDimens.group(3));
                }
                if (getFourDimens.group(4) != null) {
                    hp = Double.valueOf(getFourDimens.group(4));
                }

            }
            searchParam.setAr(ar);
            searchParam.setOd(od);
            searchParam.setCs(cs);
            searchParam.setHp(hp);
            return searchParam;
        }
    }

    private Integer convertModeStrToInteger(String mode) {
        switch (mode.toLowerCase(Locale.CHINA)) {
            case "0":
            case "std":
            case "standard":
            case "主模式":
            case "戳泡泡":
            case "屙屎":
            case "o!std":
            case "s":
            case "osu!std":
            case "泡泡":
                return 0;
            case "1":
            case "太鼓":
            case "taiko":
            case "o!taiko":
            case "t":
            case "打鼓":
                return 1;
            case "2":
            case "catch the beat":
            case "catchthebeat":
            case "ctb":
            case "接水果":
            case "接翔":
            case "fruit":
            case "艹他爸":
            case "c":
            case "接屎":
                return 2;
            case "3":
            case "osu!mania":
            case "mania":
            case "骂娘":
            case "钢琴":
            case "o!m":
            case "m":
            case "下落":
            case "下落式":
                return 3;
            default:
                return null;

        }

    }
}
