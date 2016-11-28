package com.mqttclient.protocol;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.mqttclient.protocol.util.ByteUtils;

public class ReadBufferTest {
  @Test
  public void testEncodeDecodeLength() {
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      // check all ranges
      int sample = random.nextInt(268_435_455);
      // add a byte because the read buffer auto-increments the first byte
      byte[] bytes = ByteUtils.add(0, ByteUtils.encodeVariableLength(sample));
      int back = new ReadBuffer(bytes).readLength();
      Assert.assertEquals("correctly encoded " + sample, back, sample);
      //
      sample = random.nextInt(2_097_152);
      bytes = ByteUtils.add(0, ByteUtils.encodeVariableLength(sample));
      back = new ReadBuffer(bytes).readLength();
      Assert.assertEquals("correctly encoded " + sample, back, sample);
      //
      sample = random.nextInt(16_384);
      bytes = ByteUtils.add(0, ByteUtils.encodeVariableLength(sample));
      back = new ReadBuffer(bytes).readLength();
      Assert.assertEquals("correctly encoded " + sample, back, sample);
      //
      sample = random.nextInt(128);
      bytes = ByteUtils.add(0, ByteUtils.encodeVariableLength(sample));
      back = new ReadBuffer(bytes).readLength();
      Assert.assertEquals("correctly encoded " + sample, back, sample);
    }
  }
}
