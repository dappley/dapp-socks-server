package com.dapp.server.core.handler.socks;

import com.dapp.server.core.constants.SysConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Admin
 */
@Slf4j
public class SocksPasswordAuthorizeRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    public SocksPasswordAuthorizeRequestHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        log.info("password authorize request socks5 connect : " + msg);
        if(msg.decoderResult().isFailure()) {
            log.info("connect is not socks5 protocol");
            ctx.fireChannelRead(msg);
        } else {
            if(msg.version().equals(SocksVersion.SOCKS5)) {
                ctx.channel().attr(SysConstants.USER_NAME_KEY).set(msg.username());
                DefaultSocks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
                ctx.writeAndFlush(response);
            }
        }
    }
}
