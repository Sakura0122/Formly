package com.sakura.formly.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    private static final String SYSTEM_OPERATOR = "1";

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdBy", String.class, SYSTEM_OPERATOR);
        strictInsertFill(metaObject, "updatedBy", String.class, SYSTEM_OPERATOR);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedBy", String.class, SYSTEM_OPERATOR);
    }
}
