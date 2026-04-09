package com.sakura.formly.model.vo.formgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "表单分组详情")
public class FormGroupDetailVo {

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
}
