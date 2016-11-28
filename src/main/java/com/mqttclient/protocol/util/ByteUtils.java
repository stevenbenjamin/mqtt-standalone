package com.mqttclient.protocol.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ByteUtils {
  public static final Charset UTF8 = StandardCharsets.UTF_8;
  private static Random random = new Random();

  public static String randomString(int size) {
    char[] chars = new char[size];
    for (int i = 0; i < size; i++) {
      chars[i] = (char) (random.nextInt(26) + 'a');
    }
    return new String(chars);
  }

  /**
   * Common format of int to bytes msb, lsb.Assumes a value that can fit in 2
   * bytes.
   */
  public static byte[] unsignedShortBytes(int i) {
    byte[] bytes = new byte[2];
    bytes[0] = (byte) (i >> 8);
    bytes[1] = (byte) (i & 0x000000FF);
    return bytes;
  }

  public static int fromUnsignedShortBytes(byte msb, byte lsb) {
    return 0x0000FFFF & ((msb << 8) | (lsb & 0xFF));
  }

  /** Bytes0preceded by 2-byte length field */
  public static byte[] toLengthPrefixedString(String s) {
    byte[] utf8bytes = s.getBytes(UTF8);
    byte[] ln = unsignedShortBytes((short) utf8bytes.length);
    return add(ln, utf8bytes);
  }

  public static byte[] encodeVariableLength(int i) {
    if (i < 0) return new byte[0];
    // optimizations for the simplest case
    if (i < 128) return new byte[] { (byte) i };
    byte[] bytes = (i < 16_384) ? new byte[2] : (i < 2_097_152 ? new byte[3] : new byte[4]);
    int idx = 0;
    do {
      int b = i % 128;
      i = i / 128;
      if (i > 0) {
        b |= 128;
      }
      bytes[idx++] = (byte) b;
    } while (idx < 4 && i > 0);
    return bytes;
  }

  public static byte[] add(byte[]... arrays) {
    byte[] out = new byte[0];
    for (byte[] in : arrays) {
      byte[] out2 = new byte[out.length + in.length];
      System.arraycopy(out, 0, out2, 0, out.length);
      System.arraycopy(in, 0, out2, out.length, in.length);
      out = out2;
    }
    return out;
  }

  public static byte[] add(int headerByte, byte[]... arrays) {
    int length = 1;
    for (byte[] in : arrays) {
      length += in.length;
    }
    byte[] out = new byte[length];
    out[0] = (byte) (0x00FF & headerByte);
    int next = 1;
    for (byte[] in : arrays) {
      System.arraycopy(in, 0, out, next, in.length);
      next += in.length;
    }
    return out;
  }
}
