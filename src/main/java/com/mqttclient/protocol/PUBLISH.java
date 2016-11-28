package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

/**
 * A PUBLISH message is sent by a client to a server for distribution to
 * interested subscribers. Each PUBLISH message is associated with a topic name
 * (also known as the Subject or Channel). This is a hierarchical name space
 * that defines a taxonomy of information sources for which subscribers can
 * register an interest. A message that is published to a specific topic name is
 * delivered to connected subscribers for that topic.
 * 
 * If a client subscribes to one or more topics, any message published to those
 * topics are sent by the server to the client as a PUBLISH message.
 * 
 * @author stevenbenjamin
 *
 */
public class PUBLISH extends ClientMessage implements ServerMessage {
  public int messageId;
  public String topic;
  byte[] payload;
  int ln;

  public PUBLISH(byte[] bytes) {
    super(MessageType.PUBLISH.value, bytes[0] & 0x07);
    ReadBuffer buf = new ReadBuffer(bytes);
    headerByte1 = bytes[0];
    ln = buf.readLength();
    buf.setMark();
    topic = buf.readString();
    if (this.getQos() > 0) {
      messageId = buf.readShort();
    }
    payload = buf.readBytes(ln - buf.distanceFromMark());
  }

  @Override
  public byte[] payload() {
    return payload;
  }

  /** Simplified qos 0 call. Message Id will be ignored because of qos 0. */
  public PUBLISH(String topic, byte[] payload, int qos, int messageId) {
    this(false, qos, false, messageId, topic, payload);
  }

  @Override
  public int hashCode() {
    return messageId;
  }

  /**
   * Publish messages are equal if their messageIds are equal. Used for tracking
   * ack on QOS > 1
   */
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof PUBLISH) && ((PUBLISH) obj).messageId == messageId;
  }

  @Override
  public String toString() {
    return "PUBLISH [qos=" + this.getQos() + " messageId=" + messageId + ", topic=" + topic + ", payload bytes="
        + (payload == null ? -1 : payload.length) + ":" + new String(payload) + "]";
  }

  public PUBLISH(boolean dup, int qos, boolean retain, int messageId, String topic, byte[] payload) {
    super(MessageType.PUBLISH.value, qos << 1);
    this.messageId = messageId;
    this.topic = topic;
    this.setQos(qos);
    this.setDup(dup);
    this.setRetain(retain);
    this.payload = payload;
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] topicNameBytes = ByteUtils.toLengthPrefixedString(topic);
    int length = topicNameBytes.length + payload.length;
    /*
     * If qos == 0 we don't send the message id.
     */
    if (this.getQos() == 0) {
      byte[] lengthBytes = ByteUtils.encodeVariableLength(length);
      byte[] output = ByteUtils.add(headerByte1, lengthBytes, topicNameBytes, payload);
      return output;
    }
    byte[] messageIdBytes = ByteUtils.unsignedShortBytes(messageId);
    // qos > 0 add message Id bytes
    byte[] lengthBytes = ByteUtils.encodeVariableLength(length + 2);
    return ByteUtils.add(headerByte1, lengthBytes, topicNameBytes, messageIdBytes, payload);
  }

  @Override
  public int type() {
    return MessageType.PUBLISH.value;
  }

  @Override
  public int byteLength() {
    return 1 + this.byteCountForLength(ln) + ln;
  }
}
