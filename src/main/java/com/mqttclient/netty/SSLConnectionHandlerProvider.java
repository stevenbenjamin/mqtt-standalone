package com.mqttclient.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.OpenSslClientContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mqttclient.exception.UnrecoverableMqttException;

/** Single netty connection manager containing the netty bootstrap. */
public class SSLConnectionHandlerProvider extends ChannelHandlerProvider {
  protected static Logger logger = LoggerFactory.getLogger(SSLConnectionHandlerProvider.class);
  /** if open ssl fails once, stop trying. */
  protected static boolean tryOpenSsl = true;

  public SSLConnectionHandlerProvider(MultithreadEventLoopGroup workerGroup, Class<? extends Channel> socketClass) {
    super(workerGroup, socketClass);
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @SuppressWarnings("deprecation")
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        SslContext sslCtx = null;
        if (tryOpenSsl) {
          try {
            sslCtx = new OpenSslClientContext(InsecureTrustManagerFactory.INSTANCE);
            logger.warn("Openssl succcessfully loaded");
          } catch (java.lang.UnsatisfiedLinkError e) {
            logger.warn("Can't use openssl. Fall back to jdk ssl:" + e);
            tryOpenSsl = false;
          }
        }
        if (sslCtx == null) {
          sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }
        SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
        sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<Channel>>() {
          @Override
          public void operationComplete(Future f) throws Exception {
            if (!f.isSuccess()) {
              logger.info("Client side SSL Handshake failed: {}", f.cause());
              ch.pipeline().fireExceptionCaught(
                  new UnrecoverableMqttException("Client side SSL Handshake failed: " + f.cause()));
            } else {
              logger.info("Client side SSL handshake OK");
            }
          }
        });
        ch.pipeline().addLast(sslHandler);
        ch.pipeline().addLast(new MqttChannelHandler());
      }
    });
  }
}
