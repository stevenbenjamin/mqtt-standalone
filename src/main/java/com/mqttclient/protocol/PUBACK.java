package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

public class PUBACK extends ClientMessage implements ServerMessage {
  public int messageId;

  public PUBACK(byte[] message) {
    super(MessageType.PUBACK.value, 0);
    messageId = ByteUtils.fromUnsignedShortBytes(message[2], message[3]);
  }

  /**
   * A PUBACK message is the response to a PUBLISH message with QoS level 1. A
   * PUBACK message is sent by a server in response to a PUBLISH message from a
   * publishing client, and by a subscriber in response to a PUBLISH message
   * from the server.
   */
  public PUBACK(int messageId) {
    super(MessageType.PUBACK.value, 0);
    this.messageId = messageId;
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] msgIdBytes = ByteUtils.unsignedShortBytes(messageId);
    return new byte[] { 64, 2, msgIdBytes[0], msgIdBytes[1] };
  }

  @Override
  public int type() {
    return MessageType.PUBACK.value;
  }

  @Override
  public int byteLength() {
    return 4;
  }
}
