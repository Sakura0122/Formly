package com.sakura.formly.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakura.formly.mapper.FormVersionMapper;
import com.sakura.formly.model.entity.FormVersion;
import com.sakura.formly.service.FormVersionService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
* @author sakura
* @description 针对表【form_version(表单历史版本)】的数据库操作Service实现
* @createDate 2026-04-09 17:10:49
*/
@Service
public class FormVersionServiceImpl extends ServiceImpl<FormVersionMapper, FormVersion>
    implements FormVersionService{

    @Override
    public void removeByFormIds(List<Long> formIds) {
        if (CollUtil.isEmpty(formIds)) {
            return;
        }
        remove(new LambdaQueryWrapper<FormVersion>().in(FormVersion::getFormId, formIds));
    }
}




