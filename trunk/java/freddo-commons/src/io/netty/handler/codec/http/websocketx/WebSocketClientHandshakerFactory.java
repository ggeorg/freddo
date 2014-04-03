/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http.websocketx;

import static io.netty.handler.codec.http.websocketx.WebSocketVersion.V00;
import static io.netty.handler.codec.http.websocketx.WebSocketVersion.V07;
import static io.netty.handler.codec.http.websocketx.WebSocketVersion.V08;
import static io.netty.handler.codec.http.websocketx.WebSocketVersion.V13;
import io.netty.handler.codec.http.HttpHeaders;

import java.net.URI;

/**
 * Creates a new {@link WebSocketClientHandshaker} of desired protocol version.
 */
public final class WebSocketClientHandshakerFactory {

    /**
     * Private constructor so this static class cannot be instanced.
     */
    private WebSocketClientHandshakerFactory() {
    }

    /**
     * Creates a new handshaker.
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath".
     *            Subsequent web socket frames will be sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server. Null if no sub-protocol support is required.
     * @param allowExtensions
     *            Allow extensions to be used in the reserved bits of the web socket frame
     * @param customHeaders
     *            Custom HTTP headers to send during the handshake
     */
    public static WebSocketClientHandshaker newHandshaker(
            URI webSocketURL, WebSocketVersion version, String subprotocol,
            boolean allowExtensions, HttpHeaders customHeaders) {
        return newHandshaker(webSocketURL, version, subprotocol, allowExtensions, customHeaders, 65536);
    }

    /**
     * Creates a new handshaker.
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath".
     *            Subsequent web socket frames will be sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server. Null if no sub-protocol support is required.
     * @param allowExtensions
     *            Allow extensions to be used in the reserved bits of the web socket frame
     * @param customHeaders
     *            Custom HTTP headers to send during the handshake
     * @param maxFramePayloadLength
     *            Maximum allowable frame payload length. Setting this value to your application's
     *            requirement may reduce denial of service attacks using long data frames.
     */
    public static WebSocketClientHandshaker newHandshaker(
            URI webSocketURL, WebSocketVersion version, String subprotocol,
            boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
        if (version == V13) {
            return new WebSocketClientHandshaker13(
                    webSocketURL, V13, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
        }
        if (version == V08) {
            return new WebSocketClientHandshaker08(
                    webSocketURL, V08, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
        }
        if (version == V07) {
            return new WebSocketClientHandshaker07(
                    webSocketURL, V07, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
        }
        if (version == V00) {
            return new WebSocketClientHandshaker00(
                    webSocketURL, V00, subprotocol, customHeaders, maxFramePayloadLength);
        }

        throw new WebSocketHandshakeException("Protocol version " + version.toString() + " not supported.");
    }
}
