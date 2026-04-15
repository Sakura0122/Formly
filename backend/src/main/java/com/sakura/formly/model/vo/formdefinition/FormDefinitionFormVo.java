package com.sakura.formly.model.vo.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "表单发布态预览数据")
public class FormDefinitionFormVo {

    @Schema(description = "表单ID")
    private Long id;

    @Schema(description = "表单名称")
    private String name;

    @Schema(description = "表单标识")
    private String formKey;

    @Schema(description = "已发布版本号")
    private Integer publishedVersionNo;

    @Schema(description = "当前已发布 schema")
    private Object schema;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
