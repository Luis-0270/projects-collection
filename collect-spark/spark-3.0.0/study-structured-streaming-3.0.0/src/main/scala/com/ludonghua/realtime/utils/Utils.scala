package com.ludonghua.realtime.utils

import java.math.BigInteger
import java.security.MessageDigest

object Utils {
  /**
    * 对字符串进行MD5加密
    *
    * @param input
    * @return
    */
  def generateHash(input: String): String = {
    try {
      if (input == null) {
        null
      }
      val md = MessageDigest.getInstance("MD5")
      md.update(input.getBytes());
      val digest = md.digest();
      val bi = new BigInteger(1, digest);
      var hashText = bi.toString(16);
      while (hashText.length() < 32) {
        hashText = "0" + hashText;
      }
      hashText
    } catch {
      case e: Exception => e.printStackTrace(); null
    }
  }
}
