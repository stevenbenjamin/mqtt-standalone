package com.mqttclient.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.SocketChannel;

/** Single netty connection manager containing the netty bootstrap. */
public class TCPConnectionHandlerProvider extends ChannelHandlerProvider {
  public TCPConnectionHandlerProvider(MultithreadEventLoopGroup workerGroup, Class<? extends Channel> socketClass) {
    super(workerGroup, socketClass);
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new MqttChannelHandler());
      }
    });
  }
}