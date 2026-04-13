package com.sakura.formly.model.dto.formgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建表单分组请求")
public class FormGroupCreateReq {

    @NotNull(message = "父级分组ID不能为空")
    @Schema(description = "父级分组ID")
    private Long parentId;

    @NotBlank(message = "分组名称不能为空")
    @Schema(description = "分组名称")
    private String name;

    @Min(value = 0, message = "排序值不能小于0")
    @Schema(description = "排序值")
    private Integer sort;
}
