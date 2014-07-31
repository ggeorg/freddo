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
package com.arkasoft.freddo.dtalk.netty4.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.DTalkConnection;

import freddo.dtalk.util.LOG;

public class DTalkNettyClientConnection implements DTalkConnection {
  private static final String TAG = LOG.tag(DTalkNettyClientConnection.class);
  
  private static EventLoopGroup group = null;

  private final URI uri;
  
  private Channel mChannel = null;

  public DTalkNettyClientConnection(URI uri) {
    this.uri = uri;
  }

  public void connect() {
    assert uri != null : "URI is null";
    LOG.v(TAG, ">>> connect: %s", uri);
    
    if (group == null) {
      group = new NioEventLoopGroup();
    }

    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      String protocol = uri.getScheme();
      if (!"ws".equals(protocol)) {
        throw new IllegalArgumentException("Unsupported protocol: " + protocol);
      }

      HttpHeaders customHeaders = new DefaultHttpHeaders();
      // customHeaders.add("MyHeader", "MyValue");

      // Connect with V13 (RFC 6455 aka HyBi-17).
      final DTalkNettyClientHandler handler =
          new DTalkNettyClientHandler(this, WebSocketClientHandshakerFactory.newHandshaker(uri,
              WebSocketVersion.V13, null, false, customHeaders));

      b.group(group)
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
              ChannelPipeline pipeline = ch.pipeline();
              pipeline.addLast("http-codec", new HttpClientCodec());
              pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
              pipeline.addLast("ws-handler", handler);
            }
          });

      LOG.d(TAG, "WebSocket Client connecting...");
      mChannel = b.connect(uri.getHost(), uri.getPort()).addListener(new GenericFutureListener<ChannelFuture>() {
        @Override
        public void operationComplete(ChannelFuture f) throws Exception {
          if (!f.isSuccess()) {
            LOG.w(TAG, "Connection error", f.cause());
          }
        }
      }).sync().channel();

      LOG.d(TAG, "Handshake...");
      handler.handshakeFuture().sync();

      // Ping
      // System.out.println("WebSocket Client sending ping");
      // ch.writeAndFlush(new PingWebSocketFrame(Unpooled.copiedBuffer(new
      // byte[] {1, 2, 3, 4, 5,
      // 6})));

      // Close
      // System.out.println("WebSocket Client sending close");
      // ch.writeAndFlush(new CloseWebSocketFrame());

    } catch (Exception e) {
      LOG.e(TAG, "Error: %s", e.getMessage());
      // e.printStackTrace();

      group.shutdownGracefully();
      group = null;
    }
  }
  
  @Override
  public Object getId() {
    return null;
  }

  @Override
  public Object sendMessage(JSONObject jsonMsg) throws JSONException {
    return mChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg.toString()));
  }

  @Override
  public void onMessage(JSONObject message) throws JSONException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void close() {
    // TODO we need to send close frame.
    mChannel.close();
  }

  @Override
  public boolean isOpen() {
    return mChannel.isOpen();
  }

}