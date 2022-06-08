package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-08 21:07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVisitorCount {
    private String uv;
    private String time;
    private Integer count;
}
