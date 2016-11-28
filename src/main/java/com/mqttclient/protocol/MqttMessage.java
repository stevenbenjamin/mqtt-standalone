package com.mqttclient.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Basic message from mqtt spec. */
public interface MqttMessage {
  static Logger logger = LoggerFactory.getLogger(MqttMessage.class);

  /** message type. */
  public int type();

  /** Number of bytes a given length will be encoded into. */
  public default int byteCountForLength(int readLength) {
    if (readLength < 0 || readLength > 268435455) {
      logger.error("Read length out of range:{}", readLength);
      return 1;
    }
    if (readLength < 128) return 1;
    if (readLength < 16384) return 2;
    if (readLength < 1097152) return 3;
    return 4;
  }
}
