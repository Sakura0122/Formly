package com.sakura.formly.model.dto.formdefinition;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
@Schema(description = "粘贴内容解析请求")
public class FormDefinitionPasteParseReq {

    @Schema(description = "剪贴板中的 HTML 内容")
    private String documentHtml;

    @Schema(description = "剪贴板中的纯文本内容")
    private String plainText;

    @AssertTrue(message = "documentHtml 和 plainText 不能同时为空")
    public boolean isContentValid() {
        return StrUtil.isNotBlank(documentHtml) || StrUtil.isNotBlank(plainText);
    }
}
