package com.mqttclient.exception;

public class UnrecoverableMqttException extends MqttException {
  public UnrecoverableMqttException(String message) {
    super(message);
  }
}
