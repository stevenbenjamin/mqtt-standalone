package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

public class UNSUBACK implements ServerMessage {
  public int messageId;

  public UNSUBACK(byte[] message) {
    messageId = ByteUtils.fromUnsignedShortBytes(message[2], message[3]);
  }

  @Override
  public int type() {
    return MessageType.UNSUBACK.value;
  }

  @Override
  public int byteLength() {
    return 4;
  }
}
