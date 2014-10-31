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
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.util.Set;

import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkService;
import freddo.dtalk.events.WebPresenceEvent;
import freddo.dtalk.util.Base64;
import freddo.dtalk.util.LOG;
import freddo.dtalk.zeroconf.ZConfServiceInfo;

public class WebPresenceService {
	private static final String TAG = LOG.tag(WebPresenceService.class);

	private static EventLoopGroup group = null;

	private Channel ch = null;

	public void publish(URI uri, ZConfServiceInfo serviceInfo) {
		LOG.v(TAG, ">>> publishService: %s", serviceInfo);
		LOG.v(TAG, ">>> publishService: %s", uri);

		if (group == null) {
			group = new NioEventLoopGroup();
		}

		try {
			Bootstrap b = new Bootstrap();
			String protocol = uri.getScheme();
			if (!"ws".equals(protocol)) {
				throw new IllegalArgumentException("Unsupported protocol: " + protocol);
			}

			JSONObject presence = new JSONObject();

			Set<String> pNames = serviceInfo.getTxtRecord().keySet();
			for (String property : pNames) {
				presence.put(property, serviceInfo.getTxtRecordValue(property));
			}

			presence.put(DTalk.KEY_NAME, serviceInfo.getServiceName());
			presence.put(DTalk.KEY_SERVER, DTalkService.getWebSocketAddress(serviceInfo));
			//presence.put(DTalk.KEY_PORT, serviceInfo.getPort());

			// One way to register is with custom header...
			HttpHeaders customHeaders = new DefaultHttpHeaders();
			customHeaders.add("presence", presence.toString());

			// ... the other way is with request parameter.
			String pr = Base64.encodeBytes(presence.toString().getBytes());
			uri = URI.create(uri.toString() + "?presence=" + pr);

			final WebPresenceClientHandler handler = new WebPresenceClientHandler(uri, customHeaders);
			b.group(group)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast("http-codec", new HttpClientCodec());
							pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
							pipeline.addLast("ws-handler", handler);
						}
					});

			LOG.d(TAG, "WebSocket Client connecting");
			ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
			handler.handshakeFuture().sync();
		} catch (Exception e) {
			LOG.e(TAG, "Error: %s", e.getMessage());
			if (group != null) {
				group.shutdownGracefully();
				group = null;
			}
			MessageBus.sendMessage(new WebPresenceEvent(false));
		}
	}

	public void unpublish() {
		LOG.d(TAG, ">>> unpublish");
		if (ch != null) {
			ch.close();
			ch = null;
		}
	}

	public class WebPresenceClientHandler extends SimpleChannelInboundHandler<Object> {
		private final String TAG = LOG.tag(WebPresenceClientHandler.class);

		private final WebSocketClientHandshaker handshaker;
		private ChannelPromise handshakeFuture;

		public WebPresenceClientHandler(URI uri, HttpHeaders customHeaders) {
			handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, customHeaders);
		}

		public ChannelFuture handshakeFuture() {
			return handshakeFuture;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			LOG.d(TAG, ">>> handlerAdded: %s", ctx);

			handshakeFuture = ctx.newPromise();
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			LOG.d(TAG, ">>> channelActive: %s", ctx);

			handshaker.handshake(ctx.channel());

			// Notify listeners that the WebPresence connection is open.
			MessageBus.sendMessage(new WebPresenceEvent(true));
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			LOG.d(TAG, ">>> channelInactive");

			// Notify listeners that the WebPresence connection is closed.
			MessageBus.sendMessage(new WebPresenceEvent(false));
		}

		@Override
		public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
			LOG.v(TAG, ">>> channelRead0: %s", msg);

			Channel ch = ctx.channel();
			if (!handshaker.isHandshakeComplete()) {
				
				try {
					handshaker.finishHandshake(ch, (FullHttpResponse) msg);
				} catch (WebSocketHandshakeException e) {
					handshakeFuture.setFailure(e);
					return;
				}
				
				LOG.d(TAG, "WebSocket Client connected!");
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
				LOG.d(TAG, "WebSocket Client received pong");
			} else if (frame instanceof CloseWebSocketFrame) {
				LOG.d(TAG, "WebSocket Client received closing");
				ch.close();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			LOG.e(TAG, ">>> exceptionCaught: %s (error: %s)", ctx, cause);
			// cause.printStackTrace();

			if (!handshakeFuture.isDone()) {
				handshakeFuture.setFailure(cause);
				ctx.close();
			}

			// ctx.close();
		}
	}
}
