package com.eyelinecom.whoisd.sads2.wechat.api.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 19.12.16
 * Time: 18:39
 * To change this template use File | Settings | File Templates.
 */
public class AccessTokenResponse extends ErrorResponse {

  @JsonProperty(value = "access_token")
  private String accessToken;

  @JsonProperty(value = "expires_in")
  private Integer expiresIn;

  public String getAccessToken() {
    return accessToken;
  }

  @Override
  public String toString() {
    if (isError()) return super.toString();
    return "{" + "\"access_token:\":\"" + accessToken + "\",\"expires_in\":" + expiresIn + '}';
  }
}
