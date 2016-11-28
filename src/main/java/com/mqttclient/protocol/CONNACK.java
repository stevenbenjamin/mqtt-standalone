package com.mqttclient.protocol;

public class CONNACK implements ServerMessage {
  public final ReturnCode returnCode;
  /** Only supported in mqtt 3.1.1. */
  public boolean sessionPresent;

  public enum ReturnCode {
    ConnectionAccepted(0, "Connection Accepted"), //
    ConnectionRefused(1, "Connection Refused, unacceptable protocol version"), //
    IdentifierRejected(2, "The Client identifier is correct UTF-8 but not allowed by the Server"), //
    ServerUnavailable(3, "Connection Refused, Server unavailable"), //
    BadUserNameOrPassword(4, "Connection Refused, bad user name or password"), //
    NotAuthorized(5, "Connection Refused, not authorized"), //
    Reserved(6, "Reserved"), //
    BadReturnCode(7, "Return Code not understood");
    public final byte returnCode;
    public final String message;

    private ReturnCode(int code, String msg) {
      returnCode = (byte) code;
      message = msg;
    }
  }

  @Override
  public int type() {
    return MessageType.CONNACK.value;
  }

  public CONNACK(byte[] message) {
    byte code = message.length < 4 ? 7 : message[3];
    sessionPresent = (message[2] & 2) > 0;
    if (code <= 6) {
      returnCode = ReturnCode.values()[code];
    } else {
      returnCode = ReturnCode.BadReturnCode;
    }
  }

  @Override
  public String toString() {
    return "CONNACK [sessionPresent=" + sessionPresent + " returnCode=" + returnCode + "]";
  }

  @Override
  public int byteLength() {
    return 4;
  }
}
