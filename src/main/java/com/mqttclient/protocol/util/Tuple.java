package com.mqttclient.protocol.util;

import java.io.Serializable;

public class Tuple<A, B> implements Serializable {
  public final A a;
  public final B b;

  public Tuple(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public static <A, B> Tuple<A, B> tuple(A a, B b) {
    return new Tuple<>(a, b);
  }

  @Override
  public String toString() {
    return "a=" + a + " b=" + b;
  }

  @Override
  public int hashCode() {
    int result = 31 + ((a == null) ? 0 : a.hashCode());
    return 31 * result + ((b == null) ? 0 : b.hashCode());
  }

  public boolean contains(Object o) {
    return o != null && (o.equals(a) || o.equals(b));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Tuple<?, ?> other = (Tuple<?, ?>) obj;
    if (a == null) {
      if (other.a != null) {
        return false;
      }
    } else if (!a.equals(other.a)) {
      return false;
    }
    if (b == null) {
      if (other.b != null) {
        return false;
      }
    } else if (!b.equals(other.b)) {
      return false;
    }
    return true;
  }
}
