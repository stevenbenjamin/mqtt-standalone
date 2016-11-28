package com.mqttclient.protocol;

import java.util.ArrayList;
import java.util.List;

import com.mqttclient.protocol.util.ByteUtils;
import com.mqttclient.protocol.util.GrowableBuffer;
import com.mqttclient.protocol.util.Tuple;

/**
 * The SUBSCRIBE message allows a client to register an interest in one or more
 * topic names with the server. Messages published to these topics are delivered
 * from the server to the client as PUBLISH messages. The SUBSCRIBE message also
 * specifies the QoS level at which the subscriber wants to receive published
 * messages.
 */
public class SUBSCRIBE extends ClientMessage {
  public List<Tuple<String, Integer>> topicQosPairs = new ArrayList<>();
  public int messageId;

  public SUBSCRIBE(int messageId, boolean dup, List<Tuple<String, Integer>> pairs) {
    super(MessageType.SUBSCRIBE.value, 2);
    this.setDup(dup);
    this.messageId = messageId;
    topicQosPairs.addAll(pairs);
  }

  @Override
  public byte[] createMessage(Version version) {
    byte[] variableHeaderBytes = ByteUtils.unsignedShortBytes(messageId);
    GrowableBuffer b = new GrowableBuffer(topicQosPairs.size() * 6);// size
                                                                    // guesstimate
    for (Tuple<String, Integer> t : topicQosPairs) {
      b.putString(t.a);
      b.putByte(t.b.byteValue());
    }
    byte[] topicBytes = b.getWrittenBytes();
    byte[] lengthBytes = ByteUtils.encodeVariableLength(variableHeaderBytes.length + topicBytes.length);
    /*
     * note mqtt 3.11 spec says that the first byte must be 0,0,1,0. This
     * corresponds to dup == 0 in mqtt 3.1. (dup true would be 1,0,1,0). So
     * complete first byte must read 1, 0, 0, 0 | 0 , 0 , 1 , 0
     */
    if (version == Version._311) {
      setDup(false);
    }
    return ByteUtils.add(headerByte1, lengthBytes, variableHeaderBytes, topicBytes);
  }
}
