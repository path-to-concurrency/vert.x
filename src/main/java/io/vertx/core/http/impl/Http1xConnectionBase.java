/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.stream.ChunkedFile;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.GoAway;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.impl.ConnectionBase;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
abstract class Http1xConnectionBase<S extends WebSocketImplBase<S>> extends ConnectionBase implements io.vertx.core.http.HttpConnection {

  protected S webSocket;
  protected long bytesWritten;

  Http1xConnectionBase(VertxInternal vertx, ChannelHandlerContext chctx, ContextInternal context) {
    super(vertx, chctx, context);
  }

  void handleWsFrame(WebSocketFrame msg) {
    reportBytesRead(getBytes(msg));
    WebSocketImplBase<?> w;
    synchronized (this) {
      w = webSocket;
    }
    if (w != null) {
      w.context.schedule(msg, w::handleFrame);
    }
  }

  @Override
  public Http1xConnectionBase closeHandler(Handler<Void> handler) {
    return (Http1xConnectionBase) super.closeHandler(handler);
  }

  @Override
  public Http1xConnectionBase exceptionHandler(Handler<Throwable> handler) {
    return (Http1xConnectionBase) super.exceptionHandler(handler);
  }

  @Override
  public HttpConnection goAway(long errorCode, int lastStreamId, Buffer debugData) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support GOAWAY");
  }

  @Override
  public HttpConnection goAwayHandler(@Nullable Handler<GoAway> handler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support GOAWAY");
  }

  @Override
  public HttpConnection shutdownHandler(@Nullable Handler<Void> handler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support GOAWAY");
  }

  @Override
  public void shutdown(long timeout, Handler<AsyncResult<Void>> handler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support GOAWAY");
  }

  @Override
  public Future<Void> shutdown(long timeoutMs) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support GOAWAY");
  }

  @Override
  public Http2Settings settings() {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support SETTINGS");
  }

  @Override
  public Future<Void> updateSettings(Http2Settings settings) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support SETTINGS");
  }

  @Override
  public HttpConnection updateSettings(Http2Settings settings, Handler<AsyncResult<Void>> completionHandler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support SETTINGS");
  }

  @Override
  public Http2Settings remoteSettings() {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support SETTINGS");
  }

  @Override
  public HttpConnection remoteSettingsHandler(Handler<Http2Settings> handler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support SETTINGS");
  }

  @Override
  public HttpConnection ping(Buffer data, Handler<AsyncResult<Buffer>> pongHandler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support PING");
  }

  @Override
  public HttpConnection pingHandler(@Nullable Handler<Buffer> handler) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support PING");
  }

  @Override
  public Future<Buffer> ping(Buffer data) {
    throw new UnsupportedOperationException("HTTP/1.x connections don't support PING");
  }

  @Override
  protected void reportsBytesWritten(Object msg) {
    if (msg instanceof HttpObject || msg instanceof FileRegion || msg instanceof ChunkedFile) {
      bytesWritten += getBytes(msg);
    } else if (msg instanceof WebSocketFrame) {
      // Only report WebSocket messages since HttpMessage are reported by streams
      reportBytesWritten(getBytes(msg));
    }
  }

  static long getBytes(WebSocketFrame obj) {
    return obj.content().readableBytes();
  }

  static long getBytes(Object obj) {
    if (obj == null) {
      return 0;
    } else if (obj instanceof Buffer) {
      return ((Buffer) obj).length();
    } else if (obj instanceof ByteBuf) {
      return ((ByteBuf) obj).readableBytes();
    } else if (obj instanceof HttpContent) {
      return ((HttpContent) obj).content().readableBytes();
    } else if (obj instanceof WebSocketFrame) {
      return getBytes((WebSocketFrame) obj);
    } else if (obj instanceof FileRegion) {
      return ((FileRegion) obj).count();
    } else if (obj instanceof ChunkedFile) {
      ChunkedFile file = (ChunkedFile) obj;
      return file.endOffset() - file.startOffset();
    } else {
      return -1;
    }
  }
}
