package com.mqttclient.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultithreadEventLoopGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.MqttCallback;
import com.mqttclient.ReconnectHandler;
import com.mqttclient.exception.MqttException;

/** Unified behavior for tcp and ssl connections. */
public abstract class ChannelHandlerProvider {
  protected static Logger logger = LoggerFactory.getLogger(ChannelHandlerProvider.class);
  Bootstrap bootstrap;

  public ChannelHandlerProvider(MultithreadEventLoopGroup workerGroup, Class<? extends Channel> socketClass) {
    bootstrap = new Bootstrap().group(workerGroup);
    bootstrap.channel(socketClass);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
    bootstrap.option(ChannelOption.TCP_NODELAY, false);
    bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    // bootstrap.option(ChannelOption.WRITE_SPIN_COUNT, 2);
    bootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
    bootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20 * 1000);
  }

  /**
   * Take no action on reconnect.
   * 
   * @throws MqttException
   */
  public MqttChannelHandler openChannel(String host, int port, int qos, MqttCallback callback) throws MqttException {
    return openChannel(host, port, qos, callback, new ReconnectHandler.NO_OP(host + ":" + port));
  }

  public MqttChannelHandler openChannel(String host, int port, int qos, MqttCallback callback,
      ReconnectHandler onClosed) throws MqttException {
    ChannelFuture f = bootstrap.connect(host, port);
    f.awaitUninterruptibly();
    assert f.isDone();
    if (f.isCancelled()) {
      throw new MqttException("couldn't connect to " + host + " " + port + ". Connect attempt was cancelled.");
    } else if (!f.isSuccess()) {
      throw new MqttException("couldn't connect to " + host + " " + port + ":" + f.cause().getMessage());
    } else {
      MqttChannelHandler h = f.channel().pipeline().get(MqttChannelHandler.class);
      if (h != null) {
        h.init(host + ":" + port, f.channel(), qos, callback, onClosed);
        return h;
      }
    }
    return null;
  }
}
