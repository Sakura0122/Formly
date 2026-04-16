package com.sakura.formly.controller;

import com.sakura.formly.common.PageVo;
import com.sakura.formly.common.Result;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionCreateReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionPageReq;
import com.sakura.formly.model.dto.formdefinition.FormSchemaReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionUpdateReq;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionEditorVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionFormVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionHistoryItemVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionListVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionPersistVo;
import com.sakura.formly.service.FormDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

    @Operation(summary = "查询表单编辑器初始化数据")
    @GetMapping("/editor/{id}")
    public Result<FormDefinitionEditorVo> editor(@PathVariable Long id) {
        return Result.success(formDefinitionService.getFormEditorDetail(id));
    }

    @Operation(summary = "查询表单发布态预览数据")
    @GetMapping("/form/{id}")
    public Result<FormDefinitionFormVo> form(@PathVariable Long id) {
        return Result.success(formDefinitionService.getFormPreviewDetail(id));
    }

    @Operation(summary = "查询表单历史版本列表")
    @GetMapping("/{id}/history")
    public Result<List<FormDefinitionHistoryItemVo>> history(@PathVariable Long id) {
        return Result.success(formDefinitionService.getFormHistory(id));
    }

    @Operation(summary = "分页查询表单定义")
    @GetMapping("/page")
    public Result<PageVo<FormDefinitionListVo>> page(@Valid FormDefinitionPageReq formDefinitionPageReq) {
        return Result.success(formDefinitionService.pageFormDefinitions(formDefinitionPageReq));
    }

    @Operation(summary = "保存表单 schema")
    @PostMapping("/{id}/save")
    public Result<FormDefinitionPersistVo> save(
            @PathVariable Long id,
            @Valid @RequestBody FormSchemaReq formSchemaReq
    ) {
        return Result.success(formDefinitionService.saveFormSchema(id, formSchemaReq));
    }

    @Operation(summary = "保存并发布表单 schema")
    @PostMapping("/{id}/publish")
    public Result<FormDefinitionPersistVo> publish(
            @PathVariable Long id,
            @Valid @RequestBody FormSchemaReq formSchemaReq
    ) {
        return Result.success(formDefinitionService.publishFormSchema(id, formSchemaReq));
    }

    @Operation(summary = "删除表单定义")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        formDefinitionService.deleteFormDefinition(id);
        return Result.success();
    }
}
