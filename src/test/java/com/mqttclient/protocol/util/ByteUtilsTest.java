package com.mqttclient.protocol.util;

import org.junit.Assert;
import org.junit.Test;

public class ByteUtilsTest {
  @Test
  public void testEncodeLength() {
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(0), new byte[] { 0 });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(1), new byte[] { 1 });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(127), new byte[] { 127 });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(128), new byte[] { (byte) 128, 1 });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(16383), new byte[] { (byte) 0xFF, (byte) 0x7F });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(16_384),
        new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x01 });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(2_097_151), new byte[] { (byte) 0xFF, (byte) 0xFF,
        (byte) 0x7F });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(2_097_152), new byte[] { (byte) 0x80, (byte) 0x80,
        (byte) 0x80, (byte) 0x01 });
    Assert.assertArrayEquals(ByteUtils.encodeVariableLength(268_435_455), new byte[] { (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0x7F });
  }

  @Test
  public void testUnsignedShortEncoding() {
    for (int i = 0; i < 16000; i++) {
      byte[] bytes = ByteUtils.unsignedShortBytes(i);
      int out = ByteUtils.fromUnsignedShortBytes(bytes[0], bytes[1]);
      Assert.assertEquals(i, out);
    }
  }
}
