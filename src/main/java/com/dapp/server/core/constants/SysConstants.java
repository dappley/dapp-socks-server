package com.dapp.server.core.constants;

import io.netty.util.AttributeKey;

/**
 * @Author: suxinsen
 * @Date: 2019/12/17 14:10
 * @Description:
 */
public class SysConstants {
    public static AttributeKey<String> USER_NAME_KEY = AttributeKey.valueOf("UserName");
    public static AttributeKey<String> CLIENT_ID_KEY = AttributeKey.valueOf("ClientId");
    public static AttributeKey<String> DEVICE_KEY_ATTR = AttributeKey.valueOf("device_key");
}
