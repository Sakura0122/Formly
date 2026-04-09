package com.sakura.formly.service;

import com.sakura.formly.model.entity.FormVersion;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @author sakura
* @description 针对表【form_version(表单历史版本)】的数据库操作Service
* @createDate 2026-04-09 17:10:49
*/
public interface FormVersionService extends IService<FormVersion> {

    void removeByFormIds(List<Long> formIds);
}
