package com.sakura.formly.model.dto.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "表单 schema 请求")
public class FormSchemaReq {

    @NotNull(message = "schemaJson 不能为空")
    @Schema(description = "整份编辑器 schema")
    private Object schemaJson;
}
