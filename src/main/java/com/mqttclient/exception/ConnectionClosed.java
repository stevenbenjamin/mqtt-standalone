package com.mqttclient.exception;

public class ConnectionClosed extends MqttException {
  public ConnectionClosed(String message) {
    super(message);
  }
}
