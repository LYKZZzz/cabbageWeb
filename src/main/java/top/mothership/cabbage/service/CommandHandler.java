package top.mothership.cabbage.service;

import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.Parameter;

import java.util.List;

/**
 * The interface Command handler.
 */
public interface CommandHandler {
    /**
     * 获取这个命令需要的形参列表。
     *
     * @return the parameters
     */
    List<Parameter> getParameters();

    /**
     * 获取这个命令处理器的标志，让CommandHandlerFactory生成
     *
     * @return the identifier
     */
    List<CommandIdentifier> getIdentifier();

    /**
     * 执行具体逻辑，并返回消息体
     *
     * @param argument 抽象出来的参数
     * @return 返回的消息
     */
    String doService(Argument argument);
}
