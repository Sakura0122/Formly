package com.sakura.formly.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakura.formly.mapper.FormSubmissionMapper;
import com.sakura.formly.model.entity.FormSubmission;
import com.sakura.formly.service.FormSubmissionService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
* @author sakura
* @description 针对表【form_submission(表单提交记录)】的数据库操作Service实现
* @createDate 2026-04-09 17:10:49
*/
@Service
public class FormSubmissionServiceImpl extends ServiceImpl<FormSubmissionMapper, FormSubmission>
    implements FormSubmissionService{

    @Override
    public void removeByFormIds(List<Long> formIds) {
        if (CollUtil.isEmpty(formIds)) {
            return;
        }
        remove(new LambdaQueryWrapper<FormSubmission>().in(FormSubmission::getFormId, formIds));
    }
}




