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

import freddo.dtalk.util.LOG;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * Handles handshakes and messages
 */
public class DTalkNettyServerHandler extends SimpleChannelInboundHandler<Object> {
  private static final String TAG = LOG.tag(DTalkNettyServerHandler.class);

  public static final String DTALKSRV_PATH = "/dtalksrv";

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    LOG.v(TAG, ">>> channelRead0: %s", msg);
    if (msg instanceof FullHttpRequest) {
      handleHttpRequest(ctx, (FullHttpRequest) msg);
    } else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    } else {
      LOG.e(TAG, "Unknown message type: %s", msg.getClass());
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    LOG.v(TAG, ">>> channelReadComplete");
    ctx.flush();
  }
  
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.e(TAG, ">>> exceptionCaught: ", cause);
    ctx.close();
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
    // Handle a bad request.
    if (!req.getDecoderResult().isSuccess()) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
      return;
    }

    if (!req.getUri().startsWith(DTALKSRV_PATH)) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
      return;
    }

    //
    // WebSocket Handshake
    //

    WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
    WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
    if (handshaker == null) {
      LOG.e(TAG, "Unsupported version: {}", req);
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
    } else {
      handshaker.handshake(ctx.channel(), req);
      
      String conId = "0x" + Integer.toHexString(ctx.channel().hashCode());
//      DTalkNettyConnection messageSender = new DTalkNettyConnection(conId, mBroadcaster, ctx.channel(), handshaker);
//      DTalkConnectionRegistry.getInstance().register(conId, messageSender);
    }
  }

  @SuppressWarnings("deprecation")
  private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    LOG.v(TAG, ">>> handleWebSocketFrame: %s", frame.getClass());
    
    if (frame instanceof PingWebSocketFrame) {
      ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
      return;
    }

    if (frame instanceof PongWebSocketFrame) {
      return;
    }
    
    // Check for closing frame
//    if (frame instanceof CloseWebSocketFrame) {
//      LOG.d(TAG, "Handling CloseWebSocketFrame...");
//      DTalkNettyConnection connection = (DTalkNettyConnection) DTalkConnectionRegistry.getInstance().remove(ctx.channel());
//      if (connection != null) {
//        connection.mHandshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
//        return;
//      }
//    } else if (frame instanceof TextWebSocketFrame) {
//      DTalkConnection connection = DTalkConnectionRegistry.getInstance().get(ctx.channel());
//      if (connection != null) {
//        try {
//          JSONObject jsonMsg = new JSONObject(((TextWebSocketFrame) frame).text());
//          connection.onMessage(jsonMsg);
//        } catch (JSONException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//        }
//        return;
//      }
//    }
    
    LOG.e(TAG, "Unknown message: {}", frame.getClass());
  }

  private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
    // Generate an error page if response getStatus code is not OK (200).
    if (res.getStatus().code() != 200) {
      ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
      res.content().writeBytes(buf);
      buf.release();
      HttpHeaders.setContentLength(res, res.content().readableBytes());
    }

    // Send the response and close the connection if necessary.
    ChannelFuture f = ctx.channel().writeAndFlush(res);
    if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
      f.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private static String getWebSocketLocation(FullHttpRequest req) {
    LOG.v(TAG, ">>> getWebSocketLocation");
    String location = req.headers().get(HttpHeaders.Names.HOST) + DTALKSRV_PATH;
    // if (DTalkServer.SSL) {
    // return "wss://" + location;
    // } else {
    return "ws://" + location;
    // }
  }
}
