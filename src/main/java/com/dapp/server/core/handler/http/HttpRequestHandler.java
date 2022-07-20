package com.dapp.server.core.handler.http;

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.core.handler.http.transport.HttpTransport;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: suxinsen
 * @create_time: 2020/8/12 15:28
 **/
@Slf4j
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

    private HttpTransport httpTransport;
    private ProxyContextConfiguration proxyContextConfig;

    public HttpRequestHandler(ProxyContextConfiguration proxyContextConfig) {
        this.proxyContextConfig = proxyContextConfig;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object req)
            throws Exception {

        if (req instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) req;
            HttpHeaders headers = request.headers();
            String host = headers.get("Host");
            log.info("Get a request host from {}", host);
            String[] hostStrings = host.split("\\.");
            String proxyId = hostStrings[0];
            if (proxyContextConfig.getClientContext(proxyId) == null) {
                ctx.writeAndFlush("Connect not found");
            } else {
                httpTransport = new HttpTransport();
                httpTransport.setHost("localhost");
                httpTransport.setPort(80);
                httpTransport.setProxyId(proxyId);
                httpTransport.setFullHttpRequest(request);
                httpTransport.setSourceCtx(ctx);
                httpTransport.start();
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("new a http connect");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        httpTransport.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.channelInactive(ctx);
        httpTransport.close();
    }

}
