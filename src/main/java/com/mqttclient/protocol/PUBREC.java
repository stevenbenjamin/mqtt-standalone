package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

/**
 * A PUBREC message is the response to a PUBLISH message with QoS level 2. It is
 * the second message of the QoS level 2 protocol flow. A PUBREC message is sent
 * by the server in response to a PUBLISH message from a publishing client, or
 * by a subscriber in response to a PUBLISH message from the server.
 */
public class PUBREC extends ClientMessage implements ServerMessage {
  /** id of the message that's being acknowledged. */
  public int messageId;

  public PUBREC(byte[] message) {
    super(MessageType.PUBREC.value, 0);
    messageId = ByteUtils.fromUnsignedShortBytes(message[2], message[3]);
  }

  public PUBREC(int messageId) {
    super(MessageType.PUBREC.value, 2);
    this.messageId = messageId;
  }

  @Override
  public int type() {
    return MessageType.PUBREC.value;
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] msgIdBytes = ByteUtils.unsignedShortBytes(messageId);
    // {messagetype | flags, remaininglength (2), message id bits }
    return new byte[] { 80, 2, msgIdBytes[0], msgIdBytes[1] };
  }

  @Override
  public int byteLength() {
    return 4;
  }
}
