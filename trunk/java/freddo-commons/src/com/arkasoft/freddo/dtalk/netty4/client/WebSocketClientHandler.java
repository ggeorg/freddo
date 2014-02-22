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

import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.util.LOG;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
  private static final String TAG = "WebSocketClientHandler";

  private final WebSocketClientHandshaker handshaker;
  private ChannelPromise handshakeFuture;

  public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
    this.handshaker = handshaker;
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

      String message = textFrame.text();

      try {

        JSONObject jsonMsg = new JSONObject(message);

//        String localServiceName = DTalkService.getInstance().getLocalServiceInfo().getName();
//
//        String to = jsonMsg.optString(MessageEvent.KEY_TO, null);
        String from = jsonMsg.optString(MessageEvent.KEY_FROM, null);
        // String body = jsonMsg.optString(MessageEvent.KEY_BODY, null);

        // if (body != null) {

        JSONObject jsonBody = jsonMsg; // new JSONObject(body);

        if (!jsonBody.has(MessageEvent.KEY_BODY_SERVICE)) {
          JSONObject _jsonBody = jsonBody;
          jsonBody = new JSONObject();
          jsonBody.put(MessageEvent.KEY_BODY_SERVICE, "dtalk.InvalidMessage");
          jsonBody.put(MessageEvent.KEY_BODY_PARAMS, _jsonBody);
        }

        //
        // incoming message
        //

        LOG.d(TAG, "IncomingMessageEvent to: %s", from);
        MessageBus.sendMessage(new IncomingMessageEvent(from, jsonBody));

        // }

      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

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