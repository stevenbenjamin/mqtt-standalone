package com.mqttclient.protocol;

import java.util.ArrayList;
import java.util.List;

import com.mqttclient.protocol.util.ByteUtils;
import com.mqttclient.protocol.util.GrowableBuffer;

/**
 * An UNSUBSCRIBE message is sent by the client to the server to unsubscribe
 * from named topics.
 */
public class UNSUBSCRIBE extends ClientMessage {
  List<String> topics = new ArrayList<>();
  public int messageId;

  public UNSUBSCRIBE(int messageId, List<String> topics) {
    super(MessageType.UNSUBSCRIBE.value, 2);
    this.messageId = messageId;
    this.topics.addAll(topics);
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] variableHeader = ByteUtils.unsignedShortBytes(messageId);
    GrowableBuffer b = new GrowableBuffer(topics.size() * 6);// size
                                                             // guesstimate
    for (String topic : topics) {
      b.putString(topic);
    }
    byte[] topicBytes = b.getWrittenBytes();
    byte[] lengthFieldBytes = ByteUtils.encodeVariableLength(topicBytes.length + variableHeader.length);
    return ByteUtils.add(headerByte1, lengthFieldBytes, variableHeader, b.getWrittenBytes());
  }
}
