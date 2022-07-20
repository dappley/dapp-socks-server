package com.dapp.server.core.handler.http.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.proxy.Socks5ProxyHandler;
import lombok.Data;

import java.net.InetSocketAddress;

/**
 * @author: suxinsen
 * @create_time: 2021/9/14 14:48
 **/
@Data
public class HttpTransport {

    private String host;
    private Integer port;
    private FullHttpRequest fullHttpRequest;
    private String proxyId;
    private ChannelHandlerContext sourceCtx;
    private Channel channel;
    private NioEventLoopGroup group;

    public HttpTransport() {}

    public void start(){
        group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel channel)
                                throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new Socks5ProxyHandler(new InetSocketAddress("localhost", 8083), proxyId, "password"));
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 1024));
                            pipeline.addLast(new HttpContentDecompressor());
                            pipeline.addLast(new HttpClientHandler( HttpTransport.this, fullHttpRequest, sourceCtx));
                        }
                    });
            ChannelFuture future = bootstrap.connect(host, port);
            this.channel = future.channel();
        } catch (Exception e) {
            e.printStackTrace();
            group.shutdownGracefully();
        }
    }

    public void close() {
        try {
            channel.close();
            group.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
