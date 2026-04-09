package com.sakura.formly.model.dto.formdefinition;

import com.sakura.formly.common.PageDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "表单定义分页查询请求")
public class FormDefinitionPageReq extends PageDto {

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "表单名称")
    private String name;

    @Schema(description = "表单标识")
    private String formKey;
}
