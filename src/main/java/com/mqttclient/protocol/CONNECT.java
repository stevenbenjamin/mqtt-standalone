package com.mqttclient.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.protocol.util.ByteUtils;
import com.mqttclient.protocol.util.GrowableBuffer;

public class CONNECT extends ClientMessage {
  private static Logger logger = LoggerFactory.getLogger(CONNECT.class);
  String username;
  String password;
  boolean wilLRetain;
  int willQos;
  boolean cleanSession;
  int keepAlive;
  String clientId;
  String willTopic;
  String willMessage;

  /** Basic connect w/no will, qos 0, no retain, cleansession. */
  public CONNECT(String username, String password, int keepAlive, String clientId) {
    this(username, password, false, 0, null, null, true, keepAlive, clientId);
  }

  public CONNECT(String username, String password, boolean wilLRetain, int willQos, String willMessage,
      String willTopic, boolean cleanSession, int keepAlive, String clientId) {
    super(MessageType.CONNECT.value, 0);
    this.username = username;
    this.password = password;
    this.wilLRetain = wilLRetain;
    this.willQos = willQos;
    this.cleanSession = cleanSession;
    this.keepAlive = keepAlive;
    this.willMessage = willMessage;
    this.willTopic = willTopic;
    this.clientId = clientId;
    if (clientId == null || clientId.length() == 0) {
      this.clientId = ByteUtils.randomString(23);
      logger.debug("trying to connect with a null client id:Assigning random id " + this.clientId);
    }
  }

  @Override
  public byte[] createMessage(Version version) {
    switch (version) {
      case _31:
        sanitizeClientIdFor31();
        byte[] keepAliveBytes = ByteUtils.unsignedShortBytes(keepAlive);
        byte[] payload = payload();
        byte[] variableHeader = new byte[] { 0, 6, 'M', 'Q', 'I', 's', 'd', 'p', 3, connectFlags(), keepAliveBytes[0],
            keepAliveBytes[1] };
        byte[] lengthBytes = ByteUtils.encodeVariableLength(12 + payload.length);
        return ByteUtils.add(headerByte1, lengthBytes, variableHeader, payload);
      case _311:
        keepAliveBytes = ByteUtils.unsignedShortBytes(keepAlive);
        payload = payload();
        variableHeader = new byte[] { 0, 4, 'M', 'Q', 'T', 'T', 4, connectFlags(), keepAliveBytes[0],
            keepAliveBytes[1] };
        lengthBytes = ByteUtils.encodeVariableLength(10 + payload.length);
        return ByteUtils.add(headerByte1, lengthBytes, variableHeader, payload);
      default:
        return new byte[0];
    }
  }

  /* mqtt 31 disallows client ids with length <1 or > 23 chars. */
  private void sanitizeClientIdFor31() {
    if (clientId.length() > 23) {
      logger.warn("Client id \"{}\" too long for mqtt 3.1. maximum length is 23 chars. Truncating.", clientId);
      clientId = clientId.substring(0, 23);
    }
  }

  byte connectFlags() {
    int i = 0;
    i = setBit(i, 7, username != null);
    i = setBit(i, 6, password != null);
    i = setBit(i, 5, wilLRetain);
    i = setBit(i, 4, willQos == 2);
    i = setBit(i, 3, willQos == 1);
    i = setBit(i, 2, willMessage != null);
    i = setBit(i, 1, cleanSession);
    return (byte) i;
  }

  @Override
  public byte[] payload() {
    GrowableBuffer b = new GrowableBuffer(40);
    b.putString(clientId);
    b.putString(willTopic);
    b.putString(willMessage);
    b.putString(username);
    b.putString(password);
    return b.getWrittenBytes();
  }
}
