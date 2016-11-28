package com.mqttclient.protocol;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * /** A message type that we may receive from a server. Some mqtt message types
 * are only meaningful as server-client messages.
 * 
 * Each of these types must have a byte[] constructor since that's what we get
 * from MQTT.
 */
public interface ServerMessage extends MqttMessage {
  /** Read message from a netty buffer. */
  public static ServerMessage[] readMessages(ByteBuf buf) {
    byte[] bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    return readMessages(bytes);
  }

  static ServerMessage readSingleMessage(byte[] bytes) {
    int messageType = 0x0F & (bytes[0] >> 4);
    try {
      switch (messageType) {
        case 2:
          return new CONNACK(bytes);
        case 3:
          return new PUBLISH(bytes);
        case 4:
          return new PUBACK(bytes);
        case 5:
          return new PUBREC(bytes);
        case 6:
          return new PUBREL(bytes);
        case 7:
          return new PUBCOMP(bytes);
        case 9:
          return new SUBACK(bytes);
        case 11:
          return new UNSUBACK(bytes);
        case 13:
          return new PINGRESP(bytes);
        default:
          logger.error("Unknown Message Type {} : {} bytes", messageType, bytes.length);
      }
    } catch (Exception e) {
      logger.error("message read error:{}", e);
    }
    return null;
  }

  public static ServerMessage[] readMessages(byte[] bytes) {
    try {
      ServerMessage m = readSingleMessage(bytes);
      int used = m.byteLength();
      if (used == bytes.length) {
        return new ServerMessage[] { m };
      }
      List<ServerMessage> l = new ArrayList<>(2);
      l.add(m);
      while (used < bytes.length) {
        byte[] bts = new byte[bytes.length - used];
        System.arraycopy(bytes, used, bts, 0, bts.length);
        m = readSingleMessage(bts);
        if (m == null) return new ServerMessage[0];
        l.add(m);
        used += m.byteLength();
      }
      return l.toArray(new ServerMessage[l.size()]);
    } catch (Exception e) {
      return new ServerMessage[0];
    }
  }

  public int byteLength();
}
