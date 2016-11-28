package com.mqttclient.protocol;

public class PINGREQ extends ClientMessage {
  /**
   * The PINGREQ message is an "are you alive?" message that is sent from a
   * connected client to the server.
   */
  public PINGREQ() {
    super(MessageType.PINGREQ.value, 0);
  }

  @Override
  public byte[] createMessage(Version version) {
    return new byte[] { (byte) 192, 0 };
  }
}
