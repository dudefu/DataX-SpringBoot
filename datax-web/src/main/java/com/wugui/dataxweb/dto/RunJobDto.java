package com.wugui.dataxweb.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用于启动任务接收的实体
 *
 * @author zhouhongfa@gz-yibo.com
 * @ClassName RunJobDto
 * @Version 1.0
 * @since 2019/6/27 16:12
 */
@Data
public class RunJobDto implements Serializable {

    private String jobJson;

    private Long jobConfigId;

    public String getJobJson() {
        return jobJson;
    }

    public void setJobJson(String jobJson) {
        this.jobJson = jobJson;
    }

    public Long getJobConfigId() {
        return jobConfigId;
    }

    public void setJobConfigId(Long jobConfigId) {
        this.jobConfigId = jobConfigId;
    }
}
