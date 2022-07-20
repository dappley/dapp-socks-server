package com.dapp.server.core.handler.socks;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Admin
 */
public interface ChannelListener {

	public void inActive(ChannelHandlerContext ctx);
	
	public void active(ChannelHandlerContext ctx);
}
