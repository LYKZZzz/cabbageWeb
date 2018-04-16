package top.mothership.cabbage.factory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.service.CommandHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 以前我一直觉得我用不着设计模式，if和switch进行逻辑判断足够了
 * 直到我写下了这个工厂类，然后Controller的代码从充满了switch case的200多行的方法变成现在的几十行
 */
@Component
public class CommandHandlerFactory implements ApplicationContextAware {
    private static Map<CommandIdentifier, CommandHandler> cache;

    /**
     * Build t.
     *
     * @param <T> the type parameter
     * @param ci  the ci
     * @return the t
     */
    @SuppressWarnings("unchecked")
    public static <T extends CommandHandler> T build(CommandIdentifier ci) {
        return (T) cache.get(ci);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //容器启动时，将所有实现CommandHandler的类全部装入缓存，并且用一个枚举来标明命令
        Map<String, CommandHandler> map = applicationContext.getBeansOfType(CommandHandler.class);
        //按照阿里代码规约指定大小
        cache = new HashMap<>(100);
        //支持别名，每个CommandHandler里定义的Identifier是一个List，在缓存中把List遍历
        for (CommandHandler ch : map.values()) {
            for (CommandIdentifier ci : ch.getIdentifier()) {
                cache.put(ci, ch);
            }
        }
    }
}
