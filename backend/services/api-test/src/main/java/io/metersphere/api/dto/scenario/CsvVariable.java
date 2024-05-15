package io.metersphere.api.dto.scenario;

import io.metersphere.sdk.constants.ValueEnum;
import io.metersphere.system.valid.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: jianxing
 * @CreateTime: 2024-01-12  10:11
 */
@Data
public class CsvVariable {

    @Schema(description = "id")
    @NotBlank
    @Size(max = 50)
    private String id;

    @Schema(description = "文件id/引用文件id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "{api_scenario_csv.file_id.length_range}")
    @NotBlank
    private String fileId;

    @Schema(description = "文件名称")
    @NotBlank
    private String fileName;

    @Schema(description = "是否是关联文件")
    private Boolean association = false;

    @Schema(description = "csv变量名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "{api_scenario_csv.name.length_range}")
    private String name;

    /**
     * @see CsvVariableScope
     */
    @Schema(description = "作用域 SCENARIO/STEP")
    @NotBlank(message = "{api_scenario_csv.scope.not_blank}")
    @Size(min = 1, max = 50, message = "{api_scenario_csv.scope.length_range}")
    @EnumValue(enumClass = CsvVariableScope.class)
    private String scope = CsvVariableScope.SCENARIO.name();

    @Schema(description = "启用/禁用")
    private Boolean enable = true;

    /**
     * 文件编码
     * @see CsvEncodingType
     */
    @Schema(description = "文件编码 UTF-8/UTF-16/ISO-8859-15/US-ASCII")
    @Size(max = 50, message = "{api_scenario_csv.encoding.length_range}")
    @EnumValue(enumClass = CsvEncodingType.class)
    private String encoding = CsvEncodingType.UTF8.getValue();

    @Schema(description = "是否随机")
    private Boolean random = false;

    @Schema(description = "变量名称(英文逗号间隔)")
    @Size(max = 255, message = "{api_scenario_csv.variable_names.length_range}")
    private String variableNames;

    @Schema(description = "忽略首行(只有在设置了变量名称后才生效)")
    private Boolean ignoreFirstLine = false;

    @Schema(description = "分隔符")
    @Size(max = 50, message = "{api_scenario_csv.delimiter.length_range}")
    private String delimiter;

    @Schema(description = "是否允许带引号")
    private Boolean allowQuotedData = false;

    @Schema(description = "遇到文件结束符再次循环")
    private Boolean recycleOnEof = true;

    @Schema(description = "遇到文件结束符停止线程")
    private Boolean stopThreadOnEof = false;


    public enum CsvEncodingType implements ValueEnum {
        UTF8("UTF-8"), UFT16("UTF-16"), ISO885915("ISO-8859-15"), US_ASCII("US-ASCII");
        private String value;

        CsvEncodingType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public enum CsvVariableScope {
        /**
         * 场景级：执行场景前加载CSV，当前场景任意步骤均可从CSV中读取到数据
         */
        SCENARIO,
        /**
         * 步骤级：需在测试步骤的“更多操作”中指定使用添加该CSV，执行该步骤时加载CSV，作用域为步骤内的请求
         */
        STEP
    }
}
