package com.sakura.formly.model.vo.formgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "表单目录树节点")
public class FormCatalogNodeVo {

    @Schema(description = "节点ID")
    private Long id;

    @Schema(description = "节点类型：group/form")
    private String type;

    @Schema(description = "父级节点ID，根节点为空")
    private Long parentId;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "表单标识，仅表单节点有值")
    private String formKey;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间，仅分组节点有值")
    private LocalDateTime updatedAt;

    @Schema(description = "子节点")
    private List<FormCatalogNodeVo> children = new ArrayList<>();
}
