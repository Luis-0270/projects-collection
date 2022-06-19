package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-08 20:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageViewCount {
    private String pv;
    private String time;
    private Integer count;
}
