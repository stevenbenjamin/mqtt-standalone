package com.mqttclient.exception;

public class ConnectionFailure extends MqttException {
  public ConnectionFailure(String message) {
    super(message);
  }
}
