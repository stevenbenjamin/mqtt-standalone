package com.mqttclient;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

public class EventTrackerTest {
  ObjectId o1 = new ObjectId();
  ObjectId o2 = new ObjectId();
  ObjectId o3 = new ObjectId();
  ObjectId o4 = new ObjectId();

  @Test
  public void testEventTracking() {
    for (EventType t : EventType.values()) {
      logEvent(o1, t, 10);
    }
    for (EventType t : EventType.values()) {
      logEvent(o2, t, 5);
    }
    logEvent(o3, EventType.connect, 17);
    logEvent(o3, EventType.disconnect, 13);
    Assert.assertEquals(EventTracker.peek(o1).size(), 11);
    Assert.assertEquals(EventTracker.peek(o1).get("parseError"), 10);
    Assert.assertEquals(EventTracker.peek(o2).size(), 11);
    Assert.assertEquals(EventTracker.peek(o2).get("parseError"), 5);
    Assert.assertEquals(EventTracker.peek(o3).size(), 2);
    Assert.assertEquals(EventTracker.peek(o3).get("disconnect"), 13);
    Assert.assertEquals(EventTracker.peek(o4).size(), 0);
    Assert.assertEquals(EventTracker.retrieveAndClear(o1).size(), 11);
    Assert.assertEquals(EventTracker.retrieveAndClear(o2).get("connectionLost"), 5);
    Assert.assertEquals(EventTracker.retrieveAndClear(o3).get("connect"), 17);
    Assert.assertEquals(EventTracker.retrieveAndClear(o4).size(), 0);
    Assert.assertEquals(EventTracker.peek(o1).size(), 0);
    Assert.assertEquals(EventTracker.peek(o2).size(), 0);
    Assert.assertEquals(EventTracker.peek(o3).size(), 0);
    Assert.assertEquals(EventTracker.peek(o4).size(), 0);
  }

  private static void logEvent(ObjectId key, EventType t, int ct) {
    for (int i = 0; i < ct; i++) {
      EventTracker.logEvent(key, t);
    }
  }
}
