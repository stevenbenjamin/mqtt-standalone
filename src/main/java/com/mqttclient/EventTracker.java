package com.mqttclient;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;

/**
 * Messages currently tracked are:
 * 
 * connected/disconnected/sent/received
 * 
 * Failures: connection failure/parse error/send error
 *
 * @author stevenbenjamin
 *
 */
public class EventTracker {
  static class Timer {
    String msg;
    private static int eventCt = EventType.values().length;
    int[] eventCounts = new int[eventCt];
    long timestamp = System.currentTimeMillis();

    public void logEvent(EventType event) {
      eventCounts[event.ordinal()]++;
    }

    HashMap<String, Object> toMap() {
      HashMap<String, Object> m = new HashMap<>(8);
      for (int i = 0; i < eventCounts.length; i++) {
        if (eventCounts[i] > 0) {
          m.put(EventType.values()[i].name(), eventCounts[i]);
        }
      }
      if (msg != null) m.put("message", msg);
      return m;
    }

    public double rate(EventType event) {
      return (eventCounts[event.ordinal()] * 1.0) / (System.currentTimeMillis() - timestamp);
    }
  }

  public static double rate(ObjectId id, EventType event) {
    Timer t = events.get(id);
    return t == null ? 0 : t.rate(event);
  }

  private static ConcurrentHashMap<ObjectId, Timer> events = new ConcurrentHashMap<>();

  public static void disableTracking(ObjectId id) {
    events.remove(id);
  }

  public static void logEvent(ObjectId id, EventType event) {
    Timer t = events.get(id);
    if (t == null) {
      t = new Timer();
      events.put(id, t);
    }
    t.logEvent(event);
  }

  public static void logMessage(ObjectId id, String msg) {
    Timer t = events.get(id);
    if (t == null) {
      t = new Timer();
      events.put(id, t);
    }
    t.msg = msg;
  }

  public static HashMap<String, Object> peek(ObjectId id) {
    Timer t = events.get(id);
    return t == null ? new HashMap<>() : t.toMap();
  }

  public static HashMap<String, Object> retrieveAndClear(ObjectId id) {
    Timer t = events.remove(id);
    return t == null ? new HashMap<>() : t.toMap();
  }
}
