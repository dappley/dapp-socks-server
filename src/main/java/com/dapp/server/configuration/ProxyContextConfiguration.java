package com.dapp.server.configuration;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: suxinsen
 * @Date: 2019/12/17 14:10
 * @Description:
 */
public class ProxyContextConfiguration {

    /**
     * 用于存储一次代理的请求上下文
     */
    private ConcurrentHashMap<String, ChannelHandlerContext> socksContexts = new ConcurrentHashMap<>();
    /**
     * 用来存储局域网的代理长连接
     */
    private ConcurrentHashMap<String, ChannelHandlerContext> clientContexts = new ConcurrentHashMap<>();

    public void addClientContext(String clientId, ChannelHandlerContext context) {
        //If already has same id client, report error
        try {
            ChannelHandlerContext handlerContext = clientContexts.get(clientId);
            if (handlerContext != null) {
                handlerContext.close();
            }
            clientContexts.remove(clientId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        clientContexts.put(clientId, context);
    }

    public void removeClientContext(String clientId) {
        clientContexts.remove(clientId);
    }

    public ChannelHandlerContext getClientContext(String clientId) {
        return clientContexts.get(clientId);
    }

    public boolean addSocksContext(String connectId, ChannelHandlerContext context) {
        ChannelHandlerContext existContext = socksContexts.putIfAbsent(connectId, context);
        return existContext == null;
    }

    public void removeSocksContext(String connectId) {
        socksContexts.remove(connectId);
    }

    public ChannelHandlerContext getSocksContext(String connectId) {
        return socksContexts.get(connectId);
    }

}
