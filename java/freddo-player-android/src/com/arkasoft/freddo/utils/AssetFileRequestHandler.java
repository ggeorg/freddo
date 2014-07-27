package com.arkasoft.freddo.utils;

import freddo.dtalk.DTalkService;
import freddo.dtalk.util.LOG;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.pm.PackageInfo;

import com.arkasoft.freddo.FdPlayerService;
import com.arkasoft.freddo.dtalk.netty4.server.HttpRequestHandler;
import com.arkasoft.freddo.dtalk.netty4.server.WebSocketServerHandler;

public class AssetFileRequestHandler implements HttpRequestHandler {
  private static final String TAG = LOG.tag(AssetFileRequestHandler.class);

  private final FdPlayerService service;
  private final String root;
  private final PackageInfo pInfo;

  public AssetFileRequestHandler(FdPlayerService service, String root, PackageInfo pInfo) {
    this.service = service;
    this.root = root;
    this.pInfo = pInfo;
  }

  @Override
  public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, String path) throws Exception {
    LOG.v(TAG, ">>> handleHttpRequest: %s", path);

    // Convert file separators.
    path = path.replace('/', File.separatorChar);

    if ("/".equals(path)) {
      path = "/index.html";
    }

    path = root + path;

    InputStream inputStream;
    try {
      inputStream = service.getAssets().open(path);
    } catch (IOException e) {
      LOG.w(TAG, "File not found: %s", path);
      WebSocketServerHandler.sendHttpResponse(ctx, req,
          new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
      return;
    }

    if (inputStream == null) {
      throw new NullPointerException("inputStream");
    }

    ByteBuf buffer;
    if (path.endsWith(".html")) {
      String html = readFile(inputStream);
      html = html.replace("%%WEBSOCKET_URL%%", DTalkService.getInstance().getLocalServiceAddress());
      html = html.replace("%%VERSION%%", pInfo != null ? pInfo.versionName : "0.0.0");
      buffer = Unpooled.copiedBuffer(html, CharsetUtil.UTF_8);
    } else {
      buffer = Unpooled.buffer();
      byte[] bytes = new byte[4096 * 4];
      int read = inputStream.read(bytes);
      while (read > 0) {
        buffer.writeBytes(bytes, 0, read);
        read = inputStream.read(bytes);
      }
    }

    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
    WebSocketServerHandler.setContentTypeHeader(response, path);
    HttpHeaders.setContentLength(response, buffer.readableBytes());
    if (HttpHeaders.isKeepAlive(req)) {
      response.headers().set(Names.CONNECTION, Values.KEEP_ALIVE);
    }

    WebSocketServerHandler.sendHttpResponse(ctx, req, response);
  }

  private String readFile(InputStream stream) {
    String tContents = "";

    try {
      int size = stream.available();
      byte[] buffer = new byte[size];
      stream.read(buffer);
      stream.close();
      tContents = new String(buffer, "UTF-8");
    } catch (IOException e) {
      // Handle exceptions here
    }

    return tContents;
  }

}
