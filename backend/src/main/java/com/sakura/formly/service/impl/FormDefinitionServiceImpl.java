package com.sakura.formly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
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

@Service
@RequiredArgsConstructor
public class FormDefinitionServiceImpl extends ServiceImpl<FormDefinitionMapper, FormDefinition> implements FormDefinitionService {

    private final FormGroupService formGroupService;
    private final FormVersionService formVersionService;
    private final FormSubmissionService formSubmissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDefinition(FormDefinitionCreateReq request) {
        validateGroupId(request.getGroupId());
        validateFormKeyUnique(request.getFormKey(), null);

        FormDefinition formDefinition = BeanUtil.copyProperties(request, FormDefinition.class);
        save(formDefinition);
        return formDefinition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefinition(Long id, FormDefinitionUpdateReq request) {
        FormDefinition existingDefinition = getDefinition(id);
        validateGroupId(request.getGroupId());
        validateFormKeyUnique(request.getFormKey(), id);

        FormDefinition formDefinition = BeanUtil.copyProperties(request, FormDefinition.class);
        formDefinition.setId(id);
        formDefinition.setCurrentVersionId(existingDefinition.getCurrentVersionId());
        formDefinition.setPublishedVersionId(existingDefinition.getPublishedVersionId());
        updateById(formDefinition);
    }

    @Override
    public FormDefinitionDetailVo getDefinitionDetail(Long id) {
        return BeanUtil.copyProperties(getDefinition(id), FormDefinitionDetailVo.class);
    }

    @Override
    public PageVo<FormDefinitionListVo> pageDefinitions(FormDefinitionPageReq request) {
        var page = lambdaQuery()
                .eq(ObjectUtil.isNotNull(request.getGroupId()), FormDefinition::getGroupId, request.getGroupId())
                .like(ObjectUtil.isNotEmpty(request.getName()), FormDefinition::getName, request.getName())
                .like(ObjectUtil.isNotEmpty(request.getFormKey()), FormDefinition::getFormKey, request.getFormKey())
                .page(request.toMpPage(
                        new OrderItem().setColumn("sort").setAsc(true),
                        new OrderItem().setColumn("created_at").setAsc(true)
                ));
        return PageVo.of(page, FormDefinitionListVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(Long id) {
        getDefinition(id);
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
    public List<FormSimpleVo> listCatalogForms() {
        List<FormDefinition> records = lambdaQuery()
                .orderByAsc(FormDefinition::getSort)
                .orderByAsc(FormDefinition::getCreatedAt)
                .list();
        return BeanUtil.copyToList(records, FormSimpleVo.class);
    }

    private FormDefinition getDefinition(Long id) {
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
        formGroupService.getGroup(groupId);
    }

    private void validateFormKeyUnique(String formKey, Long excludeId) {
        long count = lambdaQuery()
                .eq(FormDefinition::getFormKey, formKey)
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
