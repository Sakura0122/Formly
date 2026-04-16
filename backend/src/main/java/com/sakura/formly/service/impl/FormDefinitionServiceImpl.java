package com.sakura.formly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakura.formly.common.PageVo;
import com.sakura.formly.common.ResultCodeEnum;
import com.sakura.formly.exception.BusinessException;
import com.sakura.formly.mapper.FormDefinitionMapper;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionCreateReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionPageReq;
import com.sakura.formly.model.dto.formdefinition.FormSchemaReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionUpdateReq;
import com.sakura.formly.model.entity.FormDefinition;
import com.sakura.formly.model.entity.FormVersion;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionEditorVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionFormVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionHistoryItemVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionListVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionPersistVo;
import com.sakura.formly.model.vo.formdefinition.FormSimpleVo;
import com.sakura.formly.service.FormDefinitionService;
import com.sakura.formly.service.FormGroupService;
import com.sakura.formly.service.FormSubmissionService;
import com.sakura.formly.service.FormVersionService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FormDefinitionServiceImpl extends ServiceImpl<FormDefinitionMapper, FormDefinition> implements FormDefinitionService {

    private final FormGroupService formGroupService;
    private final FormVersionService formVersionService;
    private final FormSubmissionService formSubmissionService;
    private final ObjectMapper objectMapper;

    @Override
    public Long createFormDefinition(FormDefinitionCreateReq formDefinitionCreateReq) {
        // 1.校验父级表单是否存在
        validateGroupId(formDefinitionCreateReq.getGroupId());

        // 2.校验表单标识是否唯一
        Long count = lambdaQuery()
                .eq(FormDefinition::getFormKey, formDefinitionCreateReq.getFormKey())
                .count();
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "表单标识已存在");
        }

        // 3.请求对象转换为实体对象
        FormDefinition formDefinition = BeanUtil.copyProperties(formDefinitionCreateReq, FormDefinition.class);

        // 4.保存
        save(formDefinition);

        // 5.返回id
        return formDefinition.getId();
    }

    @Override
    public void updateFormDefinition(Long id, FormDefinitionUpdateReq formDefinitionUpdateReq) {
        // 1.判断分组是否存在
        validateGroupId(formDefinitionUpdateReq.getGroupId());

        // 2.显式更新允许置空的字段，确保表单可以移动回根目录
        boolean success = lambdaUpdate()
                .eq(FormDefinition::getId, id)
                .set(FormDefinition::getGroupId, formDefinitionUpdateReq.getGroupId())
                .set(FormDefinition::getName, formDefinitionUpdateReq.getName())
                .set(FormDefinition::getDescription, formDefinitionUpdateReq.getDescription())
                .set(FormDefinition::getSort, formDefinitionUpdateReq.getSort())
                .update();
        if (!success) {
            throw new BusinessException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public FormDefinitionEditorVo getFormEditorDetail(Long id) {
        // 1.获取表单详情
        FormDefinition formDefinition = getFormDefinitionById(id);

        // 2.转VO
        FormDefinitionEditorVo formDefinitionEditorVo = BeanUtil.copyProperties(formDefinition, FormDefinitionEditorVo.class);

        // 3.填充schema
        formDefinitionEditorVo.setSchema(getCurrentSchema(formDefinition.getDraftSchemaJson()));

        // 4.获取版本详情
        FormVersion publishedVersion = getVersion(formDefinition.getPublishedVersionId());

        // 5.是否存在未发布草稿
        formDefinitionEditorVo.setHasUnpublishedDraft(hasUnpublishedDraft(formDefinition.getDraftSchemaJson(), publishedVersion));

        // 4.返回
        return formDefinitionEditorVo;
    }

    @Override
    public FormDefinitionFormVo getFormPreviewDetail(Long id) {
        // 1.获取表单详情
        FormDefinition formDefinition = getFormDefinitionById(id);

        // 2.转VO
        FormDefinitionFormVo formDefinitionFormVo = BeanUtil.copyProperties(formDefinition, FormDefinitionFormVo.class);

        // 3.获取版本详情
        FormVersion publishedVersion = getVersion(formDefinition.getPublishedVersionId());
        formDefinitionFormVo.setPublishedVersionNo(ObjectUtil.isNull(publishedVersion) ? null : publishedVersion.getVersionNo());
        formDefinitionFormVo.setSchema(getCurrentSchema(ObjectUtil.isNull(publishedVersion) ? null : publishedVersion.getSchemaJson()));
        return formDefinitionFormVo;
    }

    @Override
    public List<FormDefinitionHistoryItemVo> getFormHistory(Long id) {
        getFormDefinitionById(id);

        return formVersionService.lambdaQuery()
                .eq(FormVersion::getFormId, id)
                .orderByDesc(FormVersion::getVersionNo)
                .list()
                .stream()
                .map(formVersion -> {
                    FormDefinitionHistoryItemVo historyItemVo = new FormDefinitionHistoryItemVo();
                    historyItemVo.setId(formVersion.getId());
                    historyItemVo.setVersionNo(formVersion.getVersionNo());
                    historyItemVo.setSchema(getCurrentSchema(formVersion.getSchemaJson()));
                    historyItemVo.setCreatedAt(formVersion.getCreatedAt());
                    historyItemVo.setCreatedBy(formVersion.getCreatedBy());
                    return historyItemVo;
                })
                .toList();
    }

    @Override
    public PageVo<FormDefinitionListVo> pageFormDefinitions(FormDefinitionPageReq formDefinitionPageReq) {
        var page = lambdaQuery()
                .eq(ObjectUtil.isNotNull(formDefinitionPageReq.getGroupId()), FormDefinition::getGroupId, formDefinitionPageReq.getGroupId())
                .and(
                        StrUtil.isNotBlank(formDefinitionPageReq.getKeyword()), wrapper -> wrapper
                                .like(FormDefinition::getName, formDefinitionPageReq.getKeyword())
                                .or()
                                .like(FormDefinition::getFormKey, formDefinitionPageReq.getKeyword())
                )
                .page(formDefinitionPageReq.toMpPageDefaultSortByCreateTimeDesc());
        return PageVo.of(page, FormDefinitionListVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormDefinitionPersistVo saveFormSchema(Long id, FormSchemaReq formSchemaReq) {
        // 1.获取表单详情
        FormDefinition formDefinition = getFormDefinitionById(id);

        // 2.序列化JSON
        String nextSchemaJson = writeSchemaJson(formSchemaReq.getSchemaJson());

        // 3.获取当前发布版本详情
        FormVersion publishedVersion = getVersion(formDefinition.getPublishedVersionId());

        // 4.保存当前草稿
        String nextDraftSchemaJson = nextSchemaJson;

        // 5.更新表单草稿与发布指针
        updateDefinitionDraft(formDefinition.getId(), nextDraftSchemaJson, formDefinition.getPublishedVersionId());

        // 5.返回
        return buildPersistVo(formDefinition.getPublishedVersionId(), hasUnpublishedDraft(nextDraftSchemaJson, publishedVersion), null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormDefinitionPersistVo publishFormSchema(Long id, FormSchemaReq formSchemaReq) {
        // 1.获取表单详情
        FormDefinition formDefinition = getFormDefinitionById(id);

        // 2.序列化JSON
        String nextSchemaJson = writeSchemaJson(formSchemaReq.getSchemaJson());

        // 3.获取当前发布版本详情
        FormVersion publishedVersion = getVersion(formDefinition.getPublishedVersionId());

        // 4.是否已是最新发布版本
        if (ObjectUtil.isNotNull(publishedVersion) && !hasSchemaChanged(publishedVersion.getSchemaJson(), nextSchemaJson)) {
            updateDefinitionDraft(formDefinition.getId(), nextSchemaJson, formDefinition.getPublishedVersionId());
            return buildPersistVo(formDefinition.getPublishedVersionId(), false, publishedVersion.getVersionNo(), true);
        }

        // 5.创建新的发布版本
        FormVersion targetVersion = createPublishedVersion(formDefinition.getId(), nextSchemaJson);

        // 6.更新表单发布指针并保留本次草稿
        updateDefinitionDraft(formDefinition.getId(), nextSchemaJson, targetVersion.getId());

        // 7.返回
        return buildPersistVo(targetVersion.getId(), false, targetVersion.getVersionNo(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFormDefinition(Long id) {
        removeFormDefinitionsByIds(List.of(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByGroupId(Long groupId) {
        List<Long> formIds = lambdaQuery()
                .eq(FormDefinition::getGroupId, groupId)
                .list()
                .stream()
                .map(FormDefinition::getId)
                .toList();
        removeFormDefinitionsByIds(formIds);
    }

    @Override
    public List<FormSimpleVo> getFormDefinitionList() {
        List<FormDefinition> records = lambdaQuery()
                .orderByAsc(FormDefinition::getSort)
                .orderByAsc(FormDefinition::getCreatedAt)
                .list();
        return BeanUtil.copyToList(records, FormSimpleVo.class);
    }

    private FormDefinition getFormDefinitionById(Long id) {
        FormDefinition formDefinition = getById(id);
        if (ObjectUtil.isNull(formDefinition)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND_ERROR, "表单定义不存在");
        }
        return formDefinition;
    }

    /**
     * 获取当前版本的表格结构
     *
     * @param schemaJson schema 内容
     * @return 表格结构
     */
    private Object getCurrentSchema(String schemaJson) {
        if (StrUtil.isBlank(schemaJson)) {
            return null;
        }

        try {
            return objectMapper.readValue(schemaJson, Object.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "当前 schema 解析失败");
        }
    }

    /**
     * 获取表单版本详情
     *
     * @param versionId 版本id
     * @return 表单版本详情
     */
    private FormVersion getVersion(Long versionId) {
        if (ObjectUtil.isNull(versionId)) {
            return null;
        }

        FormVersion formVersion = formVersionService.getById(versionId);

        if (ObjectUtil.isNull(formVersion)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND_ERROR, "版本不存在");
        }

        return formVersion;
    }

    /**
     * 创建一个新的发布版本。
     * 版本号始终基于当前表单的最大版本号递增。
     *
     * @param formId     表单id
     * @param schemaJson 已序列化的 schema 内容
     * @return 新创建的发布版本
     */
    private FormVersion createPublishedVersion(Long formId, String schemaJson) {
        FormVersion latestVersion = formVersionService.lambdaQuery()
                .eq(FormVersion::getFormId, formId)
                .orderByDesc(FormVersion::getVersionNo)
                .last("limit 1")
                .one();
        int latestVersionNo = ObjectUtil.isNull(latestVersion) ? 0 : latestVersion.getVersionNo();
        int nextVersionNo = latestVersionNo + 1;

        FormVersion formVersion = new FormVersion();
        formVersion.setFormId(formId);
        formVersion.setVersionNo(nextVersionNo);
        formVersion.setSchemaJson(schemaJson);
        formVersionService.save(formVersion);

        return formVersion;
    }

    /**
     * 当前前端 schema 与目标 schema 是否发生变化。
     *
     * @param currentSchemaJson 当前 schema
     * @param nextSchemaJson    当前前端传入并序列化后的 schema
     * @return 是否发生变化
     */
    private boolean hasSchemaChanged(String currentSchemaJson, String nextSchemaJson) {
        return !StrUtil.equals(currentSchemaJson, nextSchemaJson);
    }

    /**
     * 当前表单是否存在未发布草稿。
     *
     * @param draftSchemaJson  草稿 schema
     * @param publishedVersion 当前发布版本
     * @return 是否存在未发布草稿
     */
    private boolean hasUnpublishedDraft(String draftSchemaJson, FormVersion publishedVersion) {
        if (StrUtil.isBlank(draftSchemaJson)) {
            return false;
        }

        if (ObjectUtil.isNull(publishedVersion)) {
            return true;
        }

        return hasSchemaChanged(publishedVersion.getSchemaJson(), draftSchemaJson);
    }

    /**
     * 更新表单草稿与发布指针
     *
     * @param formId             表单id
     * @param draftSchemaJson    当前草稿schema
     * @param publishedVersionId 当前发布版本id
     */
    private void updateDefinitionDraft(Long formId, String draftSchemaJson, Long publishedVersionId) {
        boolean success = lambdaUpdate()
                .eq(FormDefinition::getId, formId)
                .set(FormDefinition::getDraftSchemaJson, draftSchemaJson)
                .set(FormDefinition::getPublishedVersionId, publishedVersionId)
                .update();

        if (!success) {
            throw new BusinessException(ResultCodeEnum.UPDATE_ERROR, "更新表单草稿失败");
        }
    }

    /**
     * 组装保存/发布结果。
     *
     * @param publishedVersionId  当前发布版本指针
     * @param hasUnpublishedDraft 是否存在未发布草稿
     * @param versionNo           本次发布版本号
     * @param alreadyPublished    当前是否本来就是最新发布版本
     * @return 前端需要的最小版本结果
     */
    private FormDefinitionPersistVo buildPersistVo(
            Long publishedVersionId,
            boolean hasUnpublishedDraft,
            Integer versionNo,
            boolean alreadyPublished
    ) {
        FormDefinitionPersistVo formDefinitionPersistVo = new FormDefinitionPersistVo();
        formDefinitionPersistVo.setPublishedVersionId(publishedVersionId);
        formDefinitionPersistVo.setHasUnpublishedDraft(hasUnpublishedDraft);
        formDefinitionPersistVo.setVersionNo(versionNo);
        formDefinitionPersistVo.setAlreadyPublished(alreadyPublished);
        return formDefinitionPersistVo;
    }

    private String writeSchemaJson(Object schemaJson) {
        try {
            return objectMapper.writeValueAsString(schemaJson);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "schemaJson 序列化失败");
        }
    }

    private void validateGroupId(Long groupId) {
        if (ObjectUtil.isNull(groupId)) {
            return;
        }
        formGroupService.getGroup(groupId);
    }

    /**
     * 删除表单
     *
     * @param formIds 表单id集合
     */
    private void removeFormDefinitionsByIds(List<Long> formIds) {
        if (CollUtil.isEmpty(formIds)) {
            return;
        }

        // 1.删除版本
        formVersionService.removeByFormIds(formIds);

        // 2.删除记录
        formSubmissionService.removeByFormIds(formIds);

        // 3.删除表单
        removeByIds(formIds);
    }
}
