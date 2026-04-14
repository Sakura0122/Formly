package com.sakura.formly.model.vo.formdefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "表单定义详情")
public class FormDefinitionDetailVo {

    @Schema(description = "表单ID")
    private Long id;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "表单名称")
    private String name;

    @Schema(description = "表单标识")
    private String formKey;

    @Schema(description = "表单描述")
    private String description;

    @Schema(description = "当前编辑版本ID")
    private Long currentVersionId;

    @Schema(description = "已发布版本ID")
    private Long publishedVersionId;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
