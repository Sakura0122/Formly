package com.sakura.formly.service;

import com.sakura.formly.model.entity.FormGroup;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sakura.formly.model.dto.formgroup.FormGroupCreateReq;
import com.sakura.formly.model.dto.formgroup.FormGroupUpdateReq;
import com.sakura.formly.model.vo.formgroup.FormCatalogTreeVo;
import com.sakura.formly.model.vo.formgroup.FormGroupDetailVo;

/**
* @author sakura
* @description 针对表【form_group(表单分组)】的数据库操作Service
* @createDate 2026-04-09 17:10:49
*/
public interface FormGroupService extends IService<FormGroup> {

    Long createGroup(FormGroupCreateReq request);

    void updateGroup(Long id, FormGroupUpdateReq request);

    FormGroupDetailVo getGroupDetail(Long id);

    FormCatalogTreeVo getGroupTree();

    void deleteGroup(Long id);

    void validateGroupExists(Long id);
}
