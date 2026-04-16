package com.sakura.formly.service;

import com.sakura.formly.model.entity.FormDefinition;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sakura.formly.common.PageVo;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionCreateReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionPageReq;
import com.sakura.formly.model.dto.formdefinition.FormSchemaReq;
import com.sakura.formly.model.dto.formdefinition.FormDefinitionUpdateReq;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionEditorVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionFormVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionHistoryItemVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionListVo;
import com.sakura.formly.model.vo.formdefinition.FormDefinitionPersistVo;
import com.sakura.formly.model.vo.formdefinition.FormSimpleVo;

import java.util.List;

/**
* @author sakura
* @description 针对表【form_definition(表单定义)】的数据库操作Service
* @createDate 2026-04-09 17:10:49
*/
public interface FormDefinitionService extends IService<FormDefinition> {

    Long createFormDefinition(FormDefinitionCreateReq request);

    void updateFormDefinition(Long id, FormDefinitionUpdateReq request);

    FormDefinitionEditorVo getFormEditorDetail(Long id);

    FormDefinitionFormVo getFormPreviewDetail(Long id);

    List<FormDefinitionHistoryItemVo> getFormHistory(Long id);

    PageVo<FormDefinitionListVo> pageFormDefinitions(FormDefinitionPageReq request);

    FormDefinitionPersistVo saveFormSchema(Long id, FormSchemaReq request);

    FormDefinitionPersistVo publishFormSchema(Long id, FormSchemaReq request);

    void deleteFormDefinition(Long id);

    void removeByGroupId(Long groupId);

    List<FormSimpleVo> getFormDefinitionList();
}
