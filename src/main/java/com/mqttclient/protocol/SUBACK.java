package com.mqttclient.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * A SUBACK Packet is sent by the Server to the Client to confirm receipt and
 * processing of a SUBSCRIBE Packet.
 * 
 * A SUBACK Packet contains a list of return codes, that specify the maximum QoS
 * level that was granted in each Subscription that was requested by the
 * SUBSCRIBE.
 */
public class SUBACK implements ServerMessage {
  public int messageId;
  public List<Integer> qosValues = new ArrayList<>();
  public int ln;

  /**
   * header byte
   * 
   * length (may be multibyte)
   * 
   * msg id (?)
   * 
   * qos list
   * 
   * @param bytes
   */
  public SUBACK(byte[] bytes) {
    ReadBuffer buf = new ReadBuffer(bytes);
    ln = buf.readLength();
    messageId = buf.readShort();
    byte[] remaining = buf.readBytes(ln - 2);// 2 for message id
    for (int i = 0; i < remaining.length; i++) {
      qosValues.add((remaining[i] & 0x03));
    }
  }

  @Override
  public int type() {
    return MessageType.SUBACK.value;
  }

  @Override
  public int byteLength() {
    return 1 + byteCountForLength(ln) + ln;
  }
}
