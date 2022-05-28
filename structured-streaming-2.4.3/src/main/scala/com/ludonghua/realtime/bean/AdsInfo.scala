package com.ludonghua.realtime.bean

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-28 20:19
 */
case class AdsInfo(ts: Long,
                   timestamp: Timestamp,
                   dayString: String,
                   hmString: String,
                   area: String,
                   city: String,
                   userId: String,
                   adsId: String)
