package com.dapp.server.core.handler.socks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import java.util.concurrent.TimeUnit;
public class AppChannelInializer extends ChannelInitializer {
    private static final EventExecutorGroup EXECUTOR_GROUOP = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 2);
    static {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    TrafficCounter trafficCounter = trafficHandler.trafficCounter();
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    final long totalRead = trafficCounter.cumulativeReadBytes();
                    final long totalWrite = trafficCounter.cumulativeWrittenBytes();
                    System.out.println("total read: " + (totalRead >> 10) + " KB");
                    System.out.println("total write: " + (totalWrite >> 10) + " KB");
                    System.out.println("流量监控: " + System.lineSeparator() + trafficCounter);
                }
            }
        }).start();
    }
    private static final GlobalTrafficShapingHandler trafficHandler = new GlobalTrafficShapingHandler(EXECUTOR_GROUOP, 30, 30);
    @Override
    protected void initChannel(final Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(trafficHandler);
        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpResponseEncoder());
//        p.addLast(EXECUTOR_GROUOP, new HelloHandler());
    }
}