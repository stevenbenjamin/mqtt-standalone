package com.mqttclient.protocol;

/**
 * A PINGRESP message is the response sent by a server to a PINGREQ message and
 * means "yes I am alive".
 */
public class PINGRESP implements ServerMessage {
  public PINGRESP(byte[] message) {
    // ignore
  }

  @Override
  public int type() {
    return MessageType.PINGRESP.value;
  }

  @Override
  public String toString() {
    return "PINGRESP";
  }

  @Override
  public int byteLength() {
    return 2;
  }
}
