package com.dapp.server.core.handler.http.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @author: suxinsen
 * @create_time: 2021/9/14 14:48
 **/
public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    private HttpTransport socketTransport;
    private FullHttpRequest fullHttpRequest;
    private ChannelHandlerContext sourceCtx;

    public HttpClientHandler(HttpTransport socketTransport, FullHttpRequest fullHttpRequest, ChannelHandlerContext sourceCtx) {
        this.sourceCtx = sourceCtx;
        this.fullHttpRequest = fullHttpRequest;
        this.socketTransport = socketTransport;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(fullHttpRequest);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        sourceCtx.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        socketTransport.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.channelInactive(ctx);
        socketTransport.close();
    }

}
