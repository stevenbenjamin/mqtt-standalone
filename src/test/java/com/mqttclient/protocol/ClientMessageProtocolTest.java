package com.mqttclient.protocol;

import static com.mqttclient.protocol.util.ByteUtils.randomString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mqttclient.protocol.util.ByteUtils;
import com.mqttclient.protocol.util.GrowableBuffer;
import com.mqttclient.protocol.util.Tuple;

// test connect 31: 311, client id truncat
public class ClientMessageProtocolTest extends TestUtils {
  @Test
  public void testConnect() {
    testConnectMessage(randomString(10), randomString(10), true, 1, randomString(23), randomString(23), true, 100,
        randomString(23));
    testConnectMessage(randomString(10), randomString(10), false, 2, randomString(23), randomString(23), false, 1000,
        randomString(23));
    testConnectMessage(null, null, false, 0, null, null, true, 0, "a");
    // works with null client id
    testConnectMessage(null, null, false, 0, null, null, true, 0, null);
  }

  @Test
  public void testSubscribe() {
    testSubscribeMessage(false, 199, new Tuple<>(randomString(23), 1), new Tuple<>(randomString(23), 2), new Tuple<>(
        randomString(1), 0), new Tuple<>(randomString(5), 2));
    testSubscribeMessage(true, 1099, new Tuple<>(randomString(23), 1), new Tuple<>(randomString(23), 2), new Tuple<>(
        randomString(1), 0), new Tuple<>(randomString(5), 2));
  }

