package com.eyelinecom.whoisd.sads2.wechat.api.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 19.12.16
 * Time: 18:53
 * To change this template use File | Settings | File Templates.
 */
public class ErrorResponse {

  @JsonProperty(value = "errcode")
  private Integer errcode;

  @JsonProperty(value = "errmsg")
  private String errmsg;

  public boolean isError() {
    return errcode != null && errcode != 0;
  }

  public Integer getErrcode() {
    return errcode;
  }

  @Override
  public String toString() {
    return "{" + "\"errcode\":" + errcode + ",\"errmsg\":\"" + errmsg + "\"}";
  }
}
