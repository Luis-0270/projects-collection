package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-08 22:51
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCount {
    private Long item;
    private String time;
    private Integer count;
}
