package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-08 12:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvgVc {
    private Integer vcSum;
    private Integer count;
}
