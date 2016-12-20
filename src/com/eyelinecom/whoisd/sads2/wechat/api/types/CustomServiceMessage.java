package com.eyelinecom.whoisd.sads2.wechat.api.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 20.12.16
 * Time: 14:04
 * To change this template use File | Settings | File Templates.
 */
public class CustomServiceMessage {

  @JsonProperty(value = "touser")
  private String touser;

  @JsonProperty(value = "msgtype")
  private String msgtype;

  @JsonProperty(value = "text")
  private Text text;

  @JsonProperty(value = "image")
  private Image image;

  public static CustomServiceMessage image(String userId, String mediaId) {
    CustomServiceMessage m = new CustomServiceMessage();
    m.touser = userId;
    m.msgtype = "image";
    m.image = new Image();
    m.image.mediaId = mediaId;
    return m;
  }

  public static CustomServiceMessage text(String userId, String content) {
    CustomServiceMessage m = new CustomServiceMessage();
    m.touser = userId;
    m.msgtype = "image";
    m.text = new Text();
    m.text.content = content;
    return m;
  }

  public static class Text {
    @JsonProperty(value = "content")
    private String content;
  }

  public static class Image {
    @JsonProperty(value = "media_id")
    private String mediaId;
  }
}
