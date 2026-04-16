package com.sakura.formly.model.vo.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "表单历史版本项")
public class FormDefinitionHistoryItemVo {

    @Schema(description = "版本ID")
    private Long id;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "版本 schema")
    private Object schema;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "创建人标识")
    private String createdBy;
}
