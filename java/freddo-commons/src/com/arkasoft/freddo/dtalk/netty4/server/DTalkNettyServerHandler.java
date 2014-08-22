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

import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkService;
import freddo.dtalk.events.DTalkChannelClosedEvent;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.MessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.util.LOG;
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
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

import com.arkasoft.freddo.dtalk.DTalkConnectionRegistry;
import com.arkasoft.freddo.dtalk.DTalkServer;
import com.arkasoft.freddo.messagebus.MessageBus;

/**
 * Handles handshakes and messages
 */
public class DTalkNettyServerHandler extends SimpleChannelInboundHandler<Object> {
	private static final String TAG = LOG.tag(DTalkNettyServerHandler.class);
	
  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int HTTP_CACHE_SECONDS = 60;

	private HttpRequestHandler mFileHandler;

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

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		// Handle a bad request.
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}

		final String path = satitizeUri(req.getUri());
		if (path == null) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
			return;
		}

		if (path.startsWith(DTalkServer.DTALKSRV_PATH)) {
			
			//
			// WebSocket handshake.
			//
			
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
			WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
			if (handshaker == null) {
				LOG.e(TAG, "Unsupported version: %s", req);
				WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			} else {
				LOG.d(TAG, "Handshake: %s", req);

				Channel channel = ctx.channel();
				handshaker.handshake(channel, req);

				DTalkConnectionRegistry.getInstance().register(channel, new DTalkNettyServerConnection(channel, handshaker));
			}

		} else {
			
			//
			// HTTP Request handling.
			//
			
			String[] parts = path.split("/");
			String handlerKey = parts.length <= 2 ? "/" : "/" + parts[1];

			HttpRequestHandler handler = getRequestHandler(handlerKey);
			LOG.d(TAG, "Handler for '%s': %s", handlerKey, handler != null ? handler.getClass().getName() : "UNKNOWN");

			if (handler != null) {
				handler.handleHttpRequest(ctx, req, path);
				return;
			}

			// Allow only GET methods.
			if (req.getMethod() != GET) {
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED));
				return;
			}

			// Default HTTP request handler.
			if (mFileHandler == null) {
				mFileHandler = new FileRequestHandler(System.getProperty("user.dir"));
			}
			mFileHandler.handleHttpRequest(ctx, req, path);
			
		}
	}

	private String satitizeUri(String uri) {

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
		LOG.v(TAG, ">>> handleWebSocketFrame: %s", frame.getClass());
		
		final Channel channel = ctx.channel();

	// Check for ping frame
		if (frame instanceof PingWebSocketFrame) {
			channel.write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

	// Check for pong frame
		if (frame instanceof PongWebSocketFrame) {
			return;
		}

    final DTalkNettyServerConnection conn = (DTalkNettyServerConnection) DTalkConnectionRegistry.getInstance().get(channel);
    if (conn == null) {
      // should not happen...
      LOG.e(TAG, "*** THIS SHOULD NOT HAPPEN ***");
      LOG.e(TAG, "Connection not registered!!!");
      // TODO close channel and return.
      return;
    }

    // Check for closing frame
    if (frame instanceof CloseWebSocketFrame) {
      DTalkConnectionRegistry.getInstance().remove(channel);
      if (conn.getId() != null) {
        // Notify listeners that channel was closed...
        MessageBus.sendMessage(new DTalkChannelClosedEvent((String)conn.getId()));
      }
      
      @SuppressWarnings("deprecation")
      WebSocketServerHandshaker handshaker = conn.mHandshaker;
      if (handshaker != null) {
        handshaker.close(channel, (CloseWebSocketFrame) frame.retain());
      }
      conn.close();
      return;
    }
    
    if (!(frame instanceof TextWebSocketFrame)) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
    }

    // Got message...
    String message = ((TextWebSocketFrame) frame).text();
    LOG.d(TAG, "Got message: %s", message);

    try {

      JSONObject jsonMsg = new JSONObject(message);

      String localServiceName = DTalkService.getInstance().getLocalServiceInfo().getServiceName();

      String to = jsonMsg.optString(DTalk.KEY_TO, null);
      String from = jsonMsg.optString(DTalk.KEY_FROM, null);
      String service = jsonMsg.optString(DTalk.KEY_BODY_SERVICE, null);

      // clean up message
      jsonMsg.remove(MessageEvent.KEY_FROM);
      jsonMsg.remove(MessageEvent.KEY_TO);

      // if (body != null) {

      JSONObject jsonBody = jsonMsg; // new JSONObject(body);

      if (service == null) {
        LOG.w(TAG, "Invalid Message");
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

        if (from == null) {
          // anonymous message...
          if (service != null && !service.startsWith("$")) {
            // if its not a broadcast message add 'from'...
            from = String.format("%s%d", DTalkService.LOCAL_CHANNEL_PREFIX, channel.hashCode());
          }
          // else: see DTalkDispatcher for message forwarding.
        }
        
        if (from != null) {
          // Set the connection id.
          conn.setId(from);

          // create double entry in DTalkConnectionRegistry...
          DTalkConnectionRegistry.getInstance().register(from, conn);
        }

        LOG.d(TAG, "IncomingMessageEvent from: %s", from);
        MessageBus.sendMessage(new IncomingMessageEvent(from, jsonBody));
      }

      // }

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
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
    response.headers().set(CONTENT_TYPE, Mimetypes.getTypeForFileName(file)[0]);
  }

	public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
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
		String location = req.headers().get(HttpHeaders.Names.HOST) + DTalkServer.DTALKSRV_PATH;
		// if (DTalkServer.SSL) {
		// return "wss://" + location;
		// } else {
		return "ws://" + location;
		// }
	}

	// -----------------------------------------------------------------------
	// REQUEST HANDLER API (TODO has to be abstract)
	// -----------------------------------------------------------------------

	private static volatile Map<String, HttpRequestHandler> sRequestHandlers;

	public static void addRequestHandler(String uri, HttpRequestHandler handler) {
		if (sRequestHandlers == null) {
			sRequestHandlers = new ConcurrentHashMap<String, HttpRequestHandler>();
		}
		sRequestHandlers.put(uri, handler);
	}

	protected static HttpRequestHandler getRequestHandler(String uri) {
		return (sRequestHandlers != null) ? sRequestHandlers.get(uri) : null;
	}

}
