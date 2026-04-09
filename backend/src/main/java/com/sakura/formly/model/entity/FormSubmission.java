package com.sakura.formly.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 表单提交记录
 * @TableName form_submission
 */
@TableName(value ="form_submission")
@Data
public class FormSubmission {
    /**
     * 提交记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 表单ID
     */
    private Long formId;

    /**
     * 提交时使用的发布版本ID
     */
    private Long formVersionId;

    /**
     * 提交人标识
     */
    private String submitter;

    /**
     * 提交状态
     */
    private String submitStatus;

    /**
     * 整份表单填写数据
     */
    private Object dataJson;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除时间
     */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
