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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NettyConfig {

  private static final int sPortNumber = 0;

  private Map<ChannelOption<?>, Object> mChannelOptions;

  private NioEventLoopGroup mBossGroup;
  private NioEventLoopGroup mWorkerGroup;

  private int mBossThreadCount;
  private int mWorkerThreadCount;

  private InetSocketAddress mSocketAddress;

  protected ChannelInitializer<? extends Channel> mChannelInitializer;

  public NettyConfig() {
    this(null);
  }

  public NettyConfig(InetSocketAddress socketAddress) {
    mSocketAddress = socketAddress;
  }

  public Map<ChannelOption<?>, Object> getChannelOptions() {
    synchronized (this) {
      return mChannelOptions != null ? Collections.unmodifiableMap(mChannelOptions) : null;
    }
  }

  public void setChannelOptions(Map<ChannelOption<?>, Object> channelOptions) {
    synchronized (this) {
      mChannelOptions = new HashMap<ChannelOption<?>, Object>(channelOptions);
    }
  }

  public NioEventLoopGroup getBossGroup() {
    if (null == mBossGroup) {
      synchronized (this) {
        if (null == mBossGroup) {
          if (0 >= mBossThreadCount) {
            mBossGroup = new NioEventLoopGroup();
          } else {
            mBossGroup = new NioEventLoopGroup(mBossThreadCount);
          }
        }
      }
    }
    return mBossGroup;
  }

  // public void setBossGroup(NioEventLoopGroup bossGroup) {
  // synchronized(this) {
  // mBossGroup = bossGroup;
  // }
  // }

  public NioEventLoopGroup getWorkerGroup() {
    if (null == mWorkerGroup) {
      synchronized (this) {
        if (null == mWorkerGroup) {
          if (0 >= mWorkerThreadCount) {
            mWorkerGroup = new NioEventLoopGroup();
          } else {
            mWorkerGroup = new NioEventLoopGroup(mWorkerThreadCount);
          }
        }
      }
    }
    return mWorkerGroup;
  }

  // public void setWorkerGroup(NioEventLoopGroup workerGroup) {
  // synchronized(this) {
  // mWorkerGroup = workerGroup;
  // }
  // }

  public int getBossThreadCount() {
    return mBossThreadCount;
  }

  // public void setBossThreadCount(int bossThreadCount) {
  // mBossThreadCount = bossThreadCount;
  // }

  public int getWorkerThreadCount() {
    return mWorkerThreadCount;
  }

  // public void setWorkerThreadCount(int workerThreadCount) {
  // mWorkerThreadCount = workerThreadCount;
  // }

  public synchronized InetSocketAddress getSocketAddress() {
    if (null == mSocketAddress) {
      synchronized (this) {
        if (null == mSocketAddress) {
          mSocketAddress = new InetSocketAddress(sPortNumber);
        }
      }
    }
    return mSocketAddress;
  }

  public void setSocketAddress(InetSocketAddress socketAddress) {
    synchronized (this) {
      mSocketAddress = socketAddress;
    }
  }

}
