package com.mqttclient;

/** Callback implemeted in clojure interface. Bridge to Connection object. */
public interface MqttCallback {
  public void onSubscribe();

  public void onSubscribeFail(Throwable t);

  public void onUnsubscribe();

  public void onUnsubscribeFail(Throwable t);

  public void onConnect();

  public void onConnectFail(Throwable t);

  public void onConnectionLost();

  public void onDisconnect();

  public void onDisconnectFail(Throwable t);

  public void onPublish();

  public void onPublishFail(Throwable t);

  public void onReceive(String topic, byte[] msg);

  public void onReceiveFail(Throwable t);

  public void onError(Throwable t);

  /**
   * Just a tracker object that only prints out what events it sees to stdout.
   */
  public class Debug implements MqttCallback {
    @Override
    public void onSubscribe() {
      System.out.println("onSubscribe");
    }

    @Override
    public void onSubscribeFail(Throwable t) {
      System.out.println("onSubscribeFail " + t);
    }

    @Override
    public void onUnsubscribe() {
      System.out.println("onUnsubscribe");
    }

    @Override
    public void onUnsubscribeFail(Throwable t) {
      System.out.println("onUnsubscribeFail " + t);
    }

    @Override
    public void onConnect() {
      System.out.println("onConnect");
    }

    @Override
    public void onConnectFail(Throwable t) {
      System.out.println("onConnectFail " + t);
    }

    @Override
    public void onDisconnect() {
      System.out.println("onDisconnect");
    }

    @Override
    public void onDisconnectFail(Throwable t) {
      System.out.println("onDisconnectFail " + t);
    }

    @Override
    public void onPublish() {
      System.out.println("onPublish");
    }

    @Override
    public void onPublishFail(Throwable t) {
      System.out.println("onPublishFail " + t);
    }

    @Override
    public void onReceive(String topic, byte[] msg) {
      System.out.printf("onReceive @topic [%s:%s]\n", topic, new String(msg));
    }

    @Override
    public void onReceiveFail(Throwable t) {
      System.out.println("onReceiveFail " + t);
    }

    @Override
    public void onError(Throwable t) {
      System.out.println("onError " + t);
      t.printStackTrace();
    }

    @Override
    public void onConnectionLost() {
      System.out.println("connection lost");
    }
  }
}
