package com.mqttclient.protocol.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Wrapper around a byte array providing growability. */
public class GrowableBuffer {
  public byte[] raw;
  private int written;
  public final int offset;
  public static final Charset UTF8 = StandardCharsets.UTF_8;

  public GrowableBuffer(int initialCapacity) {
    raw = new byte[initialCapacity];
    offset = 0;
  }

  @Override
  public String toString() {
    return "GrowableBuffer [written=" + written + ", offset=" + offset + " raw=" + Arrays.toString(raw) + "]";
  }

  /** A growable buffer that will ignore the prefixed offset number bytes. */
  public GrowableBuffer(byte[] bytes, int offset) {
    this.raw = bytes;
    written = bytes.length - offset;
    this.offset = offset;
  }

  public void putString(String s) {
    if (s != null) {
      putRaw(ByteUtils.toLengthPrefixedString(s));
    }
  }

  public void putByte(byte b) {
    if ((++written) >= (raw.length - offset)) {
      insureSize(written);
    }
    raw[written - 1] = b;
  }

  /** Convenience for (truncating) write. */
  public void writeIByte(int i) {
    putByte((byte) i);
  }

  public byte[] raw() {
    return raw;
  }

  public byte atIndex(int idx) {
    return raw[idx + offset];
  }

  public void putRaw(byte[] bts) {
    insureSize(written + bts.length);
    System.arraycopy(bts, 0, raw, written + offset, bts.length);
    written = written + bts.length;
  }

  /**
   * Bytes with any trailing (unused) space or offsets removed. This may involve
   * an array copy.
   */
  public byte[] getWrittenBytes() {
    if (offset == 0 && (written == raw.length)) {
      return raw;
    }
    byte[] out = new byte[written];
    System.arraycopy(raw, offset, out, 0, written);
    return out;
  }

  /** Number of bytes written. */
  public int getNumBytesWritten() {
    return written;
  }

  /** Current array capacity. */
  public int getCapacity() {
    return raw.length - offset;
  }

  private void insureSize(int i) {
    if (i <= (raw.length - offset)) return;
    byte[] b2 = new byte[i + offset];
    System.arraycopy(raw, 0, b2, 0, raw.length);
    raw = b2;
  }
}
