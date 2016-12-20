package com.eyelinecom.whoisd.sads2.wechat.api.types;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 16.12.16
 * Time: 13:12
 * To change this template use File | Settings | File Templates.
 */
public class MessageType {

  public static final MessageType text = new MessageType("text");
  public static final MessageType image = new MessageType("image");
  public static final MessageType audio = new MessageType("audio");
  public static final MessageType video = new MessageType("video");
  public static final MessageType location = new MessageType("location");

  private final String id;

  private MessageType(String id) {
    this.id = id;
    MapHolder.map.put(id, this);
  }

  public static MessageType get(String id) {
    MessageType messageType = MapHolder.map.get(id);
    if (messageType == null) messageType = new MessageType(id);
    return messageType;
  }

  private static class MapHolder {
    public static final Map<String, MessageType> map = new HashMap<>();
  }

}
