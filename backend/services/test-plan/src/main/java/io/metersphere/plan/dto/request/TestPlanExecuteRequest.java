package io.metersphere.plan.dto.request;

import io.metersphere.sdk.constants.ApiBatchRunMode;
import io.metersphere.sdk.constants.ApiExecuteRunMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestPlanExecuteRequest {

    @Schema(description = "执行ID")
    @NotBlank(message = "test_plan.not.exist")
    private String executeId;

    @Schema(description = "执行模式", allowableValues = {"SERIAL", "PARALLEL"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String runMode = ApiBatchRunMode.SERIAL.name();

    @Schema(description = "执行来源", allowableValues = {"JENKINS", "SCENARIO", "RUN"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String executionSource = ApiExecuteRunMode.RUN.name();

}
