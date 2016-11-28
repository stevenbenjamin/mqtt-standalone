package com.mqttclient;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.exception.BadProtocolVersion;
import com.mqttclient.exception.MqttException;
import com.mqttclient.exception.UnrecoverableMqttException;
import com.mqttclient.protocol.Version;

/**
 * handle auto-reconnect logic with a limited number of time-increasing
 * attempts.
 */
public class ReconnectHandler implements Runnable {
  protected static Logger logger = LoggerFactory.getLogger(ReconnectHandler.class);
  // reconnect handling
  int reconnectAttempts = 0;
  private static final int MAX_RECONNECT_ATTEMPTS = 5;
  private static final int BACKOFF_SECONDS = 30;
  private Connection c;
  /* default retry @ version 311. */
  private Version nextVersion = Version._311;
  /* Make sure we never schedule if we're already waiting for a reconnect. */
  private boolean reconnectIsScheduled;

  public ReconnectHandler(Connection c) {
    this.c = c;
  }

  @Override
  public void run() {
    reconnectIsScheduled = false;
    try {
      c.channelHandler = Connection.channelFactory.openChannel(//
          c.fmqtt.asURI(),//
          c.fmqtt.getQos() == null ? 0 : c.fmqtt.getQos(),//
          c.callback,//
          this);
      if (c.channelHandler.isPresent()) {
        c.channelHandler.get().version = nextVersion;
        c.connectAndSubscribe();
      }
      if (!c.isCurrentlyConnected()) {
        scheduleReconnect();
      } else {
        // reset on success.
        reconnectAttempts = 0;
      }
    } catch (UnrecoverableMqttException e) {
      logger.warn("Unrecoverable error reconnecting to {}, {}. Giving up", c.fmqtt.getId(), c.fmqtt.getUri());
      c.callback.onConnectFail(e);
    } catch (URISyntaxException e) {
      logger.warn("Bad URI, giving up {}, {}. Giving up", c.fmqtt.getId(), c.fmqtt.getUri());
      c.callback.onConnectFail(new UnrecoverableMqttException("Bad URI Specification in task: " + c.fmqtt.getUri()));
    } catch (BadProtocolVersion e) {
      // if we fail on bad protocol, try to reconnect w 3.1
      nextVersion = Version._31;
      logger.info("connection failure to " + c.fmqtt.getUri() + " with bad protocol. Will retry with mqtt 3.1");
      run();
    } catch (MqttException e) {
      c.callback.onConnectFail(e);
      scheduleReconnect();
    }
  }

  public void scheduleReconnect() {
    if (reconnectIsScheduled) {
      logger.debug("Reconnect is already scheduled for {}: ignoring request for reconnect.", c.fmqtt.getId());
      return;
    }
    logger.info("Scheduling reconnect for {}", c.fmqtt.getId());
    if (c.channelHandler.isPresent()) c.channelHandler.get().close();
    if ((reconnectAttempts++) < MAX_RECONNECT_ATTEMPTS) {
      long wait = new Double(Math.pow(2, reconnectAttempts) * BACKOFF_SECONDS).longValue();
      Connection.logger.info("scheduling next reconnect in {} seconds", wait);
      reconnectIsScheduled = true;
      Connection.executor.schedule(this, wait, TimeUnit.SECONDS);
    } else {
      c.callback.onConnectFail(new UnrecoverableMqttException("Couldn't connect: maximum retry count exceeded."));
    }
  }

  public static class NO_OP extends ReconnectHandler {
    private String connectString;

    public NO_OP(String connectString) {
      super(null);
      this.connectString = connectString;
    }

    @Override
    public void run() {
      logger.warn("lost connection to {}", connectString);
    }

    @Override
    public void scheduleReconnect() {
    }
  }
}