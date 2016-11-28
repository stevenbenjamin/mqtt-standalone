package com.mqttclient.netty;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingHandler {
  protected static Logger logger = LoggerFactory.getLogger(PingHandler.class);
  protected static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2,
      new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor ex) {
          logger.warn("rejected task {} because this executor {} is overloaded. Current active count = {}",
              new Object[] { r, ex, ex.getActiveCount() });
        }
      });
  static {
    // stopgap until we figure out the GC issue
    executor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        System.gc();
      }
    }, 1, 1, TimeUnit.HOURS);
  }
  int keepAlive;
  int pingsSent;
  int consecutiveErrors;
  MqttChannelHandler handler;
  boolean closed = false;

  public void close() {
    closed = true;
  }

  public PingHandler(MqttChannelHandler handler, int keepAlive) {
    this.keepAlive = keepAlive;
    this.handler = handler;
    scheduleNext();
  }

  void scheduleNext() {
    if (!closed) {
      executor.schedule(handler::pingreq, keepAlive, TimeUnit.SECONDS);
    }
  }

  void error() {
    consecutiveErrors++;
    if (consecutiveErrors > 5) {
      closed = true;
      logger.warn("connection lost:%s", handler.connectString);
      handler.callback.onConnectionLost();
      // close the underlying channel.
      handler.close();
    }
  }

  void success() {
    consecutiveErrors = 0;
  }
}
