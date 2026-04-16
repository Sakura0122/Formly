package com.sakura.formly.model.vo.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "粘贴内容解析结果")
public class FormDefinitionPasteParseVo {

    @Schema(description = "解析后的编辑器 schema")
    private Object schemaJson;
}
