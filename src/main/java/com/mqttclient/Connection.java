package com.mqttclient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.exception.ConnectionFailure;
import com.mqttclient.exception.MqttException;
import com.mqttclient.netty.MqttChannelFactory;
import com.mqttclient.netty.MqttChannelHandler;
import com.mqttclient.protocol.util.ByteUtils;

/**
 * Bridge between the flow object and the actual channel holding the connection
 * to the running implementation, held in the callback.
 * 
 * Given these elements, responsible for basic mqtt channel ops -subscribe,
 * publish, unsub, etc etc
 * 
 * @author stevenbenjamin
 *
 */
public class Connection {
  protected static Logger logger = LoggerFactory.getLogger(Connection.class);
  static MqttChannelFactory channelFactory;
  public Optional<MqttChannelHandler> channelHandler = Optional.empty();
  public MqttCallback callback;
  public final MqttConnection fmqtt;
  static ScheduledThreadPoolExecutor executor;
  private String clientId;
  private ReconnectHandler reconnectHandler = new ReconnectHandler(this);
  /** Hack to allow us to run some tests without the connection active. */
  public static boolean debug;

  public Connection(MqttConnection c, MqttCallback callback) throws MqttException, URISyntaxException {
    this.fmqtt = c;
    this.callback = callback;
    clientId = Optional.ofNullable(fmqtt.getClientId()).orElse(ByteUtils.randomString(23));
    int qos = Optional.ofNullable(fmqtt.getQos()).orElse(0).intValue();
    if (!debug) {
      try {
        this.channelHandler = channelFactory.openChannel(fmqtt.asURI(), qos, callback, reconnectHandler);
      } catch (MqttException e) {
        reconnectHandler.scheduleReconnect();
        callback.onError(new ConnectionFailure("Unable to open connection to " + fmqtt.getUri()));
      }
    }
  }

  public void subscribe() {
    subscribe(fmqtt.getTopic());
  }

  public void publishMessage(byte[] message) throws IOException {
    /*
     * if the drop has a topic set in metadata, use that. Otherwise use the
     * fmqtt topic.
     */
    if (message == null || message.length == 0) {
      return;
    }
    publish(fmqtt.getTopic(), message);
  }

  public void publish(byte[] payload) {
    publish(fmqtt.getTopic(), payload);
  }

  public void unsubscribe() {
    unsubscribe(fmqtt.getTopic());
  }

  public void subscribe(String... topics) {
    logger.info("subscribe to {} {}", topics, fmqtt.getId());
    channelHandler.ifPresent(h -> h.subscribe(topics));
  }

  public void unsubscribe(String topics) {
    logger.info("unsubscribe from {} {}", topics, fmqtt.getId());
    channelHandler.ifPresent(h -> h.unsubscribe(topics));
  }

  public void publish(String topic, byte[] payload) {
    channelHandler.ifPresent(h -> h.publish(topic, payload, fmqtt.getQos() == null ? 0 : fmqtt.getQos().intValue()));
  }

  public void connectAndSubscribe() {
    logger.info("connecting to {}:{} with username={}, password={}, clientId={} topic={}", new Object[] { fmqtt.getId(),
        fmqtt.getUri(), fmqtt.getUsername(), fmqtt.getPassword(), clientId, fmqtt.getTopic() });
    if (channelHandler.isPresent()) {
      channelHandler.get().connectAndSubscribe(Optional.ofNullable(fmqtt.getUsername()).orElse(null),//
          Optional.ofNullable(fmqtt.getPassword()).orElse(null),//
          60,//
          clientId,//
          fmqtt.getTopic());
    } else {
      callback.onConnectFail(new ConnectionFailure("Couldn't create a connection"));
    }
  }

  public void disconnect() {
    channelHandler.ifPresent(h -> h.disconnect());
  }

  public boolean isCurrentlyConnected() {
    return channelHandler.isPresent() && channelHandler.get().isOpen();
  }
}
