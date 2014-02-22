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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;

import java.net.URI;

import javaxx.jmdns.JmDNS;
import javaxx.jmdns.ServiceInfo;

import org.json.JSONObject;

import com.arkasoft.freddo.util.Base64;
import com.arkasoft.freddo.util.LOG;

public class WebPresenceService {
  private static final String TAG = LOG.tag(WebPresenceService.class);
  
  private Channel ch = null;

  public void publishService(URI uri, JmDNS jmdns, ServiceInfo serviceInfo) {
    LOG.v(TAG, ">>> publishService: %s", serviceInfo);
    LOG.v(TAG, ">>> publishService: %s", uri);
    
    EventLoopGroup group = new NioEventLoopGroup();
    
    try {
      Bootstrap b = new Bootstrap();
      String protocol = uri.getScheme();
      if (!"ws".equals(protocol)) {
        throw new IllegalArgumentException("Unsupported protocol: " + protocol);
      }

      JSONObject presence = new JSONObject();
      presence.put("id", serviceInfo.getName());
      presence.put("type", "who knows");
      presence.put("ws", "ws://" +jmdns.getHostName() + ":" + serviceInfo.getPort() + WebSocketServer.WEBSOCKET_PATH);

      HttpHeaders customHeaders = new DefaultHttpHeaders();
      customHeaders.add("presence", presence.toString());

      String pr = Base64.encodeBytes(presence.toString().getBytes());
      uri = URI.create(uri.toString() + "?presence=" + pr);

      // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or
      // V00. If you change it to V00, ping is not supported and remember to
      // change HttpResponseDecoder to WebSocketHttpResponseDecoder in the
      // pipeline.
      final WebPresenceClientHandler handler = new WebPresenceClientHandler(uri, customHeaders);

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

      LOG.d(TAG, "WebSocket Client connecting");
      ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
      handler.handshakeFuture().sync();
    } catch (Exception e) {
      e.printStackTrace();
      
      group.shutdownGracefully();
    }
  }

  public void unpublishService() {
    if (ch != null) {
      ch.close();
      ch = null;
    }
  }

  public class WebPresenceClientHandler extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebPresenceClientHandler(URI uri, HttpHeaders customHeaders) {
      this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, customHeaders);
    }

    public ChannelFuture handshakeFuture() {
      return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      System.out.println("WebSocket Client disconnected!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      Channel ch = ctx.channel();
      if (!handshaker.isHandshakeComplete()) {
        handshaker.finishHandshake(ch, (FullHttpResponse) msg);
        System.out.println("WebSocket Client connected!");
        handshakeFuture.setSuccess();
        return;
      }

      if (msg instanceof FullHttpResponse) {
        FullHttpResponse response = (FullHttpResponse) msg;
        throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content="
            + response.content().toString(CharsetUtil.UTF_8) + ')');
      }

      WebSocketFrame frame = (WebSocketFrame) msg;
      if (frame instanceof TextWebSocketFrame) {
        TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
        System.out.println("WebSocket Client received message: " + textFrame.text());
      } else if (frame instanceof PongWebSocketFrame) {
        System.out.println("WebSocket Client received pong");
      } else if (frame instanceof CloseWebSocketFrame) {
        System.out.println("WebSocket Client received closing");
        ch.close();
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();

      if (!handshakeFuture.isDone()) {
        handshakeFuture.setFailure(cause);
      }

      ctx.close();
    }
  }
}
