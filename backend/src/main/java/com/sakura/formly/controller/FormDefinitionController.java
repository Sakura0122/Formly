package com.sakura.formly.controller;

import com.sakura.formly.common.PageVo;
import com.sakura.formly.common.Result;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionCreateReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionPageReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionUpdateReq;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionDetailVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionListVo;
import com.sakura.formly.service.FormDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "表单定义")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/form-definitions")
public class FormDefinitionController {

    private final FormDefinitionService formDefinitionService;

    @Operation(summary = "创建表单定义")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FormDefinitionCreateReq formDefinitionCreateReq) {
        return Result.success(formDefinitionService.createFormDefinition(formDefinitionCreateReq));
    }

    @Operation(summary = "更新表单定义")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FormDefinitionUpdateReq formDefinitionUpdateReq) {
        formDefinitionService.updateFormDefinition(id, formDefinitionUpdateReq);
        return Result.success();
    }

    @Operation(summary = "查询表单定义详情")
    @GetMapping("/{id}")
    public Result<FormDefinitionDetailVo> detail(@PathVariable Long id) {
        return Result.success(formDefinitionService.getFormDefinitionDetail(id));
    }

    @Operation(summary = "分页查询表单定义")
    @GetMapping("/page")
    public Result<PageVo<FormDefinitionListVo>> page(@Valid FormDefinitionPageReq formDefinitionPageReq) {
        return Result.success(formDefinitionService.pageFormDefinitions(formDefinitionPageReq));
    }

    @Operation(summary = "删除表单定义")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        formDefinitionService.deleteFormDefinition(id);
        return Result.success();
    }
}
