package com.sakura.formly.service;

import com.sakura.formly.model.entity.FormSubmission;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @author sakura
* @description 针对表【form_submission(表单提交记录)】的数据库操作Service
* @createDate 2026-04-09 17:10:49
*/
public interface FormSubmissionService extends IService<FormSubmission> {

    void removeByFormIds(List<Long> formIds);
}
