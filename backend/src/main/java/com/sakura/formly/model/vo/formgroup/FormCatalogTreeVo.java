package com.sakura.formly.model.vo.formgroup;

import com.sakura.formly.model.vo.formdefinition.FormSimpleVo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "表单目录树")
public class FormCatalogTreeVo {

    @Schema(description = "分组树")
    private List<FormGroupTreeVo> groups = new ArrayList<>();

    @Schema(description = "根级表单")
    private List<FormSimpleVo> rootForms = new ArrayList<>();
}
