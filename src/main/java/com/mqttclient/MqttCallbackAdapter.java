package com.mqttclient;

/** Default no-op for all callback methods. */
public class MqttCallbackAdapter implements MqttCallback {
  @Override
  public void onSubscribe() {
  }

  @Override
  public void onSubscribeFail(Throwable t) {
  }

  @Override
  public void onUnsubscribe() {
  }

  @Override
  public void onUnsubscribeFail(Throwable t) {
  }

  @Override
  public void onConnect() {
  }

  @Override
  public void onConnectFail(Throwable t) {
  }

  @Override
  public void onConnectionLost() {
  }

  @Override
  public void onDisconnect() {
  }

  @Override
  public void onDisconnectFail(Throwable t) {
  }

  @Override
  public void onPublish() {
  }

  @Override
  public void onPublishFail(Throwable t) {
  }

  @Override
  public void onReceive(String topic, byte[] msg) {
  }

  @Override
  public void onReceiveFail(Throwable t) {
  }

  @Override
  public void onError(Throwable t) {
  }
}
