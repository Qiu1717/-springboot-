package com.edu.course.dto;

import lombok.Data;

/**
 * 成绩更新DTO
 */
@Data
public class ScoreUpdateDTO {

    /** 选课记录ID */
    private Integer selectionId;

    /** 分数 */
    private Float score;
}
