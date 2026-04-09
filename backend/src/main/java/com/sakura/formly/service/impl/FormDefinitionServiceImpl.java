package com.sakura.formly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakura.formly.common.PageVo;
import com.sakura.formly.common.ResultCodeEnum;
import com.sakura.formly.exception.BusinessException;
import com.sakura.formly.mapper.FormDefinitionMapper;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionCreateReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionPageReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionUpdateReq;
import com.sakura.formly.model.entity.FormDefinition;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionDetailVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionListVo;
import com.sakura.formly.model.vo.formdefinition.FormSimpleVo;
import com.sakura.formly.service.FormDefinitionService;
import com.sakura.formly.service.FormGroupService;
import com.sakura.formly.service.FormSubmissionService;
import com.sakura.formly.service.FormVersionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author sakura
* @description 针对表【form_definition(表单定义)】的数据库操作Service实现
* @createDate 2026-04-09 17:10:49
*/
@Service
@RequiredArgsConstructor
public class FormDefinitionServiceImpl extends ServiceImpl<FormDefinitionMapper, FormDefinition>
    implements FormDefinitionService{

    private static final String SYSTEM_OPERATOR = "";

    private final FormGroupService formGroupService;
    private final FormVersionService formVersionService;
    private final FormSubmissionService formSubmissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDefinition(FormDefinitionCreateReq request) {
        validateGroupId(request.getGroupId());
        validateFormKeyUnique(request.getFormKey(), null);

        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setId(IdUtil.getSnowflakeNextId());
        formDefinition.setGroupId(request.getGroupId());
        formDefinition.setName(StrUtil.trim(request.getName()));
        formDefinition.setFormKey(StrUtil.trim(request.getFormKey()));
        formDefinition.setDescription(StrUtil.blankToDefault(request.getDescription(), ""));
        formDefinition.setSort(ObjectUtil.defaultIfNull(request.getSort(), 0));
        formDefinition.setCurrentVersionId(null);
        formDefinition.setPublishedVersionId(null);
        formDefinition.setCreatedBy(SYSTEM_OPERATOR);
        formDefinition.setUpdatedBy(SYSTEM_OPERATOR);
        save(formDefinition);
        return formDefinition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefinition(Long id, FormDefinitionUpdateReq request) {
        FormDefinition existingDefinition = getDefinitionOrThrow(id);
        validateGroupId(request.getGroupId());
        validateFormKeyUnique(request.getFormKey(), id);

        FormDefinition updateEntity = new FormDefinition();
        updateEntity.setId(id);
        updateEntity.setGroupId(request.getGroupId());
        updateEntity.setName(StrUtil.trim(request.getName()));
        updateEntity.setFormKey(StrUtil.trim(request.getFormKey()));
        updateEntity.setDescription(StrUtil.blankToDefault(request.getDescription(), ""));
        updateEntity.setSort(ObjectUtil.defaultIfNull(request.getSort(), 0));
        updateEntity.setCurrentVersionId(existingDefinition.getCurrentVersionId());
        updateEntity.setPublishedVersionId(existingDefinition.getPublishedVersionId());
        updateEntity.setUpdatedBy(SYSTEM_OPERATOR);
        updateById(updateEntity);
    }

    @Override
    public FormDefinitionDetailVo getDefinitionDetail(Long id) {
        FormDefinition formDefinition = getDefinitionOrThrow(id);
        return BeanUtil.copyProperties(formDefinition, FormDefinitionDetailVo.class);
    }

    @Override
    public PageVo<FormDefinitionListVo> pageDefinitions(FormDefinitionPageReq request) {
        var page = lambdaQuery()
            .eq(ObjectUtil.isNotNull(request.getGroupId()), FormDefinition::getGroupId, request.getGroupId())
            .like(StrUtil.isNotBlank(request.getName()), FormDefinition::getName, StrUtil.trim(request.getName()))
            .like(StrUtil.isNotBlank(request.getFormKey()), FormDefinition::getFormKey, StrUtil.trim(request.getFormKey()))
            .page(request.toMpPage(
                new OrderItem().setColumn("sort").setAsc(true),
                new OrderItem().setColumn("created_at").setAsc(true)
            ));
        return PageVo.of(page, FormDefinitionListVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(Long id) {
        getDefinitionOrThrow(id);
        removeDefinitionsWithCascade(List.of(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByGroupIdWithCascade(Long groupId) {
        List<Long> formIds = lambdaQuery()
            .eq(FormDefinition::getGroupId, groupId)
            .list()
            .stream()
            .map(FormDefinition::getId)
            .toList();
        removeDefinitionsWithCascade(formIds);
    }

    @Override
    public List<FormSimpleVo> listRootForms() {
        List<FormDefinition> records = lambdaQuery()
            .isNull(FormDefinition::getGroupId)
            .orderByAsc(FormDefinition::getSort)
            .orderByAsc(FormDefinition::getCreatedAt)
            .list();
        return BeanUtil.copyToList(records, FormSimpleVo.class);
    }

    private FormDefinition getDefinitionOrThrow(Long id) {
        FormDefinition formDefinition = getById(id);
        if (ObjectUtil.isNull(formDefinition)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND_ERROR, "表单定义不存在");
        }
        return formDefinition;
    }

    private void validateGroupId(Long groupId) {
        if (ObjectUtil.isNull(groupId)) {
            return;
        }
        formGroupService.validateGroupExists(groupId);
    }

    private void validateFormKeyUnique(String formKey, Long excludeId) {
        long count = lambdaQuery()
            .eq(FormDefinition::getFormKey, StrUtil.trim(formKey))
            .ne(ObjectUtil.isNotNull(excludeId), FormDefinition::getId, excludeId)
            .count();
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "表单标识已存在");
        }
    }

    private void removeDefinitionsWithCascade(List<Long> formIds) {
        if (CollUtil.isEmpty(formIds)) {
            return;
        }
        formVersionService.removeByFormIds(formIds);
        formSubmissionService.removeByFormIds(formIds);
        removeByIds(formIds);
    }
}