  @Test
  public void testUnsubscribe() {
    int messageId = randomUnsignedShort();
    UNSUBSCRIBE u = new UNSUBSCRIBE(messageId, Arrays.asList(new String[] { "ABC", "DEF" }));
    GrowableBuffer b = new GrowableBuffer(10);
    byte[] topicBytes_a = ByteUtils.toLengthPrefixedString("ABC");
    byte[] topicBytes_b = ByteUtils.toLengthPrefixedString("DEF");
    int length = topicBytes_a.length + topicBytes_b.length + 2;
    b.putByte((byte) 162);
    b.putRaw(ByteUtils.encodeVariableLength(length));
    b.putRaw(ByteUtils.unsignedShortBytes(messageId));
    b.putRaw(topicBytes_a);
    b.putRaw(topicBytes_b);
    Assert.assertArrayEquals(u.createMessage(Version._31), b.getWrittenBytes());
    // test long message
    List<String> topics = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      topics.add(randomString(20));
    }
    u = new UNSUBSCRIBE(messageId, topics);
    b = new GrowableBuffer(10);
    length = 2;
    for (int i = 0; i < 100; i++) {
      byte[] topicBytes = ByteUtils.toLengthPrefixedString(topics.get(i));
      length += topicBytes.length;
    }
    b.putByte((byte) 162);
    b.putRaw(ByteUtils.encodeVariableLength(length));
    b.putRaw(ByteUtils.unsignedShortBytes(messageId));
    for (int i = 0; i < 100; i++) {
      b.putRaw(ByteUtils.toLengthPrefixedString(topics.get(i)));
    }
    Assert.assertArrayEquals(u.createMessage(Version._31), b.getWrittenBytes());
  }

  @Test
  public void testPingReq() {
    byte[] bytes = new PINGREQ().createMessage(Version._31);
    Assert.assertArrayEquals(bytes, new byte[] { (byte) 192, 0 });
  }

  @Test
  public void testDisconnect() {
    byte[] bytes = new DISCONNECT().createMessage(Version._31);
    Assert.assertArrayEquals(bytes, new byte[] { (byte) 224, 0 });
  }

  // --------------------------------------------
  // Bidirectional messages
  // --------------------------------------------
  @Test
  public void testPublish() {
    testPublishMessage(false, 0, false, 100, randomString(23), randomByteArray(100));
    testPublishMessage(false, 1, false, 100, randomString(23), randomByteArray(200));
    testPublishMessage(false, 2, false, 100, randomString(23), randomByteArray(400));
    testPublishMessage(true, 0, true, 100, randomString(23), randomByteArray(1000));
  }

  @Test
  public void testPubAck() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    PUBACK p = serverMessage(new byte[] { 64, 2, messageIdBytes[0], messageIdBytes[1] }, PUBACK.class);
    PUBACK assembled = new PUBACK(messageId);
    Assert.assertArrayEquals(p.createMessage(Version._31), assembled.createMessage(Version._31));
  }

  @Test
  public void testPubRec() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    PUBREC p = serverMessage(new byte[] { (byte) (64 | 16), 2, messageIdBytes[0], messageIdBytes[1] }, PUBREC.class);
    PUBREC assembled = new PUBREC(messageId);
    Assert.assertArrayEquals(p.createMessage(Version._31), assembled.createMessage(Version._31));
  }

  @Test
  public void testPubRel() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    PUBREL p = serverMessage(new byte[] { (byte) (64 | 32 | 2), 2, messageIdBytes[0], messageIdBytes[1] }, PUBREL.class);
    PUBREL assembled = new PUBREL(messageId);
    Assert.assertArrayEquals(p.createMessage(Version._31), assembled.createMessage(Version._31));
  }

  @Test
  public void testPubComp() {
    int messageId = randomUnsignedShort();
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    PUBCOMP p = serverMessage(new byte[] { (byte) (64 | 32 | 16), 2, messageIdBytes[0], messageIdBytes[1] },
        PUBCOMP.class);
    PUBCOMP assembled = new PUBCOMP(messageId);
    Assert.assertArrayEquals(p.createMessage(Version._31), assembled.createMessage(Version._31));
  }

  // --------------------------------------------
  // Support
  // --------------------------------------------
  protected static <T extends ServerMessage> T serverMessage(byte[] bytes, Class<T> klazz) {
    return klazz.cast(ServerMessage.readSingleMessage(bytes));
  }

  @SafeVarargs
  private static void testSubscribeMessage(boolean dup, int messageId, Tuple<String, Integer>... pairs) {
    SUBSCRIBE p = new SUBSCRIBE(messageId, dup, Arrays.asList(pairs));
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    GrowableBuffer topics = new GrowableBuffer(100);
    for (Tuple<String, Integer> t : pairs) {
      topics.putString(t.a);
      topics.putByte(t.b.byteValue());
    }
    byte[] topicBytes = topics.getWrittenBytes();
    int length = messageIdBytes.length + topicBytes.length;
    byte[] assembled = ByteUtils.add((128 | 2 | (dup ? 8 : 0)), ByteUtils.encodeVariableLength(length), messageIdBytes,
        topicBytes);
    checkArrayEquals(p.createMessage(Version._31), assembled);
  }

  private static void testPublishMessage(boolean dup, int qos, boolean retain, int messageId, String topic,
      byte[] payload) {
    PUBLISH p = new PUBLISH(dup, qos, retain, messageId, topic, payload);
    GrowableBuffer variableHeader = new GrowableBuffer(10);
    variableHeader.putString(topic);
    if (qos > 0) {
      variableHeader.putRaw(ByteUtils.unsignedShortBytes(messageId));
    }
    byte[] variableHeaderBytes = variableHeader.getWrittenBytes();
    int length = variableHeaderBytes.length + payload.length;
    GrowableBuffer raw = new GrowableBuffer(length);
    // make byte 1
    int b1 = 48;
    if (dup) b1 |= 8;
    if (qos == 2) b1 |= 4;
    if (qos == 1) b1 |= 2;
    if (retain) b1 |= 1;
    raw.writeIByte(b1);
    // System.out.println(Arrays.toString(raw.getWrittenBytes()));
    raw.putRaw(ByteUtils.encodeVariableLength(length));
    // System.out.println(Arrays.toString(raw.getWrittenBytes()));
    raw.putRaw(variableHeaderBytes);
    // System.out.println(Arrays.toString(raw.getWrittenBytes()));
    raw.putRaw(payload);
    checkArrayEquals(raw.getWrittenBytes(), p.createMessage(Version._31));
    // check fields
    PUBLISH p2 = new PUBLISH(p.createMessage(Version._31));
    Assert.assertEquals(p.getQos(), p2.getQos());
    Assert.assertEquals(p.isDup(), p2.isDup());
    Assert.assertEquals(p.isRetain(), p2.isRetain());
  }

  private static void testConnectMessage(String username, String password, boolean wilLRetain, int willQos,
      String willMessage, String willTopic, boolean cleanSession, int keepAlive, String clientId) {
    CONNECT c = new CONNECT(username, password, wilLRetain, willQos, willMessage, willTopic, cleanSession, keepAlive,
        clientId);
    byte[] payload = createConnectPayload(c.clientId, willTopic, willMessage, username, password);
    byte[] bytes = c.createMessage(Version._31);
    // \
    GrowableBuffer b = new GrowableBuffer(10);
    b.writeIByte(16);
    int length = 12 + payload.length;
    b.putRaw(ByteUtils.encodeVariableLength(length));
    byte[] keepAliveBytes = ByteUtils.unsignedShortBytes(keepAlive);
    // calculate flags
    int flags = 0;
    if (username != null) flags |= 128;
    if (password != null) flags |= 64;
    if (wilLRetain) flags |= 32;
    if (willQos == 2) flags |= 16;
    if (willQos == 1) flags |= 8;
    if (willMessage != null) flags |= 4;
    if (cleanSession) flags |= 2;
    b.putRaw(new byte[] { 0, 6, 'M', 'Q', 'I', 's', 'd', 'p', 3, (byte) flags, keepAliveBytes[0], keepAliveBytes[1] });
    b.putRaw(createConnectPayload(c.clientId, willTopic, willMessage, username, password));
    checkArrayEquals(bytes, b.getWrittenBytes());
  }

  private static byte[] createConnectPayload(String... fields) {
    GrowableBuffer b = new GrowableBuffer(10);
    for (String s : fields) {
      b.putString(s);
    }
    return b.getWrittenBytes();
  }
}
