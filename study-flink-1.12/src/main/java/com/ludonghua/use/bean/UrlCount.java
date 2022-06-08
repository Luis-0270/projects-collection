package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-08 23:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlCount {
    private String url;
    private Long windowEnd;
    private Integer count;
}
