package com.sakura.formly.model.dto.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建表单定义请求")
public class FormDefinitionCreateReq {

    @Schema(description = "所属分组ID")
    private Long groupId;

    @NotBlank(message = "表单名称不能为空")
    @Schema(description = "表单名称")
    private String name;

    @NotBlank(message = "表单标识不能为空")
    @Schema(description = "表单唯一业务标识")
    private String formKey;

    @Schema(description = "表单描述")
    private String description;

    @Min(value = 0, message = "排序值不能小于0")
    @Schema(description = "排序值")
    private Integer sort;
}
