package com.sakura.formly.model.vo.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "表单简要信息")
public class FormSimpleVo {

    @Schema(description = "表单ID")
    private Long id;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "表单名称")
    private String name;

    @Schema(description = "表单标识")
    private String formKey;

    @Schema(description = "排序值")
    private Integer sort;
}
