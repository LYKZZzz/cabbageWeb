package top.mothership.cabbage.pojo.coolq;

import lombok.Builder;
import lombok.Data;
import top.mothership.cabbage.enums.Parameters;

/**
 * 用于给命令处理器标注形参，字段有形参的位置、种类和是否必须
 */
@Data
@Builder

public class Parameter {
    private Integer index;
    private Parameters type;
    private boolean required;
}
