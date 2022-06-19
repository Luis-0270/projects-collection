package com.ludonghua.use.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author Luis
 * DATE 2022-06-09 23:42
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent {
    private Long userId;
    private String ip;
    private String eventType;
    private Long eventTime;
}

