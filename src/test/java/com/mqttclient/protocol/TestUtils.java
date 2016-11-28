package com.mqttclient.protocol;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;

public class TestUtils {
  /** The number of variable length bytes this length will be encoded in. */
  public static int numVariableLengthBytes(int length) {
    if (length < 128) return 1;
    if (length < 16384) return 2;
    if (length < 2097152) return 3;
    if (length < 268435436) return 4;
    return -1;// Error
  }

  public static void trace(String s) {
    StringBuilder b = new StringBuilder("-------TRACE -----------\n");
    if (s != null) b.append(s + "\n");
    String prefix = "_";
    int ct = 0;
    for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
      prefix = prefix + "_";
      b.append(prefix + e.getClassName() + ':' + e.getMethodName() + ":" + e.getLineNumber() + "\n");
      if (ct++ > 8) break;
    }
    System.out.println(b);
  }

  protected static void debug(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      System.out.println(i + "==>" + bytes[i]);
    }
    System.out.println("------------");
  }

  public static Random random = new Random();

  protected static byte[] randomByteArray(int size) {
    byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return bytes;
  }

  public static byte[] slice(byte[] input, int startIndex, int endIndex) {
    byte[] out = new byte[endIndex - startIndex];
    System.arraycopy(input, startIndex, out, 0, out.length);
    return out;
  }

  public static void checkArrayEquals(byte[] a, byte[] b) {
    if (!Arrays.equals(a, b)) {
      System.out.println(Arrays.toString(a) + "\n" + Arrays.toString(b) + "\n-------");
    }
    Assert.assertArrayEquals(a, b);
  }

  public static int randomUnsignedShort() {
    return random.nextInt(65535);
  }

  public static void pause(int ms) {
    try {
      Thread.sleep(ms);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
