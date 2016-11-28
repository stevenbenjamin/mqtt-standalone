package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

public class PUBCOMP extends ClientMessage implements ServerMessage {
  public int messageId;

  public PUBCOMP(byte[] message) {
    super(MessageType.PUBCOMP.value, 0);
    messageId = ByteUtils.fromUnsignedShortBytes(message[2], message[3]);
  }

  public PUBCOMP(int messageId) {
    super(MessageType.PUBCOMP.value, 0);
    this.messageId = messageId;
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] msgIdBytes = ByteUtils.unsignedShortBytes(messageId);
    // {0110 | 001x, remaininglength (2), message id bits }
    return new byte[] { 99, 2, msgIdBytes[0], msgIdBytes[1] };
  }

  @Override
  public int type() {
    return MessageType.PUBCOMP.value;
  }

  @Override
  public int byteLength() {
    return 4;
  }
}
