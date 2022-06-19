package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-09 22:47
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdsCount {
    private String province;
    private Long windowEnd;
    private Integer count;
}
