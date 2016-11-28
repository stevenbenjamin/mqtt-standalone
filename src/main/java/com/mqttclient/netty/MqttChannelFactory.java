package com.mqttclient.netty;

import io.netty.channel.Channel;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.MqttCallback;
import com.mqttclient.ReconnectHandler;
import com.mqttclient.exception.MqttException;
import com.mqttclient.exception.UnrecoverableMqttException;

public class MqttChannelFactory {
  Logger logger = LoggerFactory.getLogger(MqttChannelFactory.class);
  public ChannelHandlerProvider tcp;
  public ChannelHandlerProvider ssl;
  MultithreadEventLoopGroup workerGroup;
  Class<? extends Channel> channelClass;
  {
    init();
  }

  public Optional<MqttChannelHandler> openChannel(URI uri, int qos, MqttCallback callback, ReconnectHandler rh)
      throws MqttException {
    int port = getPort(uri);
    logger.info("Opening channel to uri {}: scheme = {}, will connect to {} @ port {}",
        new Object[] { uri, uri.getScheme(), uri.getHost() + uri.getPath(), port });
    ChannelHandlerProvider provider = getProvider(uri.getScheme());
    logger.debug("Using provider {} to open channel", provider);
    Optional<MqttChannelHandler> handler = Optional.ofNullable(provider.openChannel(uri.getHost() + uri.getPath(),
        port, qos, callback, rh));
    logger.debug("Will return handler {}", handler);
    return handler;
  }

  private static int getPort(URI uri) {
    String scheme = uri.getScheme();
    int port = uri.getPort();
    // missing ports in the uri are reported as -1
    if (port > 0) return port;
    if ("tcp".equalsIgnoreCase(scheme)) { // default tcp port
      return 1883;
    }
    if ("ssl".equalsIgnoreCase(scheme) || "tls".equalsIgnoreCase(scheme)) {
      return 8883; // default ssl port
    }
    return port;
  }

  private void init() {
    logger.info("Initializing connection manager");
    // workerGroup.setIoRatio(95);
    try {
      workerGroup = new EpollEventLoopGroup();
      channelClass = EpollSocketChannel.class;
      logger.warn("Epoll is available. Using Epoll instead of Nio selector.");
    } catch (Throwable e) {
      logger.warn("Epoll is not available. Using standard nio.");
      workerGroup = new NioEventLoopGroup();
      channelClass = NioSocketChannel.class;
    }
    tcp = new TCPConnectionHandlerProvider(workerGroup, channelClass);
    ssl = new SSLConnectionHandlerProvider(workerGroup, channelClass);
  }

  public void close() {
    workerGroup.shutdownGracefully();
  }

  private ChannelHandlerProvider getProvider(String scheme) throws UnrecoverableMqttException {
    if (scheme.equals("tcp")) {
      return tcp;
    } else if (scheme.equals("ssl") || scheme.equals("tls")) {
      return ssl;
    }
    throw new UnrecoverableMqttException(String.format("the scheme %s is not supported", scheme));
  }
}
