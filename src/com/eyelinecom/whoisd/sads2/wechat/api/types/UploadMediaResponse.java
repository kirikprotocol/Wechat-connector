package com.eyelinecom.whoisd.sads2.wechat.api.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 20.12.16
 * Time: 13:49
 * To change this template use File | Settings | File Templates.
 */
public class UploadMediaResponse extends ErrorResponse {
  @JsonProperty(value = "type")
  private String type;

  @JsonProperty(value = "media_id")
  private String mediaId;

  @JsonProperty(value = "created_at")
  private Long createdAt;


  public String getMediaId() {
    return mediaId;
  }

  @Override
  public String toString() {
    if (isError()) return super.toString();
    return "{" + "\"type:\":\"" + type + "\",\"media_id\":\"" + mediaId + "\",\"created_at\":" + createdAt + '}';
  }

}
