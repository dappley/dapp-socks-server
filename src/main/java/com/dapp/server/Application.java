package com.dapp.server;

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.core.handler.http.HttpRequestHandler;
import com.dapp.server.core.handler.pc.DefaultRequestHandler;
import com.dapp.server.core.handler.pc.decoder.ClientMessageDecoder;
import com.dapp.server.core.handler.pc.encoder.ClientMessageEncoder;
import com.dapp.server.core.handler.socks.Socks5CommandRequestHandler;
import com.dapp.server.core.handler.socks.Socks5InitialRequestHandler;
import com.dapp.server.core.handler.socks.SocksPasswordAuthorizeRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: suxinsen
 * @create_time: 2020/8/12 15:52
 **/
public class Application {

    private Logger log = LoggerFactory.getLogger(getClass());
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(4);
    private static ProxyContextConfiguration proxyContextConfig = new ProxyContextConfiguration();

    public void start() {
        try {
            ServerBootstrap httpStrap = new ServerBootstrap();
            httpStrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, false)
                    .childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();

                            socketChannel.pipeline()
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(1024 * 1024 * 1024))
                                    .addLast(new CorsHandler(corsConfig))
                                    .addLast(new HttpRequestHandler(proxyContextConfig));
                        }
                    });

            ServerBootstrap pcBootstrap = httpStrap.clone();
            pcBootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new ClientMessageEncoder())
                                    .addLast(new ClientMessageDecoder())
                                    .addLast(new DefaultRequestHandler(proxyContextConfig));
                        }
                    });

            ServerBootstrap socksBootstrap = httpStrap.clone();
            socksBootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            //channel timeout handler
                            ch.pipeline().addLast(new IdleStateHandler(3, 30, 0))
                                    //Socks5Message ByteBuf
                                    .addLast(Socks5ServerEncoder.DEFAULT)
                                    .addLast(new Socks5InitialRequestDecoder())
                                    .addLast(new Socks5InitialRequestHandler())
                                    .addLast(new Socks5PasswordAuthRequestDecoder())
                                    .addLast(new SocksPasswordAuthorizeRequestHandler());
                            //socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                            //Socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestHandler(proxyContextConfig));
                        }
                    });

            ChannelFuture channelFuture = httpStrap.bind(8081).sync();
            pcBootstrap.bind(8082).sync();
            socksBootstrap.bind(8083).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Application().start();
    }

}
