package com.mqttclient.netty;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.MqttCallback;
import com.mqttclient.ReconnectHandler;
import com.mqttclient.exception.BadProtocolVersion;
import com.mqttclient.exception.ConnectionFailure;
import com.mqttclient.exception.MqttException;
import com.mqttclient.exception.UnrecoverableMqttException;
import com.mqttclient.protocol.CONNACK;
import com.mqttclient.protocol.CONNECT;
import com.mqttclient.protocol.ClientMessage;
import com.mqttclient.protocol.DISCONNECT;
import com.mqttclient.protocol.MessageType;
import com.mqttclient.protocol.PINGREQ;
import com.mqttclient.protocol.PINGRESP;
import com.mqttclient.protocol.PUBACK;
import com.mqttclient.protocol.PUBLISH;
import com.mqttclient.protocol.QOSMessageBuffer;
import com.mqttclient.protocol.SUBACK;
import com.mqttclient.protocol.SUBSCRIBE;
import com.mqttclient.protocol.ServerMessage;
import com.mqttclient.protocol.UNSUBACK;
import com.mqttclient.protocol.UNSUBSCRIBE;
import com.mqttclient.protocol.Version;
import com.mqttclient.protocol.util.GrowableBuffer;
import com.mqttclient.protocol.util.Tuple;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MqttChannelHandler extends ChannelInboundHandlerAdapter {
  private static Logger logger = LoggerFactory.getLogger(MqttChannelHandler.class);
  MqttCallback callback;
  /** How long do we wait for a response. */
  public static final long WAIT_FOR_RESPONSE_TIME = 10_000;
  AtomicInteger messageId = new AtomicInteger(0);
  private Channel channel;
  public MessageTracer tracer = MessageTracer.NO_OP;
  /*
   * Create individual listeners as per/message type per-channel instances.These
   * can't be instantiated until we have the callback to reference to.
   */
  private SendListener connectListener;
  private SendListener disconnectListener;
  private SendListener publishListener;
  private SendListener subscribeListener;
  private SendListener unsubscribeListener;
  private ReconnectHandler reconnectHandler;
  /**
   * Map of the type we're waiting for and the message that triggered the wait.
   */
  public final EnumMap<MessageType, Runnable> messagesInTransit = new EnumMap<>(MessageType.class);
  PingHandler pingHandler;
  public Version version = Version._311;// 1;// assume mqtt 311 unless error
  ChannelHandlerContext ctx;
  // for reporting.
  protected String connectString;
  // have to initialize to true for the intial connection messages to passs
  protected boolean isConnected = true;
  // * message buffer is null unless we set to qos > 0
  private QOSMessageBuffer qosMessageBuffer = null;
  protected int qos;

  /**
   * Because of the way netty is structured with a global bootstrap we have to
   * create the handler and THEN attach the callback to it.
   */
  void init(String connectString, Channel c, int qos, MqttCallback mc, ReconnectHandler reconnectHandler) {
    this.connectString = connectString;
    this.callback = mc;
    this.channel = c;
    // setup listeners
    connectListener = new SendListener(MessageType.CONNECT, reconnectHandler, callback::onConnect,
        callback::onConnectFail);
    disconnectListener = new SendListener(MessageType.DISCONNECT, reconnectHandler, callback::onDisconnect,
        callback::onDisconnectFail);
    publishListener = new SendListener(MessageType.PUBLISH, reconnectHandler, callback::onPublish,
        callback::onPublishFail);
    subscribeListener = new SendListener(MessageType.SUBSCRIBE, reconnectHandler, callback::onSubscribe,
        callback::onSubscribeFail);
    unsubscribeListener = new SendListener(MessageType.UNSUBSCRIBE, reconnectHandler, callback::onUnsubscribe,
        callback::onUnsubscribeFail);
    this.reconnectHandler = reconnectHandler;
    this.ctx = c.pipeline().context(this);
    setQOS(qos);
  }

  public void setQOS(int qos) {
    this.qos = qos;
    qosMessageBuffer = (qos == 0) ? null : new QOSMessageBuffer(this);
  }

  GrowableBuffer buf;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf bb = (ByteBuf) msg;
    byte[] bytes = new byte[bb.readableBytes()];
    bb.readBytes(bytes);
    bb.release();
    if (buf != null) buf.putRaw(bytes);
    else {
      buf = new GrowableBuffer(bytes, 0);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    if (buf == null) {
      logger.debug("Channel read complete but message is empty:{}", ctx.channel().remoteAddress());
      return;
    }
    ServerMessage[] messages = ServerMessage.readMessages(buf.getWrittenBytes());
    buf = null;
    for (ServerMessage m : messages) {
      handleIncoming(m);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    logger.debug("Channel inactive fired");
    reconnectHandler.scheduleReconnect();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    callback.onError(cause);
  }

  public void setDebug(boolean on) {
    tracer = on ? MessageTracer.createDebugTracer(connectString) : MessageTracer.NO_OP;
  }

  void send(ClientMessage m, ChannelFutureListener l) {
    if (!isConnected) {
      logger.info("Not sending message {} because the channel is not connected", m);
    } else {
      tracer.traceOutgoing(m);
      ByteBuf msg = m.createBufferMessage(version);
      ChannelFuture f = ctx.write(msg);
      if (l != null) f.addListener(l);
      ctx.flush();
    }
  }

  /**
   * The message identifier is present in the variable header of the following
   * MQTT messages:
   * 
   * PUBLISH (only on qos > 0),PUBACK, PUBREC, PUBREL, PUBCOMP,
   * SUBSCRIBE(always), SUBACK, UNSUBSCRIBE(always), UNSUBACK.
   * 
   * The Message Identifier (Message ID) field is only present in messages where
   * the QoS bits in the fixed header indicate QoS levels 1 or 2. See section on
   * Quality of Service levels and flows for more information.
   * 
   * The Message ID is a 16-bit unsigned integer that must be unique amongst the
   * set of "in flight" messages in a particular direction of communication. It
   * typically increases by exactly one from one message to the next, but is not
   * required to do so.
   * 
   * Do not use Message ID 0. It is reserved as an invalid Message ID.
   */
  int generateMessageId() {
    return 1 + (messageId.incrementAndGet() % 65535);
  }

  /**
   * Close this handler. After this method no more messages will be processed.
   * Assumes a disconnect message has already been sent.
   */
  public void close() {
    if (isOpen()) channel.close();
    if (pingHandler != null) pingHandler.close();
    isConnected = false;
  }

  public boolean isOpen() {
    return channel != null && channel.isOpen();
  }

  // --------------------------------------
  // Action
  // --------------------------------------
  void waitFor(MessageType messageType, Runnable attachement) {
    if (messagesInTransit.containsKey(messageType)) {
      logger.warn("Already Waiting for message of type {} : {}", messageType, attachement);
    }
    messagesInTransit.put(messageType, attachement);
  }

  private static final Runnable ACTION_NO_OP = new Runnable() {
    @Override
    public void run() {
    }
  };

  /** Wait with a default placeholder. */
  void waitFor(MessageType messageType) {
    waitFor(messageType, ACTION_NO_OP);
  }

  // --------------------------------------
  // connect
  // --------------------------------------
  /**
   * 
   * @param username
   * @param password
   * @param keepAlive
   *          0 means no keep alive.
   * @param clientId
   */
  public void connect(String username, String password, int keepAlive, String clientId) { //
    connectAndSubscribe(username, password, keepAlive, clientId, null);
  }

  /** Also subscribe to topic if not null. */
  public void connectAndSubscribe(String username, String password, int keepAlive, String clientId, String topic) { //
    if (keepAlive > 0) pingHandler = new PingHandler(this, keepAlive);
    // call subscribe on topic if it's not null
    if (topic != null) {
      waitFor(MessageType.CONNACK, new Runnable() {
        @Override
        public void run() {
          subscribe(topic);
        }
      });
    }
    CONNECT c = new CONNECT(username, password, keepAlive * 3, clientId);
    send(c, connectListener);
  }

  void onConnack(CONNACK c) {
    isConnected = false;
    switch (c.returnCode) {
      case ConnectionAccepted:
        isConnected = true;
        callback.onConnect();
        // if there's a runnable waiting for connack (e.g. subscribe, run it)
        Runnable onConnect = messagesInTransit.remove(MessageType.CONNACK);
        if (onConnect != null) onConnect.run();
        break;
      case BadReturnCode:
        handleConnectException(new BadProtocolVersion());
        break;
      case BadUserNameOrPassword:
        handleConnectException(new UnrecoverableMqttException(CONNACK.ReturnCode.BadUserNameOrPassword.message));
        break;
      case ConnectionRefused:
        handleConnectException(new UnrecoverableMqttException(CONNACK.ReturnCode.ConnectionRefused.message));
        break;
      case IdentifierRejected:
        handleConnectException(new UnrecoverableMqttException(CONNACK.ReturnCode.IdentifierRejected.message));
        break;
      case NotAuthorized:
        handleConnectException(new UnrecoverableMqttException(CONNACK.ReturnCode.NotAuthorized.message));
        break;
      case Reserved:
        handleConnectException(
            new UnrecoverableMqttException("Unknown (reserved) return code received from Mqtt CONNACK message"));
        break;
      case ServerUnavailable:
        handleConnectException(new ConnectionFailure(CONNACK.ReturnCode.ServerUnavailable.message));
        break;
      default:
        callback.onConnectFail(new ConnectionFailure("Connection failure with unrecognized return code."));
    }
  }

  private void handleConnectException(MqttException e) {
    callback.onConnectFail(e);
    isConnected = false;
    if (!(e instanceof UnrecoverableMqttException)) {
      reconnectHandler.scheduleReconnect();
    }
  }

  void onReceive(PUBLISH p) {
    callback.onReceive(p.topic, p.payload());
  }

  // --------------------------------------
  // disconnect
  // --------------------------------------
  public void disconnect() {
    logger.info("Disconnecting {}", connectString);
    send(new DISCONNECT(), disconnectListener);
    isConnected = false;
    close();
  }

  // --------------------------------------
  // ping
  // --------------------------------------
  void pingreq() {
    // if we're still waiting for a ping response it's an error.
    Runnable lastPingResp = messagesInTransit.remove(MessageType.PINGRESP);
    if (lastPingResp != null) {
      pingHandler.error();
      logger.warn("no response from last ping for {}", connectString);
    }
    waitFor(MessageType.PINGRESP);
    send(new PINGREQ(), null);
  }

  void onPingResp(PINGRESP c) {
    messagesInTransit.remove(MessageType.PINGRESP);
    logger.debug("{} received a ping", connectString);
    if (pingHandler != null) pingHandler.scheduleNext();
  }

  // --------------------------------------
  // publish
  // --------------------------------------
  public void publish(String topic, byte[] message, int qos) {
    // (wait for ack only for qos > 0), currently unsupported
    // for now we're just sending @ qos 0
    if (isConnected) {
      PUBLISH p = new PUBLISH(topic, message, qos, (qos == 0 ? 0 : generateMessageId()));
      if (qos > 0) {
        p.setQos(1);
        qosMessageBuffer.buffer(p);
      }
      send(p, publishListener);
    } else {
      logger.info("Not publishing to {} because the channel is not connected", topic);
    }
  }

  public void republish(PUBLISH p) {
    /*
     * if the message sent is already a dup, don't bother republishing a 3d
     * time. So if this is the first republish, try again to save in the buffer.
     * Otherwise give up.
     */
    boolean willTryAgain = !p.isDup();
    p.setDup(true);
    if (isConnected) {
      send(p, publishListener);
      if (willTryAgain) {
        qosMessageBuffer.buffer(p);
      } else {
        logger.info("Giving up sending msg {} after 3 tries.", p.messageId);
      }
    }
  }

  // --------------------------------------
  // subscribe
  // --------------------------------------
  /**
   * Subscribe messages will always have a message id that will be echoed back
   * in the suback
   * 
   * @param topics
   */
  public void subscribe(String... topics) {
    List<Tuple<String, Integer>> l = Arrays.asList(topics).stream().map(s -> new Tuple<>(s, 0))
        .collect(Collectors.toList());
    SUBSCRIBE s = new SUBSCRIBE(generateMessageId(), false, l);
    waitFor(MessageType.SUBACK);
    send(s, subscribeListener);
  }

  void onSubAck(SUBACK c) {
    logger.debug("Received suback with qos values {} ", c.qosValues);
    if (c.qosValues.size() == 0) {
      callback.onSubscribeFail(new MqttException("No QOS values found for suback in subscribe response"));
    } else {
      callback.onSubscribe();
    }
  }

  // --------------------------------------
  // unsubscribe
  // --------------------------------------
  /**
   * Unsubscribe messages will always have a message id that will be echoed back
   * in the unsuback
   * 
   * @param topics
   */
  public void unsubscribe(String... topics) {
    UNSUBSCRIBE s = new UNSUBSCRIBE(generateMessageId(), Arrays.asList(topics));
    send(s, unsubscribeListener);
    waitFor(MessageType.UNSUBACK);
  }

  void onUnSubAck(UNSUBACK c) {
    callback.onUnsubscribe();
  }

  void onPubAck(PUBACK c) {
    qosMessageBuffer.remove(c);
  }

  // --------------------------------------
  // unsupported, only for QOS > 0
  // --------------------------------------
  void pubcomp() {
    throw new UnsupportedOperationException("pubcomp is only required for qos 2. Currently only supporting qos 0");
  }

  void pubrec() {
    throw new UnsupportedOperationException("pubrec is only required for qos 2. Currently only supporting qos 0");
  }

  void pubrel() {
    throw new UnsupportedOperationException("pubrel is only required for qos 2. Currently only supporting qos 0");
  }

  // --------------------------------------
  // server messages
  // --------------------------------------
  void handleIncoming(ServerMessage msg) {
    if (msg == null) {
      return;
    }
    tracer.traceIncoming(msg);
    switch (msg.type()) {
      case 2: // CONNACK:
        onConnack((CONNACK) msg);
        break;
      case 3:// MessageType.PUBLISH:
        this.onReceive((PUBLISH) msg);
        break;
      case 13: // MessageType.PINGRESP:
        onPingResp((PINGRESP) msg);
        break;
      case 9:// MessageType.SUBACK:
        this.onSubAck((SUBACK) msg);
        break;
      case 11: // MessageType.UNSUBACK:
        this.onUnSubAck((UNSUBACK) msg);
        break;
      // Unsupported types for QOS > 0
      case 4: // MessageType.PUBACK:
        this.onPubAck((PUBACK) msg);
        break;
      case 5: // MessageType.PUBREC:
        logger.debug("pubrec is only required for qos 2. Currently only supporting qos 0");
        break;
      case 6: // MessageType.PUBREL:
        logger.debug("pubrel is only required for qos 2. Currently only supporting qos 0");
        break;
      case 7: // MessageType.PUBCOMP:
        logger.debug("pubcomp is only required for qos 2. Currently only supporting qos 0");
        break;
      default:
        logger.debug("Message " + msg + " type=" + msg.type() + " unsupported.");
    }
  }

  @Override
  public String toString() {
    return "MqttChannelHandler [callback=" + callback + ", channel=" + channel + ", open=" + isOpen() + "]";
  }
}
