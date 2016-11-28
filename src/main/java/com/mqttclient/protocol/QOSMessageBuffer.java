package com.mqttclient.protocol;

import java.util.concurrent.TimeUnit;

import com.mqttclient.netty.MqttChannelHandler;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class QOSMessageBuffer {
  Cache<Integer, PUBLISH> cache;

  public QOSMessageBuffer(final MqttChannelHandler handler) {
    cache = CacheBuilder.newBuilder().maximumSize(2).expireAfterWrite(2, TimeUnit.SECONDS)
        .removalListener(new RemovalListener<Integer, PUBLISH>() {
          @Override
          public void onRemoval(RemovalNotification<Integer, PUBLISH> rn) {
            if (rn.getCause() == RemovalCause.EXPIRED) {
              handler.republish(rn.getValue());
            }
          }
        }).build();
  }

  public void buffer(PUBLISH msg) {
    cache.put(msg.messageId, msg);
  }

  public void remove(PUBACK msg) {
    cache.invalidate(msg.messageId);
  }
}
