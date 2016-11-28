package com.mqttclient.protocol;

public class DISCONNECT extends ClientMessage {
  /** DUP, QoS, and RETAIN flags are not used in the DISCONNECT message. */
  public DISCONNECT() {
    super(MessageType.DISCONNECT.value, 0);
  }

  @Override
  public byte[] createMessage(Version version) {
    return new byte[] { (byte) 224, 0 };
  }
}
