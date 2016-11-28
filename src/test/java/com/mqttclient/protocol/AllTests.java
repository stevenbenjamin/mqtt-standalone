package com.mqttclient.protocol;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.mqttclient.EventTrackerTest;
import com.mqttclient.PublicBrokerTest;
import com.mqttclient.protocol.util.ByteUtilsTest;

@RunWith(Suite.class)
@SuiteClasses({ ServerMessageProtocolTest.class, ClientMessageProtocolTest.class, ByteUtilsTest.class,
    ReadBufferTest.class, EventTrackerTest.class,
    // broker tests
    PublicBrokerTest.class })
public class AllTests {
}
