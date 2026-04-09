package com.sakura.formly.model.vo.formgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "表单分组树节点")
public class FormGroupTreeVo {

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "父级分组ID")
    private Long parentId;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "子分组")
    private List<FormGroupTreeVo> children = new ArrayList<>();
}
