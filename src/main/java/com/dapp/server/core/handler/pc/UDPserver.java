package com.dapp.server.core.handler.pc;

/**
 * @description:
 * @param: null
 * @return:
 * @author Sun
 * @date: 2022/3/28 9:55
 */

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.protobuf.DeviceProxyMessageProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;


public class UDPserver {



    /**
     * @description:
     * @param: port
     * @return: void
     * @author Sun
     * @date: 2022/3/28 9:56
     */
    public void run(int port) throws  Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new UDPServerHandler());
            b.bind(port).sync().channel().closeFuture().await();
        }finally {
            group.shutdownGracefully();
        }


    }

    public static void main(String[] args) throws  Exception{
        int port = 8011;
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        }catch (NumberFormatException e){
            e.printStackTrace();
        }
        new UDPserver().run(port);
    }

}
