package com.mqttclient.exception;

import com.mqttclient.protocol.MessageType;

public class MqttException extends Exception {
  MessageType t;
  Throwable cause;

  public MqttException(String message) {
    super(message);
  }

  public MqttException withCause(Throwable th) {
    this.cause = th;
    return this;
  }

  public MqttException withMessageType(MessageType t) {
    this.t = t;
    return this;
  }
}
