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
import com.sakura.formly.model.vo.formdefinition.FormDefinitionListVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionPersistVo;
import com.sakura.formly.model.vo.formdefinition.FormSimpleVo;
import com.sakura.formly.service.FormDefinitionService;
import com.sakura.formly.service.FormGroupService;
import com.sakura.formly.service.FormSubmissionService;
import com.sakura.formly.service.FormVersionService;

import java.time.LocalDateTime;
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

        // 3.填充table的json数据
        FormVersion currentVersion = getCurrentVersion(formDefinition.getCurrentVersionId());
        formDefinitionEditorVo.setCurrentSchema(getCurrentSchema(currentVersion));

        // 4.返回
        return formDefinitionEditorVo;
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

        // 2.创建版本记录
        FormVersion formVersion = createDraftVersion(formDefinition, formSchemaReq.getSchemaJson());

        // 3.更新表单指针
        updateDefinitionVersionPointers(formDefinition.getId(), formVersion.getId(), formDefinition.getPublishedVersionId());

        // 4.返回
        return buildPersistVo(formVersion, formVersion.getId(), formDefinition.getPublishedVersionId(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormDefinitionPersistVo publishFormSchema(Long id, FormSchemaReq formSchemaReq) {
        // 1.获取表单详情
        FormDefinition formDefinition = getFormDefinitionById(id);

        // 2.序列化JSON
        String nextSchemaJson = writeSchemaJson(formSchemaReq.getSchemaJson());

        // 3.获取当前版本详情
        FormVersion currentVersion = getCurrentVersion(formDefinition.getCurrentVersionId());

        // 4.是否已是最新发布版本
        if (isCurrentVersionPublished(formDefinition, currentVersion, nextSchemaJson)) {
            return buildPersistVo(currentVersion, formDefinition.getCurrentVersionId(), formDefinition.getPublishedVersionId(), true);
        }

        // 5.根据当前编辑器内容确定本次要发布的目标版本
        FormVersion targetVersion = resolvePublishTargetVersion(formDefinition, currentVersion, nextSchemaJson);

        // 6.将目标版本标记为已发布
        publishVersion(targetVersion);

        // 7.更新表单指针
        updateDefinitionVersionPointers(formDefinition.getId(), targetVersion.getId(), targetVersion.getId());

        // 8.返回
        return buildPersistVo(targetVersion, targetVersion.getId(), targetVersion.getId(), false);
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
     * @param currentVersion 当前版本
     * @return 表格结构
     */
    private Object getCurrentSchema(FormVersion currentVersion) {
        if (ObjectUtil.isNull(currentVersion) || StrUtil.isBlank(currentVersion.getSchemaJson())) {
            return null;
        }

        try {
            return objectMapper.readValue(currentVersion.getSchemaJson(), Object.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "当前 schema 解析失败");
        }
    }

    /**
     * 获取当前表单版本详情
     * @param currentVersionId 当前版本id
     * @return 表单版本详情
     */
    private FormVersion getCurrentVersion(Long currentVersionId) {
        if (ObjectUtil.isNull(currentVersionId)) {
            return null;
        }

        FormVersion formVersion = formVersionService.getById(currentVersionId);

        if (ObjectUtil.isNull(formVersion)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND_ERROR, "当前版本不存在");
        }

        return formVersion;
    }

    private FormVersion createDraftVersion(FormDefinition formDefinition, Object schemaJson) {
        return createDraftVersion(formDefinition, writeSchemaJson(schemaJson));
    }

    /**
     * 创建一个新的草稿版本。
     * 版本号始终基于当前表单的最大版本号递增，保存不会覆盖历史版本。
     *
     * @param formDefinition 表单定义
     * @param schemaJson 已序列化的 schema 内容
     * @return 新创建的草稿版本
     */
    private FormVersion createDraftVersion(FormDefinition formDefinition, String schemaJson) {
        FormVersion latestVersion = formVersionService.lambdaQuery()
                .eq(FormVersion::getFormId, formDefinition.getId())
                .orderByDesc(FormVersion::getVersionNo)
                .last("limit 1")
                .one();
        int latestVersionNo = ObjectUtil.isNull(latestVersion) ? 0 : latestVersion.getVersionNo();
        int nextVersionNo = latestVersionNo + 1;

        FormVersion formVersion = new FormVersion();
        formVersion.setFormId(formDefinition.getId());
        formVersion.setVersionNo(nextVersionNo);
        formVersion.setSchemaJson(schemaJson);
        formVersion.setIsPublished(0);
        formVersion.setPublishedAt(null);
        formVersionService.save(formVersion);

        return formVersion;
    }

    /**
     * 判断当前编辑器内容是否已经对应当前发布版本。
     * 只有“当前草稿存在、schema 未变化、且当前草稿本身就是发布版本”时才直接返回已发布结果。
     *
     * @param formDefinition 表单定义
     * @param currentVersion 当前草稿版本
     * @param nextSchemaJson 当前前端传入并序列化后的 schema
     * @return 是否已是最新发布版本
     */
    private boolean isCurrentVersionPublished(FormDefinition formDefinition, FormVersion currentVersion, String nextSchemaJson) {
        return ObjectUtil.isNotNull(currentVersion)
                && !hasSchemaChanged(currentVersion, nextSchemaJson)
                && ObjectUtil.equals(formDefinition.getCurrentVersionId(), formDefinition.getPublishedVersionId());
    }

    /**
     * 根据当前编辑器内容确定本次要发布的目标版本。
     * 规则只有两种：
     * 1. 没有草稿版本，或者 schema 相比当前草稿有变化：先创建新版本，再发布它。
     * 2. schema 未变化但当前草稿尚未发布：直接发布当前草稿版本。
     *
     * @param formDefinition 表单定义
     * @param currentVersion 当前草稿版本
     * @param nextSchemaJson 当前前端传入并序列化后的 schema
     * @return 本次应被发布的版本
     */
    private FormVersion resolvePublishTargetVersion(FormDefinition formDefinition, FormVersion currentVersion, String nextSchemaJson) {
        if (ObjectUtil.isNull(currentVersion) || hasSchemaChanged(currentVersion, nextSchemaJson)) {
            return createDraftVersion(formDefinition, nextSchemaJson);
        }

        return currentVersion;
    }

    /**
     * 当前前端 schema 与最新草稿 schema 是否发生变化。
     *
     * @param currentVersion 当前草稿版本
     * @param nextSchemaJson 当前前端传入并序列化后的 schema
     * @return 是否发生变化
     */
    private boolean hasSchemaChanged(FormVersion currentVersion, String nextSchemaJson) {
        return !StrUtil.equals(currentVersion.getSchemaJson(), nextSchemaJson);
    }

    /**
     * 将目标版本标记为已发布。
     * 这里除了落库，也会把内存中的版本对象同步到最新状态，便于后续直接复用该对象组装返回值。
     *
     * @param formVersion 目标版本
     */
    private void publishVersion(FormVersion formVersion) {
        LocalDateTime publishedAt = LocalDateTime.now();
        boolean success = formVersionService.lambdaUpdate()
                .eq(FormVersion::getId, formVersion.getId())
                .set(FormVersion::getIsPublished, 1)
                .set(FormVersion::getPublishedAt, publishedAt)
                .update();

        if (!success) {
            throw new BusinessException(ResultCodeEnum.UPDATE_ERROR, "发布版本失败");
        }

        formVersion.setIsPublished(1);
        formVersion.setPublishedAt(publishedAt);
    }

    /**
     * 更新表单指针
     * @param formId 表单id
     * @param currentVersionId 当前最新编辑版本id
     * @param publishedVersionId 当前发布版本id
     */
    private void updateDefinitionVersionPointers(Long formId, Long currentVersionId, Long publishedVersionId) {
        boolean success = lambdaUpdate()
                .eq(FormDefinition::getId, formId)
                .set(FormDefinition::getCurrentVersionId, currentVersionId)
                .set(FormDefinition::getPublishedVersionId, publishedVersionId)
                .update();

        if (!success) {
            throw new BusinessException(ResultCodeEnum.UPDATE_ERROR, "更新表单版本指针失败");
        }
    }

    /**
     * 组装保存/发布结果。
     *
     * @param formVersion 本次处理完成后的版本对象
     * @param currentVersionId 当前编辑版本指针
     * @param publishedVersionId 当前发布版本指针
     * @param alreadyPublished 当前是否本来就是最新发布版本
     * @return 前端需要的最小版本结果
     */
    private FormDefinitionPersistVo buildPersistVo(
            FormVersion formVersion,
            Long currentVersionId,
            Long publishedVersionId,
            boolean alreadyPublished
    ) {
        FormDefinitionPersistVo formDefinitionPersistVo = new FormDefinitionPersistVo();
        formDefinitionPersistVo.setCurrentVersionId(currentVersionId);
        formDefinitionPersistVo.setPublishedVersionId(publishedVersionId);
        formDefinitionPersistVo.setVersionNo(formVersion.getVersionNo());
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
