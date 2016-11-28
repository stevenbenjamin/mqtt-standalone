package com.mqttclient.protocol;

import org.junit.Assert;
import org.junit.Test;

import com.mqttclient.protocol.CONNACK;
import com.mqttclient.protocol.PINGRESP;
import com.mqttclient.protocol.PUBACK;
import com.mqttclient.protocol.PUBCOMP;
import com.mqttclient.protocol.PUBLISH;
import com.mqttclient.protocol.PUBREC;
import com.mqttclient.protocol.PUBREL;
import com.mqttclient.protocol.SUBACK;
import com.mqttclient.protocol.ServerMessage;
import com.mqttclient.protocol.UNSUBACK;
import com.mqttclient.protocol.util.ByteUtils;

public class ServerMessageProtocolTest extends TestUtils {
  @Test
  public void testConnack() {
    CONNACK c = testServerMessage(new byte[] { 32, 2, 0, 4 }, CONNACK.class);
    Assert.assertEquals(c.returnCode, CONNACK.ReturnCode.BadUserNameOrPassword);
  }

  @Test
  public void testPublish() {
    int qos = 1;
    byte[] topicBytes = ByteUtils.toLengthPrefixedString("abc");
    // int messageId = randomUnsignedShort();
    // byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    byte[] payload = randomByteArray(100);
    byte[] lengthBytes = ByteUtils.encodeVariableLength(topicBytes.length + payload.length);
    byte[] raw = ByteUtils.add((32 | 16 | qos), lengthBytes, topicBytes, payload);
    PUBLISH p = testServerMessage(raw, PUBLISH.class);
    // Assert.assertEquals(messageId, p.messageId);
    Assert.assertEquals("abc", p.topic);
    Assert.assertArrayEquals(payload, p.payload);
  }

  @Test
  public void testPuback() {
    // anything with bit 7 of the header byte set
    for (int i = 0; i < 15; i++) {
      int messageId = randomUnsignedShort();
      byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
      PUBACK p = testServerMessage(new byte[] { (byte) (64 | i), 2, messageIdBytes[0], messageIdBytes[1] },
          PUBACK.class);
      Assert.assertEquals(p.messageId, messageId);
    }
  }

  @Test
  public void testPubrec() {
    for (int i = 0; i < 15; i++) {
      int messageId = randomUnsignedShort();
      byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
      PUBREC p = testServerMessage(new byte[] { (byte) (64 | 16 | i), 2, messageIdBytes[0], messageIdBytes[1] },
          PUBREC.class);
      Assert.assertEquals(p.messageId, messageId);
    }
  }

  @Test
  public void testPubrel() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    PUBREL p = testServerMessage(new byte[] { (byte) (64 | 32 | 2), 2, messageIdBytes[0], messageIdBytes[1] },
        PUBREL.class);
    Assert.assertEquals(p.messageId, messageId);
  }

  @Test
  public void testPubcomp() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    PUBCOMP p = testServerMessage(new byte[] { (byte) (64 | 32 | 16), 2, messageIdBytes[0], messageIdBytes[1] },
        PUBCOMP.class);
    Assert.assertEquals(p.messageId, messageId);
  }

  @Test
  public void testSuback() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    SUBACK p = testServerMessage(new byte[] { (byte) (128 | 16), 6, messageIdBytes[0], messageIdBytes[1], 0, 1, 2, 1 },
        SUBACK.class);
    Assert.assertEquals(p.messageId, messageId);
    Assert.assertArrayEquals(p.qosValues.toArray(), new Integer[] { 0, 1, 2, 1 });
  }

  @Test
  public void testUnsuback() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    UNSUBACK p = testServerMessage(new byte[] { (byte) (128 | 32 | 16), 2, messageIdBytes[0], messageIdBytes[1] },
        UNSUBACK.class);
    Assert.assertEquals(p.messageId, messageId);
  }

  @Test
  public void testPingresp() {
    testServerMessage(new byte[] { (byte) (128 | 64 | 16), 0 }, PINGRESP.class);
  }

  protected static <T extends ServerMessage> T testServerMessage(byte[] bytes, Class<T> klazz) {
    ServerMessage m = ServerMessage.readSingleMessage(bytes);
    Assert.assertTrue("Message instance of " + klazz, klazz.isInstance(m));
    return klazz.cast(m);
  }
}
