package com.mqttclient;

import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import com.mqttclient.exception.MqttException;
import com.mqttclient.netty.MessageTracer;
import com.mqttclient.netty.MqttChannelFactory;
import com.mqttclient.netty.MqttChannelHandler;
import com.mqttclient.protocol.ClientMessage;
import com.mqttclient.protocol.MessageType;
import com.mqttclient.protocol.ServerMessage;
import com.mqttclient.protocol.TestUtils;

public class PublicBrokerTest extends TestUtils {
  MessageTrackingCallback callback = new MessageTrackingCallback();

  public class TestTracer extends MessageTracer {
    public TestTracer() {
      super("test tracer");
    }

    private LinkedHashMap<MessageType, ServerMessage> incomingMessages = new LinkedHashMap<>();
    private LinkedHashMap<MessageType, ClientMessage> outgoingMessages = new LinkedHashMap<>();

    @Override
    public synchronized void traceOutgoing(ClientMessage m) {
      System.out.println("OUTGOING " + m);
      outgoingMessages.put(MessageType.values()[m.type()], m);
    }

    @Override
    public synchronized void traceIncoming(ServerMessage m) {
      System.out.println("INCOMING " + m);
      incomingMessages.put(MessageType.values()[m.type()], m);
    }

    @Override
    public synchronized boolean sent(MessageType t) {
      System.out.println("TRACER : SENT " + t);
      return outgoingMessages.remove(t) != null;
    }

    @Override
    public synchronized boolean received(MessageType t) {
      boolean contains = incomingMessages.containsKey(t);
      System.out.println("TRACER : TEST RECEIVED CONTAINS " + t + "? " + contains + " IN " + incomingMessages.keySet());
      return incomingMessages.remove(t) != null;
    }

    @Override
    public String toString() {
      return "Tracer: \n\tincoming=" + incomingMessages + "\n\toutgoing=" + outgoingMessages;
    }
  }

  @Test
  public void testSSL() throws MqttException {
    testWHandler(new MqttChannelFactory().ssl.openChannel("test.mosquitto.org", 8883, 0, callback), null, null, 0);
  }

  @Test
  public void testTCP() throws MqttException {
    testWHandler(new MqttChannelFactory().tcp.openChannel("localhost", 1883, 0, callback), "foo", "bar", 0);
  }

  @Test
  public void testTCP_QOS_1() throws MqttException {
    testWHandler(new MqttChannelFactory().tcp.openChannel("localhost", 1883, 1, callback), "foo", "bar", 1);
  }

  private void testWHandler(MqttChannelHandler handler, String username, String password, int qos) {
    TestTracer tracer = new TestTracer();
    handler.tracer = tracer;
    handler.connect(username, password, 60, "clientId");
    pause(1000);
    Assert.assertTrue(tracer.sent(MessageType.CONNECT));
    Assert.assertTrue(tracer.received(MessageType.CONNACK));
    handler.subscribe("a", "b");
    pause(500);
    Assert.assertTrue(tracer.sent(MessageType.SUBSCRIBE));
    Assert.assertTrue(tracer.received(MessageType.SUBACK));
    handler.publish("a", "abcdefg".getBytes(), qos);
    pause(500);
    Assert.assertTrue(tracer.sent(MessageType.PUBLISH));
    if (qos == 1) {
      Assert.assertTrue(tracer.received(MessageType.PUBACK));
    }
    Assert.assertTrue(tracer.received(MessageType.PUBLISH));
    // subscribed to both a and b -
    pause(500);
    handler.publish("b", "abcdefg".getBytes(), qos);
    pause(500);
    Assert.assertTrue(tracer.sent(MessageType.PUBLISH));
    if (qos == 1) {
      Assert.assertTrue(tracer.received(MessageType.PUBACK));
    }
    Assert.assertTrue(tracer.received(MessageType.PUBLISH));
    handler.unsubscribe("a");
    pause(500);
    Assert.assertTrue(tracer.sent(MessageType.UNSUBSCRIBE));
    Assert.assertTrue(tracer.received(MessageType.UNSUBACK));
    handler.publish("a", "abcdefg".getBytes(), qos);
    pause(500);
    Assert.assertTrue(tracer.sent(MessageType.PUBLISH));
    Assert.assertFalse(tracer.received(MessageType.PUBLISH));
    // subscribed to both a and b -
    handler.publish("b", "abcdefg".getBytes(), qos);
    pause(500);
    Assert.assertTrue(tracer.sent(MessageType.PUBLISH));
    Assert.assertTrue(tracer.received(MessageType.PUBLISH));
    handler.disconnect();
    Assert.assertTrue(tracer.sent(MessageType.DISCONNECT));
  }

  public static void mains(String args[]) throws MqttException {
    MqttChannelHandler handler = new MqttChannelFactory().tcp.openChannel("test.mosquitto.org", 1883, 0,
        new MessageTrackingCallback());
    while (true) {
      handler.connect(null, null, 10, "clientId");
      pause(400);
      handler.subscribe("a", "b");
      pause(400);
      for (int i = 0; i < 10000000; i++) {
        handler.publish("a", "abcdefg".getBytes(), 0);
        pause(100);
      }
      // subscribed to both a and b -
      handler.publish("b", "abcdefg".getBytes(), 0);
      pause(400);
      handler.unsubscribe("a");
      pause(400);
      handler.publish("a", "abcdefg".getBytes(), 0);
      pause(400);
      // subscribed to both a and b -
      handler.publish("b", "abcdefg".getBytes(), 0);
      pause(400);
      // handler.disconnect();
    }
  }
}
