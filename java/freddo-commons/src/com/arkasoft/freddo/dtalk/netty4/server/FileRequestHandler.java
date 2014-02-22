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
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.regex.Pattern;

import com.arkasoft.freddo.util.LOG;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;

public class FileRequestHandler implements HttpRequestHandler {
  private static final String TAG = LOG.tag(FileRequestHandler.class);
  
  private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
  
  private final String root;
  
  public FileRequestHandler(String root) {
    this.root = root;
  }

  public String getRoot() {
    return root;
  }
  
  protected String realPath(String path) {
    return getRoot() + path;
  }

  @SuppressWarnings("resource")
  @Override
  public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, String path) throws Exception {
    LOG.v(TAG, ">>> handleHttpRequest: %s (%s)", path, root);
    
    // Convert file separators.
    path = path.replace('/', File.separatorChar);

    // Simplistic dumb security check.
    // You will have to do something serious in the production environment.
    if (path.contains(File.separator + '.') ||
        path.contains('.' + File.separator) ||
        path.startsWith(".") || path.endsWith(".") || INSECURE_URI.matcher(path).matches()) {
      WebSocketServerHandler.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FORBIDDEN));
      return;
    }

    // Convert to real path.
    path = realPath(path.startsWith(File.separator) ? path : File.separator + path);
    
    LOG.d(TAG, "Real path: %s", path);

    File file = new File(path);
    if (file.isHidden() || !file.exists() || file.isDirectory()) {
      LOG.w(TAG, "Not found: %s", file);
      WebSocketServerHandler.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
      return;
    }

    if (!file.isFile()) {
      WebSocketServerHandler.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FORBIDDEN));
      return;
    }

    // Serve file... (TODO: Cache?)
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(file, "r");
    } catch (FileNotFoundException fnfe) {
      LOG.d(TAG, "File %s not found, sending: 404", file.getName());
      WebSocketServerHandler.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
      return;
    }
    long fileLength = raf.length();
    
    LOG.d(TAG, "file length: %d", fileLength);

    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
    setContentLength(response, fileLength);
    WebSocketServerHandler.setContentTypeHeader(response, path);
    WebSocketServerHandler.setDateAndCacheHeaders(response, file);
    if (isKeepAlive(req)) {
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Write the initial line and the header.
    ctx.write(response);

    // Write the content.
    ChannelFuture sendFileFuture;
    
    // if (useSendFile) {
    sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0L, fileLength), ctx.newProgressivePromise());
    // } else {
    // sendFileFuture =
    // ctx.write(new ChunkedFile(raf, 0, fileLength, 8192),
    // ctx.newProgressivePromise());
    // }
    
    sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
      @Override
      public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        if (total < 0) { // total unknown
          LOG.d(TAG, "Transfer progress: " + progress);
        } else {
          LOG.d(TAG, "Transfer progress: " + progress + " / " + total);
        }
      }

      @Override
      public void operationComplete(ChannelProgressiveFuture future) throws Exception {
        LOG.d(TAG, "Transfer complete.");
        future.channel().flush();
      }
    });

    // Write the end marker
    ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

    // Decide whether to close the connection or not.
    if (!isKeepAlive(req)) {
      // Close the connection when the whole content is written out.
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

}
