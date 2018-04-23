package top.mothership.cabbage.service.regular;

import org.springframework.stereotype.Component;
import top.mothership.cabbage.enums.CommandIdentifier;
import top.mothership.cabbage.enums.Parameters;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.Parameter;
import top.mothership.cabbage.service.CommandHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class SwitchMode implements CommandHandler {

    private static final List<Parameter> PARAMETER_LIST = Arrays.asList(
            Parameter.builder().index(0).required(true).type(Parameters.MODE).build());

    @Override
    public List<Parameter> getParameters() {
        return PARAMETER_LIST;
    }

    @Override
    public List<CommandIdentifier> getIdentifier() {
        return Collections.singletonList(CommandIdentifier.MODE);
    }

    @Override
    public String doService(Argument argument) {
        return null;
    }
}
