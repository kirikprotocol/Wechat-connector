package com.eyelinecom.whoisd.sads2.wechat.registry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 16.12.16
 * Time: 14:52
 * To change this template use File | Settings | File Templates.
 */
public class WechatToken {
  private static final Pattern PATTERN = Pattern.compile("([^:]+):(.+)");
  private final String appId;
  private final String secret;

  private WechatToken(String appId, String secret) {
    this.appId = appId;
    this.secret = secret;
  }

  public static WechatToken get(String tokenString) {
    if (tokenString == null || tokenString.isEmpty()) return null;
    Matcher m = PATTERN.matcher(tokenString);
    if (!m.matches()) return null;
    return new WechatToken(m.group(1), m.group(2));
  }

  public String appId() {
    return appId;
  }

  public String secret() {
    return secret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WechatToken that = (WechatToken) o;
    return appId.equals(that.appId) && secret.equals(that.secret);

  }

  @Override
  public int hashCode() {
    int result = appId.hashCode();
    result = 31 * result + secret.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return appId + ":" + secret;
  }
}
