package com.arkasoft.freddo.dtalk.netty4.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;

import freddo.dtalk.util.LOG;

public abstract class AbstractDTalkNettyServer implements DTalkNettyServer {
  private static final String TAG = LOG.tag(AbstractDTalkNettyServer.class);

  public static final ChannelGroup ALL_CHANNELS = 
      new DefaultChannelGroup("FREDDO-DTALK-CHANNELS", GlobalEventExecutor.INSTANCE);
  
  protected final NettyConfig mNettyConfig;
  protected final ChannelInitializer<? extends Channel> mChannelInitializer;
  
  protected AbstractDTalkNettyServer(NettyConfig nettyConfig, ChannelInitializer<? extends Channel> channelInitializer) {
    mNettyConfig = nettyConfig;
    mChannelInitializer = channelInitializer;
  }
  
  @Override
  public ChannelInitializer<? extends Channel> getChannelInitializer() {
    return mChannelInitializer;
  }
  
  @Override
  public NettyConfig getNettyConfig() {
    return mNettyConfig;
  }
  
  @Override
  public InetSocketAddress getSocketAddress() {
    return mNettyConfig.getSocketAddress();
  }

  @Override
  public void startServer(int port) throws Exception {
    startServer(new InetSocketAddress(port));
  }

  @Override
  public void startServer(InetSocketAddress socketAddress) throws Exception {
    mNettyConfig.setSocketAddress(socketAddress);
    startServer();
  }

  @Override
  public void stopServer() throws Exception {
    LOG.i(TAG, ">>> stopServer: %s", getClass());
    
    try {
      ALL_CHANNELS.close().sync();
    } catch (InterruptedException e) {
      LOG.e(TAG, "Exception occured while waiting for channels to close.", e);
    } finally {
      if (mNettyConfig.getBossGroup() != null) {
        mNettyConfig.getBossGroup().shutdownGracefully();
      } else if (mNettyConfig.getWorkerGroup() != null) {
        mNettyConfig.getWorkerGroup().shutdownGracefully();
      }
    }
  }
  
}
