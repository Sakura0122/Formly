package com.sakura.formly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakura.formly.common.ResultCodeEnum;
import com.sakura.formly.exception.BusinessException;
import com.sakura.formly.model.dto.formgroup.FormGroupCreateReq;
import com.sakura.formly.model.dto.formgroup.FormGroupUpdateReq;
import com.sakura.formly.model.entity.FormGroup;
import com.sakura.formly.mapper.FormGroupMapper;
import com.sakura.formly.model.vo.formgroup.FormCatalogTreeVo;
import com.sakura.formly.model.vo.formgroup.FormGroupDetailVo;
import com.sakura.formly.model.vo.formgroup.FormGroupTreeVo;
import com.sakura.formly.service.FormDefinitionService;
import com.sakura.formly.service.FormGroupService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author sakura
 * @description 针对表【form_group(表单分组)】的数据库操作Service实现
 * @createDate 2026-04-09 17:10:49
 */
@Service
@RequiredArgsConstructor
public class FormGroupServiceImpl extends ServiceImpl<FormGroupMapper, FormGroup> implements FormGroupService {

    private final ObjectProvider<FormDefinitionService> formDefinitionServiceProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(FormGroupCreateReq request) {
        validateParentGroup(request.getParentId());

        FormGroup formGroup = BeanUtil.copyProperties(request, FormGroup.class);
        save(formGroup);
        return formGroup.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long id, FormGroupUpdateReq request) {
        getGroupOrThrow(id);
        validateParentForUpdate(id, request.getParentId());

        FormGroup updateEntity = BeanUtil.copyProperties(request, FormGroup.class);
        updateEntity.setId(id);
        updateById(updateEntity);
    }

    @Override
    public FormGroupDetailVo getGroupDetail(Long id) {
        FormGroup formGroup = getGroupOrThrow(id);
        return BeanUtil.copyProperties(formGroup, FormGroupDetailVo.class);
    }

    @Override
    public FormCatalogTreeVo getGroupTree() {
        List<FormGroup> formGroups = lambdaQuery()
                .orderByAsc(FormGroup::getSort)
                .orderByAsc(FormGroup::getCreatedAt)
                .list();

        Map<Long, List<FormGroup>> childrenMap = new HashMap<>();
        for (FormGroup formGroup : formGroups) {
            childrenMap.computeIfAbsent(formGroup.getParentId(), key -> new ArrayList<>()).add(formGroup);
        }

        FormCatalogTreeVo treeVo = new FormCatalogTreeVo();
        treeVo.setGroups(buildGroupTree(childrenMap, null));
        treeVo.setRootForms(formDefinitionServiceProvider.getObject().listRootForms());
        return treeVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        validateGroupExists(id);

        long childCount = lambdaQuery().eq(FormGroup::getParentId, id).count();
        if (childCount > 0) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "分组下仍有子分组，无法删除");
        }

        formDefinitionServiceProvider.getObject().removeByGroupIdWithCascade(id);
        removeById(id);
    }

    @Override
    public void validateGroupExists(Long id) {
        getGroupOrThrow(id);
    }

    private List<FormGroupTreeVo> buildGroupTree(Map<Long, List<FormGroup>> childrenMap, Long parentId) {
        List<FormGroup> children = childrenMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        return children.stream()
                .sorted(Comparator.comparing(FormGroup::getSort).thenComparing(FormGroup::getCreatedAt))
                .map(group -> {
                    FormGroupTreeVo treeNode = BeanUtil.copyProperties(group, FormGroupTreeVo.class);
                    treeNode.setChildren(buildGroupTree(childrenMap, group.getId()));
                    return treeNode;
                })
                .toList();
    }

    private FormGroup getGroupOrThrow(Long id) {
        FormGroup formGroup = getById(id);
        if (ObjectUtil.isNull(formGroup)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND_ERROR, "分组不存在");
        }
        return formGroup;
    }

    private void validateParentGroup(Long parentId) {
        if (ObjectUtil.isNull(parentId)) {
            return;
        }
        getGroupOrThrow(parentId);
    }

    private void validateParentForUpdate(Long currentId, Long parentId) {
        if (ObjectUtil.isNull(parentId)) {
            return;
        }
        if (ObjectUtil.equal(currentId, parentId)) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "分组不能挂载到自身");
        }

        FormGroup parentGroup = getGroupOrThrow(parentId);
        while (ObjectUtil.isNotNull(parentGroup)) {
            if (ObjectUtil.equal(parentGroup.getId(), currentId)) {
                throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "分组不能挂载到自己的子分组下");
            }
            if (ObjectUtil.isNull(parentGroup.getParentId())) {
                break;
            }
            parentGroup = getById(parentGroup.getParentId());
        }
    }
}

