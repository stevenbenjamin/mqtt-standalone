package com.mqttclient;

import java.util.LinkedHashMap;

import com.mqttclient.MqttCallback;
import com.mqttclient.protocol.MessageType;
import com.mqttclient.protocol.util.Tuple;

/** Callback for testing that tracks last messages and responses. */
public class MessageTrackingCallback implements MqttCallback {
  public final LinkedHashMap<MessageType, Object> messagesReceived = new LinkedHashMap<>();

  @Override
  public void onSubscribe() {
    messagesReceived.put(MessageType.SUBSCRIBE, true);
  }

  @Override
  public void onSubscribeFail(Throwable t) {
    messagesReceived.put(MessageType.SUBSCRIBE, t);
  }

  @Override
  public void onUnsubscribe() {
    messagesReceived.put(MessageType.UNSUBSCRIBE, true);
  }

  @Override
  public void onUnsubscribeFail(Throwable t) {
    messagesReceived.put(MessageType.UNSUBSCRIBE, t);
  }

  @Override
  public void onConnect() {
    messagesReceived.put(MessageType.CONNECT, true);
  }

  @Override
  public void onConnectFail(Throwable t) {
    messagesReceived.put(MessageType.CONNECT, t);
  }

  @Override
  public void onDisconnect() {
    messagesReceived.put(MessageType.DISCONNECT, true);
  }

  @Override
  public void onPublish() {
    System.out.println("ON PUBLISH OK");
    messagesReceived.put(MessageType.PUBLISH, true);
  }

  @Override
  public void onDisconnectFail(Throwable t) {
    messagesReceived.put(MessageType.DISCONNECT, t);
  }

  @Override
  public void onPublishFail(Throwable t) {
    System.out.println("ON PUBLISH FAIL " + t);
    messagesReceived.put(MessageType.PUBLISH, t);
  }

  @Override
  public void onReceive(String topic, byte[] msg) {
    messagesReceived.put(MessageType.PUBLISH, new Tuple<>(topic, msg));
  }

  @Override
  public void onReceiveFail(Throwable t) {
    messagesReceived.put(MessageType.PUBLISH, t);
  }

  @Override
  public void onError(Throwable t) {
    t.printStackTrace();
  }

  @Override
  public void onConnectionLost() {
    System.out.println("connection lost");
  }
}
