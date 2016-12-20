package com.eyelinecom.whoisd.sads2.wechat.resource;

import com.eyelinecom.whoisd.sads2.common.HttpDataLoader;
import com.eyelinecom.whoisd.sads2.common.HttpLoader;
import com.eyelinecom.whoisd.sads2.common.Loader;
import com.eyelinecom.whoisd.sads2.common.SADSInitUtils;
import com.eyelinecom.whoisd.sads2.common.UrlUtils;
import com.eyelinecom.whoisd.sads2.eventstat.DetailedStatLogger;
import com.eyelinecom.whoisd.sads2.resource.ResourceFactory;
import com.eyelinecom.whoisd.sads2.wechat.api.types.AccessTokenResponse;
import com.eyelinecom.whoisd.sads2.wechat.api.types.CustomServiceMessage;
import com.eyelinecom.whoisd.sads2.wechat.api.types.ErrorResponse;
import com.eyelinecom.whoisd.sads2.wechat.api.types.UploadMediaResponse;
import com.eyelinecom.whoisd.sads2.wechat.registry.WechatToken;
import com.eyelinecom.whoisd.sads2.wechat.util.MarshalUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 16.12.16
 * Time: 14:01
 * To change this template use File | Settings | File Templates.
 */
public class WeChatApiImpl implements WeChatApi {

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WeChatApiImpl.class);

  private final Cache<WechatToken, AccessTokenResponse> accessTokenCache = CacheBuilder.newBuilder()
    .maximumSize(100000)
    .expireAfterWrite(7200, TimeUnit.SECONDS)
    .removalListener(new RemovalListener<WechatToken, AccessTokenResponse>() {
      @Override
      public void onRemoval(RemovalNotification<WechatToken, AccessTokenResponse> notification) {
        log.info("Removing from cache entry for " + notification.getKey() + ":" + notification.getValue());
      }
    }).build();

  private final HttpDataLoader loader;
  private final DetailedStatLogger detailedStatLogger;
  private final Properties properties;
  private final ConcurrentHashMap<String, Waiter> waitUsersMap;

  public WeChatApiImpl(HttpDataLoader loader,
                       DetailedStatLogger detailedStatLogger,
                       Properties properties) {

    this.loader = loader;
    this.detailedStatLogger = detailedStatLogger;
    this.properties = properties;
    this.waitUsersMap = new ConcurrentHashMap<String, Waiter>();
  }

  @Override
  public void respond(String text, String userId) {
    log.debug("Responding to user \"" + userId + "\", text: " + text);
    Waiter waiter = waitUsersMap.get(userId);
    if (waiter == null) {
      log.warn("For user \"" + userId + "\" no waiter found");
    } else {
      waiter.send(text);
    }
  }

  @Override
  public String waitForResponse(String botId, String userId) {
    log.debug("Waiting for response from user " + userId);
    Waiter waiter = new Waiter();
    waitUsersMap.put(userId, waiter);
    String text = waiter.waitForText();
    if (text == null || text.isEmpty()) {
      log.debug("Failed ");
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xml>" +
      "<ToUserName><![CDATA[" + userId + "]]></ToUserName>" +
      "<FromUserName><![CDATA[" + botId + "]]></FromUserName>" +
      "<CreateTime>" + (System.currentTimeMillis() / 1000) + "</CreateTime>" +
      "<MsgType><![CDATA[text]]></MsgType>" +
      "<Content><![CDATA[" + text + "]]></Content>" +
      "</xml>");
    return sb.toString();
  }

  @Override
  public String uploadPhoto(final WechatToken token, byte[] data) {
    try {
      AccessTokenResponse accessTokenResponse = getAccessToken(token);
      if (accessTokenResponse.isError()) {
        log.warn("Can't upload photo, failed to get access_token: " + accessTokenResponse);
        return null;
      }
      String accessToken = accessTokenResponse.getAccessToken();
      String uploadUrl = "http://file.api.wechat.com/cgi-bin/media/upload?type=image&access_token=" + accessToken;
      ByteArrayPartSource partSource = new ByteArrayPartSource("image.jpg", data);
      ArrayList<Part> parts = new ArrayList<>();
      Map<String, String> parameters = UrlUtils.getParametersMap(uploadUrl);
      for (Map.Entry<String, String> en : parameters.entrySet()) parts.add(new StringPart(en.getKey(), en.getValue()));
      parts.add(new FilePart("photo", partSource, "image/jpeg", "UTF-8"));
      Loader.Entity entity = loader.postMultipart(uploadUrl, Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), parts);
      String response = new String(entity.getBuffer());

      UploadMediaResponse uploadMediaResponse = MarshalUtils.unmarshal(response, UploadMediaResponse.class);
      if (uploadMediaResponse.isError()) {
        log.warn("Failed to upload photo, api response: " + uploadMediaResponse);
        return null;
      }
      log.debug("Uploaded photo, response: " + uploadMediaResponse);
      return uploadMediaResponse.getMediaId();
    } catch (Exception e) {
      log.error("Failed to upload photo", e);
      return null;
    }
  }

  @Override
  public void sendImage(String mediaId, String userId, WechatToken token) {
    try {
      AccessTokenResponse accessTokenResponse = getAccessToken(token);
      if (accessTokenResponse.isError()) {
        log.warn("Can't send image, failed to get access_token: " + accessTokenResponse);
        return;
      }
      String accessToken = accessTokenResponse.getAccessToken();
      String url = "https://api.wechat.com/cgi-bin/message/custom/send?access_token=" + accessToken;
      String requestString = MarshalUtils.marshal(CustomServiceMessage.image(userId, mediaId));
      Loader.Entity entity = loader.load(url, requestString, "application/x-www-form-urlencoded", "UTF-8", Collections.<String, String>emptyMap(), HttpLoader.METHOD_POST);
      ErrorResponse response = MarshalUtils.unmarshal(new String(entity.getBuffer()), ErrorResponse.class);
      log.debug("Send image result: " + response);
    } catch (Exception e) {
      log.error("Failed to send message to user", e);
    }
  }

  @Override
  public boolean check(WechatToken token) throws Exception {
    AccessTokenResponse accessTokenResponse = getAccessToken(token);
    return (!accessTokenResponse.isError() && accessTokenResponse.getAccessToken() != null);
  }

  public AccessTokenResponse getAccessToken(final WechatToken token) throws Exception {
    return accessTokenCache.get(token, new Callable<AccessTokenResponse>() {
      @Override
      public AccessTokenResponse call() throws Exception {
        Loader.Entity data = loader.load("https://api.wechat.com/cgi-bin/token?grant_type=client_credential&appid=" + token.appId() + "&secret=" + token.secret());
        String response = new String(data.getBuffer());
        log.debug("Getting access_token for " + token + ", result: " + response);
        return MarshalUtils.unmarshal(response, AccessTokenResponse.class);
      }
    });
  }

  public static class Factory implements ResourceFactory {

    @Override
    public Object build(String id, Properties properties, HierarchicalConfiguration config) throws Exception {
      final HttpDataLoader loader = SADSInitUtils.getResource("loader", properties);
      final DetailedStatLogger detailedStatLogger = SADSInitUtils.getResource("detailed-stat-logger", properties);
      return new WeChatApiImpl(loader, detailedStatLogger, properties);
    }

    @Override
    public boolean isHeavyResource() {
      return false;
    }
  }

  private class Waiter {

    private volatile String text;

    public synchronized void send(String text) {
      this.text = text;
      notifyAll();
    }

    public synchronized String waitForText() {
      int timeLeft = 5000;
      try {
        while (text == null) {
          long t1 = System.currentTimeMillis();
          wait(timeLeft);
          long t2 = System.currentTimeMillis();
          timeLeft -= (t2 - t1);
          if (timeLeft <= 0) break;
        }
        return text;
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
