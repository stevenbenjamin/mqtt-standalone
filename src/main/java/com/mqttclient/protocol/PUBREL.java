package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

/**
 * A PUBREL message is the response either from a publisher to a PUBREC message
 * from the server, or from the server to a PUBREC message from a subscriber. It
 * is the third message in the QoS 2 protocol flow.
 */
public class PUBREL extends ClientMessage implements ServerMessage {
  /** id of the message that's being acknowledged. */
  public int messageId;

  public PUBREL(byte[] message) {
    super(MessageType.PUBREL.value, 0);
    assert (message.length == 4);
    messageId = ByteUtils.fromUnsignedShortBytes(message[2], message[3]);
  }

  public PUBREL(int messageId) {
    super(MessageType.PUBREL.value, 0);
    this.messageId = messageId;
  }

  @Override
  public int type() {
    return MessageType.PUBREL.value;
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] msgIdBytes = ByteUtils.unsignedShortBytes(messageId);
    // {0110 | 001x, remaininglength (2), message id bits }
    return new byte[] { 98, 2, msgIdBytes[0], msgIdBytes[1] };
  }

  @Override
  public int byteLength() {
    return 4;
  }
}
