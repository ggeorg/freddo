/*
 * Copyright 2013-2014 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.arkasoft.freddo.dtalk.netty4.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetSocketAddress;

import freddo.dtalk.DTalkService;
import freddo.dtalk.util.LOG;

public class WebSocketServer {
  private static final String TAG = LOG.tag(WebSocketServer.class);

  public static final String WEBSOCKET_PATH = "/dtalksrv";

  private Channel ch = null;

  private final DTalkService dtalkService;

  public WebSocketServer(DTalkService dtalkService) {
    this.dtalkService = dtalkService;
  }

  public InetSocketAddress getAddress() {
    return ch != null && ch.isOpen() ? (InetSocketAddress) ch.localAddress() : null;
  }

  public void startup(Runnable onStartUp) throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
              ChannelPipeline pipeline = ch.pipeline();
              pipeline.addLast("codec-http", new HttpServerCodec());
              pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
              pipeline.addLast("handler", new WebSocketServerHandler(dtalkService));
            }
          });
      
      final int port = dtalkService.getConfig().getPort();
      ch = bootstrap.bind(new InetSocketAddress(port)).sync().channel();
      InetSocketAddress address = (InetSocketAddress) ch.localAddress();
      LOG.i(TAG, "Web socket server started at port " + address.getPort() + '.');
      LOG.i(TAG, "Open your browser and navigate to http://localhost:%d/", address.getPort());

      if (onStartUp != null) {
        onStartUp.run();
      }

      // WebSocketServerHandler will close the connection when the client
      // responds to the CloseWebSocketFrame.
      ch.closeFuture().sync();
    } finally {
      LOG.i(TAG, "Shutdown gracefully...");
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public void shutdown() {
    LOG.v(TAG, "shutdown()");

    if (ch != null) {
      if (ch.isOpen()) {
        ch.flush();
        ch.close();
      }
      ch = null;
    }
  }

  @Deprecated
  public boolean isRunning() {
    synchronized (this) {
      return ch != null && ch.isOpen();
    }
  }

}