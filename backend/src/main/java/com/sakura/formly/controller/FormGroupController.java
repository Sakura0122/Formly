package com.sakura.formly.controller;

import com.sakura.formly.common.Result;
import com.sakura.formly.model.dto.formgroup.FormGroupCreateReq;
import com.sakura.formly.model.dto.formgroup.FormGroupUpdateReq;
import com.sakura.formly.model.vo.formgroup.FormCatalogTreeVo;
import com.sakura.formly.model.vo.formgroup.FormGroupDetailVo;
import com.sakura.formly.service.FormGroupService;
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

@Tag(name = "表单分组")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/form-groups")
public class FormGroupController {

    private final FormGroupService formGroupService;

    @Operation(summary = "创建表单分组")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FormGroupCreateReq request) {
        return Result.success(formGroupService.createGroup(request));
    }

    @Operation(summary = "更新表单分组")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FormGroupUpdateReq request) {
        formGroupService.updateGroup(id, request);
        return Result.success();
    }

    @Operation(summary = "查询表单分组详情")
    @GetMapping("/{id}")
    public Result<FormGroupDetailVo> detail(@PathVariable Long id) {
        return Result.success(formGroupService.getGroupDetail(id));
    }

    @Operation(summary = "查询表单目录树")
    @GetMapping("/tree")
    public Result<FormCatalogTreeVo> tree() {
        return Result.success(formGroupService.getGroupTree());
    }

    @Operation(summary = "删除表单分组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        formGroupService.deleteGroup(id);
        return Result.success();
    }
}
