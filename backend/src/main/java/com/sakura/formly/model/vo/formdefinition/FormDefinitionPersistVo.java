package com.sakura.formly.model.vo.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "表单保存结果")
public class FormDefinitionPersistVo {

    @Schema(description = "已发布版本ID")
    private Long publishedVersionId;

    @Schema(description = "是否存在未发布草稿")
    private Boolean hasUnpublishedDraft;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "当前是否已是最新发布版本")
    private Boolean alreadyPublished;
}
