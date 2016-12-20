package com.eyelinecom.whoisd.sads2.wechat.resource;

import com.eyelinecom.whoisd.sads2.wechat.registry.WechatToken;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 16.12.16
 * Time: 14:01
 * To change this template use File | Settings | File Templates.
 */
public interface WeChatApi {

  void respond(String text, String userId);

  String waitForResponse(String botId, String userId);

  String uploadPhoto(WechatToken token, byte[] data);

  void sendImage(String mediaId, String userId, WechatToken token);

  boolean check(WechatToken token) throws Exception;
}
