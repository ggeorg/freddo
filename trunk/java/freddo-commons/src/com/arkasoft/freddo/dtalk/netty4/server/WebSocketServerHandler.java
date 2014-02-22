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

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.DTalkService;
import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.util.LOG;

/**
 * Handles handshakes and messages
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
  private static final String TAG = "WebSocketServerHandler";

  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int HTTP_CACHE_SECONDS = 60;

  private static volatile Map<String, HttpRequestHandler> sRequestHandlers;

  private final DTalkService dtalkService;
  
  public WebSocketServerHandler(DTalkService dtalkService) {
    this.dtalkService = dtalkService;
  }

  public static void addRequestHandler(String uri, HttpRequestHandler handler) {
    if (sRequestHandlers == null) {
      sRequestHandlers = new ConcurrentHashMap<String, HttpRequestHandler>();
    }
    sRequestHandlers.put(uri, handler);
  }

  public static HttpRequestHandler removeRequestHandler(String uri) {
    return (sRequestHandlers != null) ? sRequestHandlers.remove(uri) : null;
  }

  protected static HttpRequestHandler getRequestHandler(String uri) {
    return (sRequestHandlers != null) ? sRequestHandlers.get(uri) : null;
  }

  private final Map<Channel, WebSocketServerHandshaker> handshakers = new ConcurrentHashMap<Channel, WebSocketServerHandshaker>();

  private FileRequestHandler fileHandler = null;

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof FullHttpRequest) {
      handleHttpRequest(ctx, (FullHttpRequest) msg);
    } else if (msg instanceof WebSocketFrame) {
      handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    // Handle a bad request.
    if (!req.getDecoderResult().isSuccess()) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
      return;
    }

    // Allow only GET methods.
    if (req.getMethod() != GET) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED));
      return;
    }

    String path = satitizeUri(req.getUri());
    if (path == null) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FORBIDDEN));
      return;
    }

    LOG.d(TAG, "Handling: %s", path);

    // Handshake
    if (path.equals(WebSocketServer.WEBSOCKET_PATH)) {
      WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
      WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
      if (handshaker == null) {
        LOG.w(TAG, "Unsupported version: %s", req);
        WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
      } else {
        LOG.d(TAG, "Handshake: %s", req);
        Channel channel = ctx.channel();

        // register anonymous channel
        dtalkService.addChannel(channel);

        handshakers.put(channel, handshaker);
        handshaker.handshake(ctx.channel(), req);
      }
    } else {
      String[] parts = path.split("/");
      String handlerKey = parts.length <= 2 ? "/" : "/" + parts[1];
      HttpRequestHandler handler = getRequestHandler(handlerKey);
      LOG.d(TAG, "'%s' ============================= %s", handlerKey, handler);
      if (handler != null) {
        handler.handleHttpRequest(ctx, req, path);
        return;
      }

      if (fileHandler == null) {
        fileHandler = new FileRequestHandler(System.getProperty("user.dir"));
      }

      fileHandler.handleHttpRequest(ctx, req, path);
    }
  }

  private String satitizeUri(String uri) {
    // Decode the path.
    try {
      uri = URLDecoder.decode(uri, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      try {
        uri = URLDecoder.decode(uri, "ISO-8859-1");
      } catch (UnsupportedEncodingException e1) {
        throw new Error();
      }
    }

    if (!uri.startsWith("/")) {
      return null;
    }

    final int endIndex = uri.indexOf('?');
    if (endIndex > 0) {
      uri = uri.substring(0, endIndex);
    }

    return uri;
  }

  private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    LOG.v(TAG, "Got %s", frame);

    final Channel channel = ctx.channel();

    // Check for closing frame
    if (frame instanceof CloseWebSocketFrame) {
      dtalkService.removeChannel(channel);

      WebSocketServerHandshaker handshaker = handshakers.remove(channel);
      if (handshaker != null) {
        handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
      }

      return;
    }

    if (frame instanceof PingWebSocketFrame) {
      ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
      return;
    }

    if (!(frame instanceof TextWebSocketFrame)) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
    }

    // Got message...
    String message = ((TextWebSocketFrame) frame).text();
    LOG.v(TAG, "Got messages: %s", message);

    try {

      JSONObject jsonMsg = new JSONObject(message);

      String localServiceName = dtalkService.getLocalServiceInfo().getName();

      String to = jsonMsg.optString(MessageEvent.KEY_TO, null);
      String from = jsonMsg.optString(MessageEvent.KEY_FROM, null);
      // String body = jsonMsg.optString(MessageEvent.KEY_BODY, null);

      // clean up message
      jsonMsg.remove(MessageEvent.KEY_FROM);
      jsonMsg.remove(MessageEvent.KEY_TO);

      // if (body != null) {

      JSONObject jsonBody = jsonMsg; // new JSONObject(body);

      if (!jsonBody.has(MessageEvent.KEY_BODY_SERVICE)) {
        JSONObject _jsonBody = jsonBody;
        jsonBody = new JSONObject();
        jsonBody.put(MessageEvent.KEY_BODY_SERVICE, "dtalk.InvalidMessage");
        jsonBody.put(MessageEvent.KEY_BODY_PARAMS, _jsonBody);
      }

      if (to != null && !to.equals(localServiceName)) {

        //
        // outgoing message
        //

        LOG.d(TAG, "OutgoingMessageEvent to: %s", to);
        MessageBus.sendMessage(new OutgoingMessageEvent(to, jsonBody));

      } else {

        //
        // incoming message
        //

        if (from == null) { // anonymous message
          from = String.valueOf(channel.hashCode());
        } else {
          // replace anonymous channel
          dtalkService.addChannel(from, channel);
        }

        LOG.d(TAG, "IncomingMessageEvent from: %s", from);
        MessageBus.sendMessage(new IncomingMessageEvent(from, jsonBody));
      }

      // }

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // ctx.channel().write(new TextWebSocketFrame(request.toUpperCase()));
  }

  /**
   * Sets the Date header for the HTTP response
   * 
   * @param response HTTP response
   */
  public static void setDateHeader(FullHttpResponse response) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    Calendar time = new GregorianCalendar();
    response.headers().set(DATE, dateFormatter.format(time.getTime()));
  }

  /**
   * Sets the Date and Cache headers for the HTTP Response
   * 
   * @param response HTTP response
   * @param fileToCache file to extract content type
   */
  public static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    // Date header
    Calendar time = new GregorianCalendar();
    response.headers().set(DATE, dateFormatter.format(time.getTime()));

    // Add cache headers
    time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
    response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
    response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
    response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
  }

  /**
   * Sets the content type header for the HTTP Response
   * 
   * @param response HTTP response
   * @param file file name to extract content type
   */
  public static void setContentTypeHeader(HttpResponse response, String file) {
    response.headers().set(CONTENT_TYPE, MimetypesFileTypeMap.getTypeForFileName(file)[0]);
  }

  public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
    // Generate an error page if response getStatus code is not OK (200).
    if (res.getStatus().code() != 200) {
      ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
      res.content().writeBytes(buf);
      buf.release();
      setContentLength(res, res.content().readableBytes());
    }

    // Send the response and close the connection if necessary.
    ChannelFuture f = ctx.channel().writeAndFlush(res);
    if (!isKeepAlive(req) || res.getStatus().code() != 200) {
      f.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }

  public static String getWebSocketLocation(FullHttpRequest req) {
    return "ws://" + req.headers().get(HOST) + WebSocketServer.WEBSOCKET_PATH;
  }
}
