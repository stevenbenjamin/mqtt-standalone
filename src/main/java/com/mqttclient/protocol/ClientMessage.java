package com.mqttclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A message type that we may write to a server. Some mqtt message types are
 * only meaningful as client->server messages.
 */
public abstract class ClientMessage implements MqttMessage {
  int headerByte1;

  public ClientMessage(int type, int flags) {
    headerByte1 = type << 4 | flags;
  }

  @Override
  public int type() {
    return 0x00FF & (headerByte1 >> 4);
  }

  public boolean isDup() {
    return bitIsSet(headerByte1, 3);
  }

  public void setDup(boolean dup) {
    headerByte1 = setBit(headerByte1, 3, dup);
  }

  public int getQos() {
    return (headerByte1 >> 1) & 0x03;
  }

  public void setQos(int qos) {
    if (qos == 1) {
      headerByte1 = setBit(headerByte1, 1, true);
    } else if (qos == 2) {
      headerByte1 = setBit(headerByte1, 2, true);
    }
  }

  public boolean isRetain() {
    return bitIsSet(headerByte1, 0);
  }

  public void setRetain(boolean retain) {
    headerByte1 = setBit(headerByte1, 0, retain);
  }

  /** Default no-op implementation. */
  public byte[] payload() {
    return new byte[0];
  }

  public abstract byte[] createMessage(Version version);

  /** Return a netty buffer object view of the message bytes. */
  public ByteBuf createBufferMessage(Version version) {
    return Unpooled.copiedBuffer(createMessage(version));
  }

  static int setBit(int i, int bit, boolean value) {
    return value ? (0x00FF & (i | 1 << bit)) : (0x00FF & (i & ~(1 << bit)));
  }

  static boolean bitIsSet(int i, int bit) {
    return (i & (1 << bit)) > 0;
  }

  @Override
  public String toString() {
    return MessageType.values()[type()].toString();
  }
}
