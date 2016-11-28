package com.mqttclient.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.function.Consumer;

import com.mqttclient.ReconnectHandler;
import com.mqttclient.exception.ConnectionClosed;
import com.mqttclient.exception.MqttException;
import com.mqttclient.protocol.MessageType;

public class SendListener implements ChannelFutureListener {
  MessageType t;
  Consumer<Throwable> onFailure;
  Runnable onSuccess;
  ReconnectHandler reconnectHandler;
  protected static Runnable RunnableNoOp = new Runnable() {
    @Override
    public void run() {
    }
  };
  protected static Consumer<Throwable> ConsumerNoOp = new Consumer<Throwable>() {
    @Override
    public void accept(Throwable t) {
    }
  };

  public SendListener(MessageType t, ReconnectHandler reconnectHandler, Runnable onSuccess,
      Consumer<Throwable> onFailure) {
    this.t = t;
    this.onSuccess = onSuccess == null ? RunnableNoOp : onSuccess;
    this.onFailure = onFailure == null ? ConsumerNoOp : onFailure;
    this.reconnectHandler = reconnectHandler;
  }

  @Override
  public void operationComplete(ChannelFuture future) throws Exception {
    if (future.isDone()) {
      if (future.isSuccess()) {
        // log success
        onSuccess.run();
      } else if (future.cause() != null) {  // failure
        if (!(future.channel().isOpen())) {
          onFailure.accept(new ConnectionClosed("error sending " + t + " message because the channel was closed."));
          reconnectHandler.scheduleReconnect();
        } else {
          onFailure.accept(new MqttException("error sending " + t + " message").withCause(future.cause())
              .withMessageType(t));
        }
      } else if (future.isCancelled()) {
        onFailure.accept(new MqttException("error sending " + t + " message: Operation cancelled").withMessageType(t));
      }
    } else {
      throw new MqttException(String.format(
          "Unexpected state sending message %s: done=%s,success=%s,cancelled=%s,cause=%s", t, future.isDone(),
          future.isSuccess(), future.isCancelled(), future.cause()));
    }
  }
}