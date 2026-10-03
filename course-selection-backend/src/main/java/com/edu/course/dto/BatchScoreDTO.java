package com.edu.course.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量成绩录入DTO
 */
@Data
public class BatchScoreDTO {
    private List<ScoreUpdateDTO> scores;
}
