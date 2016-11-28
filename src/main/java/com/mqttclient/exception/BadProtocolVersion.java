package com.mqttclient.exception;

public class BadProtocolVersion extends MqttException {
  public BadProtocolVersion() {
    super("Connection Refused, unacceptable protocol version");
  }
}
