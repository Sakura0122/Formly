package com.sakura.formly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
    public FormDefinitionDetailVo getFormDefinitionDetail(Long id) {
        return BeanUtil.copyProperties(getFormDefinitionById(id), FormDefinitionDetailVo.class);
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
