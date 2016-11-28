package com.mqttclient.protocol;

import com.mqttclient.protocol.util.ByteUtils;

public class ReadBuffer {
  byte[] bytes;
  int pos = 1;// skip header byte
  private int mark;

  public ReadBuffer(byte[] bytes) {
    this.bytes = bytes;
  }

  /**
   * Set a marker to count movement of position from. Call distance from mark to
   * get bytes traversed since the mark.
   */
  public void setMark() {
    mark = pos;
  }

  public int distanceFromMark() {
    return pos - mark;
  }

  byte readHeaderByte() {
    return bytes[0];
  }

  int readLength() {
    int multiplier = 1;
    int value = 0;
    int idx = 0;
    byte encodedByte = 0;
    do {
      encodedByte = bytes[(pos++)];
      idx++;
      value += (encodedByte & 127) * multiplier;
      multiplier *= 128;
    } while ((encodedByte & 128) != 0 && (idx < 4));
    return value;
  }

  String readString() {
    int length = ByteUtils.fromUnsignedShortBytes(readByte(), readByte());
    byte[] stringBytes = readBytes(length);
    return new String(stringBytes, ByteUtils.UTF8);
  }

  byte readByte() {
    return bytes[pos++];
  }

  byte[] readBytes(int ct) {
    byte[] out = new byte[ct];
    System.arraycopy(bytes, pos, out, 0, ct);
    pos += ct;
    return out;
  }

  @Override
  public String toString() {
    return "ReadBuffer [" + bytes.length + "bytes @pos=" + pos + "]";
  }

  int readShort() {
    return ByteUtils.fromUnsignedShortBytes(readByte(), readByte());
  }

  byte[] remaining() {
    byte[] out = new byte[bytes.length - pos];
    System.arraycopy(bytes, pos, out, 0, out.length);
    return out;
  }
}
