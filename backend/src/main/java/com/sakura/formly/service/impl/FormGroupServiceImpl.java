package com.sakura.formly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakura.formly.common.ResultCodeEnum;
import com.sakura.formly.constant.FormCatalogNodeTypeConstant;
import com.sakura.formly.exception.BusinessException;
import com.sakura.formly.model.dto.formgroup.FormGroupCreateReq;
import com.sakura.formly.model.dto.formgroup.FormGroupUpdateReq;
import com.sakura.formly.model.entity.FormGroup;
import com.sakura.formly.mapper.FormGroupMapper;
import com.sakura.formly.model.vo.formdefinition.FormSimpleVo;
import com.sakura.formly.model.vo.formgroup.FormCatalogNodeVo;
import com.sakura.formly.model.vo.formgroup.FormGroupDetailVo;
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
    public Long createGroup(FormGroupCreateReq request) {
        // 1.判断父级分组是否存在
        if (ObjectUtil.isNotNull(request.getParentId())) {
            getGroup(request.getParentId());
        }

        // 2.请求对象转换为实体对象
        FormGroup formGroup = BeanUtil.copyProperties(request, FormGroup.class);

        // 3.保存
        save(formGroup);

        // 4.返回id
        return formGroup.getId();
    }

    @Override
    public void updateGroup(Long id, FormGroupUpdateReq request) {
        // 1.判断父级是否合法
        validateParentForUpdate(id, request.getParentId());


        // 2.请求对象转换为实体对象
        FormGroup formGroup = BeanUtil.copyProperties(request, FormGroup.class);
        formGroup.setId(id);

        // 3.更新
        boolean success = updateById(formGroup);
        if(!success){
            throw new BusinessException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public FormGroupDetailVo getGroupDetail(Long id) {
        return BeanUtil.copyProperties(getGroup(id), FormGroupDetailVo.class);
    }

    @Override
    public List<FormCatalogNodeVo> getGroupTree() {
        FormDefinitionService formDefinitionService = formDefinitionServiceProvider.getObject();

        // 1.获取全部分组
        List<FormGroup> formGroups = lambdaQuery()
                .orderByAsc(FormGroup::getSort)
                .orderByAsc(FormGroup::getCreatedAt)
                .list();

        // 2.获取全部表单
        List<FormSimpleVo> forms = formDefinitionService.getFormDefinitionList();

        // 3.按 parentId 对分组进行分组，把同一父节点下的子分组放到一个 List 里
        Map<Long, List<FormGroup>> groupChildrenMap = new HashMap<>();
        for (FormGroup formGroup : formGroups) {
            groupChildrenMap.computeIfAbsent(formGroup.getParentId(), key -> new ArrayList<>()).add(formGroup);
        }

        // 4.按 groupId 对表单进行分组，把同一父节点下的子表单放到一个 List 里
        Map<Long, List<FormSimpleVo>> formChildrenMap = new HashMap<>();
        for (FormSimpleVo form : forms) {
            formChildrenMap.computeIfAbsent(form.getGroupId(), key -> new ArrayList<>()).add(form);
        }

        // 5.构造树结构
        return buildCatalogTree(groupChildrenMap, formChildrenMap, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        // 1.判断是否有子分组
        Long childCount = lambdaQuery().eq(FormGroup::getParentId, id).count();
        if (childCount > 0) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "分组下仍有子分组，无法删除");
        }

        // 2.删除分组下的表单
        formDefinitionServiceProvider.getObject().removeByGroupId(id);

        // 3.删除分组
        removeById(id);
    }

    @Override
    public FormGroup getGroup(Long id) {
        FormGroup formGroup = getById(id);
        if (ObjectUtil.isNull(formGroup)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND_ERROR, "分组不存在");
        }
        return formGroup;
    }

    private List<FormCatalogNodeVo> buildCatalogTree(
            Map<Long, List<FormGroup>> groupChildrenMap,
            Map<Long, List<FormSimpleVo>> formChildrenMap,
            Long parentId
    ) {
        List<FormCatalogNodeVo> nodes = new ArrayList<>();

        List<FormGroup> childGroups = groupChildrenMap.get(parentId);
        if (CollUtil.isNotEmpty(childGroups)) {
            for (FormGroup childGroup : childGroups) {
                FormCatalogNodeVo node = BeanUtil.copyProperties(childGroup, FormCatalogNodeVo.class);
                node.setType(FormCatalogNodeTypeConstant.GROUP);
                node.setChildren(buildCatalogTree(groupChildrenMap, formChildrenMap, childGroup.getId()));
                nodes.add(node);
            }
        }

        List<FormSimpleVo> childForms = formChildrenMap.get(parentId);
        if (CollUtil.isNotEmpty(childForms)) {
            for (FormSimpleVo childForm : childForms) {
                FormCatalogNodeVo node = BeanUtil.copyProperties(childForm, FormCatalogNodeVo.class);
                node.setType(FormCatalogNodeTypeConstant.FORM);
                node.setParentId(childForm.getGroupId());
                nodes.add(node);
            }
        }

        nodes.sort(
                Comparator.comparing(FormCatalogNodeVo::getSort, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(FormCatalogNodeVo::getCreatedAt, Comparator.nullsFirst(java.time.LocalDateTime::compareTo))
                        .thenComparing(node -> FormCatalogNodeTypeConstant.GROUP.equals(node.getType()) ? 0 : 1)
        );
        return nodes;
    }

    /**
     * 验证更新时的父分组是否合法
     *
     * @param currentId 当前分组ID
     * @param parentId  父分组ID
     */
    private void validateParentForUpdate(Long currentId, Long parentId) {
        if (ObjectUtil.isNull(parentId)) {
            return;
        }
        if (ObjectUtil.equal(currentId, parentId)) {
            throw new BusinessException(ResultCodeEnum.PARAMS_ERROR, "分组不能挂载到自身");
        }

        FormGroup parentGroup = getGroup(parentId);
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
