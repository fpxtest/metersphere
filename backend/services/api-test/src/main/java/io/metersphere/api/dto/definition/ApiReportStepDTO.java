package io.metersphere.api.dto.definition;

import io.metersphere.sdk.constants.ExecStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApiReportStepDTO {
    @Schema(description = "步骤id")
    private String stepId;

    @Schema(description = "报告id")
    private String reportId;

    @Schema(description = "步骤名称")
    private String name;

    @Schema(description = "序号")
    private Long sort;

    @Schema(description = "步骤类型/API/CASE等")
    private String stepType;

    @Schema(description = "结果状态")
    private String status = ExecStatus.PENDING.name();

    @Schema(description = "误报编号/误报状态独有")
    private String fakeCode;

    @Schema(description = "请求名称")
    private String requestName;

    @Schema(description = "请求耗时")
    private Long requestTime;

    @Schema(description = "请求响应码")
    private String code;

    @Schema(description = "响应内容大小")
    private Long responseSize;

    @Schema(description = "脚本标识")
    private String scriptIdentifier;
}
