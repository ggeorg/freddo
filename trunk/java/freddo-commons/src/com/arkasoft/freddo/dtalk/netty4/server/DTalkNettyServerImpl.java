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
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;

import freddo.dtalk.util.LOG;

/**
 * A HTTP server which serves Web Socket requests.
 * <p>
 * This server will work with:
 * <ul>
 * <li>Safari 5+ (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 6-13 (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 14+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Chrome 16+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * <li>Firefox 7+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Firefox 11+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 */
public class DTalkNettyServerImpl extends AbstractDTalkNettyServer {
  private static final String TAG = LOG.tag(DTalkNettyServerImpl.class);

  private ServerBootstrap mBootstrap;

  public DTalkNettyServerImpl(NettyConfig nettyConfig, ChannelInitializer<? extends Channel> channelInitializer) {
    super(nettyConfig, channelInitializer);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void startServer() throws Exception {
    LOG.i(TAG, ">>> startServer: %s", getClass().getName());
    
    try {
      
      mBootstrap = new ServerBootstrap();
      
      Map<ChannelOption<?>, Object> options = mNettyConfig.getChannelOptions();
      if (options != null) {
        for(Entry<ChannelOption<?>, Object> entry : options.entrySet()) {
           mBootstrap.option((ChannelOption<Object>)entry.getKey(), entry.getValue());
        }
      }
      
      Channel ch = mBootstrap.group(mNettyConfig.getBossGroup(), mNettyConfig.getWorkerGroup())
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(getChannelInitializer())
          .bind(mNettyConfig.getSocketAddress())
          .sync().channel();
      
      ALL_CHANNELS.add(ch);
      
      // Set actual socket address...
      getNettyConfig().setSocketAddress((InetSocketAddress) ch.localAddress());
      LOG.i(TAG, "Web socket server started at port %d.", getSocketAddress().getPort());
      
    } catch(Exception e) {
      LOG.e(TAG, "Server start error: %s.\nShutting down.", e.getMessage());
      super.stopServer();
      throw e;
    }
  }
}
