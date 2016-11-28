package com.mqttclient.protocol;


public enum MessageType {
  /** Reserved */
  Unused_1(0, Direction.FORBIDDEN),
  /** Client request to connect to Server */
  CONNECT(1, Direction.CLIENT_TO_SERVER),
  /** Connect acknowledgment */
  CONNACK(2, Direction.SERVER_TO_CLIENT),
  /** Publish message */
  PUBLISH(3, Direction.BOTH),
  /** Publish acknowledgment */
  PUBACK(4, Direction.BOTH),
  /** Publish received (assured delivery part 1) */
  PUBREC(5, Direction.BOTH),
  /** Publish release (assured delivery part 2) */
  PUBREL(6, Direction.BOTH),
  /** Publish complete (assured delivery part 3) */
  PUBCOMP(7, Direction.BOTH),
  /** Publish complete (assured delivery part 3) */
  /** Client subscribe request */
  SUBSCRIBE(8, Direction.CLIENT_TO_SERVER),
  /** Subscribe acknowledgment */
  SUBACK(9, Direction.SERVER_TO_CLIENT),
  /** Unsubscribe request */
  UNSUBSCRIBE(10, Direction.CLIENT_TO_SERVER),
  /** Unsubscribe acknowledgment */
  UNSUBACK(11, Direction.SERVER_TO_CLIENT),
  /** PING request */
  PINGREQ(12, Direction.CLIENT_TO_SERVER),
  /** PING response */
  PINGRESP(13, Direction.SERVER_TO_CLIENT),
  /** Client is disconnecting */
  DISCONNECT(14, Direction.CLIENT_TO_SERVER),
  /** Reserved */
  Unused_2(15, Direction.FORBIDDEN);
  public final int value;
  public final Direction direction;

  private MessageType(int i, Direction d) {
    value = i;
    direction = d;
  }

  public enum Direction {
    FORBIDDEN, CLIENT_TO_SERVER, SERVER_TO_CLIENT, BOTH;
  }
}
