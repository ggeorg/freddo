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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.concurrent.GenericFutureListener;

import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.dtalk.DTalkConnection;

import freddo.dtalk.util.AsyncCallback;
import freddo.dtalk.util.LOG;

public class DTalkNettyServerConnection implements DTalkConnection {
	private static final String TAG = LOG.tag(DTalkNettyServerConnection.class);

	private final Channel mChannel;

	@Deprecated
	final WebSocketServerHandshaker mHandshaker;

	private Object mId;

	public DTalkNettyServerConnection(Channel channel, WebSocketServerHandshaker handshaker) {
		mChannel = channel;

		// Used internally to handle CloseWebSocketFrame events (see:
		// DTalkChannelInbountHandler).
		mHandshaker = handshaker;
	}

	@Override
	public Object getId() {
		return mId;
	}

	void setId(Object id) {
		mId = id;
	}

	@Override
	public void connect() {
		// do nothing
	}

	@Override
	public void sendMessage(JSONObject jsonMsg, final AsyncCallback<Boolean> callback) {
		LOG.v(TAG, ">>> sendMessage: (%s) %s", mChannel, jsonMsg);
		try {
			mChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg == null ? "" : jsonMsg.toString())).addListener(new GenericFutureListener<ChannelFuture>() {
				@Override
				public void operationComplete(ChannelFuture channelFuture) throws Exception {
					callback.onSuccess(channelFuture.isSuccess());
				}
			});
		} catch (Throwable caught) {
			callback.onFailure(caught);
		}
	}

	@Override
	public void onMessage(JSONObject msg) throws JSONException {
		LOG.v(TAG, ">>> onMessage: (%s) %s", mChannel, msg);

	}

	@Override
	public void close() {
		if (mChannel != null) {
			mChannel.close();
		}
	}

	@Override
	public boolean isOpen() {
		return mChannel != null && mChannel.isOpen();
	}

}
