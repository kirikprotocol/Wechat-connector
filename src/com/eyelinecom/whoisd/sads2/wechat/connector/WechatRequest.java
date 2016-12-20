package com.eyelinecom.whoisd.sads2.wechat.connector;

import com.eyelinecom.whoisd.sads2.common.StoredHttpRequest;
import com.eyelinecom.whoisd.sads2.events.Event;
import com.eyelinecom.whoisd.sads2.eventstat.LoggableExternalRequest;
import com.eyelinecom.whoisd.sads2.profile.Profile;
import com.eyelinecom.whoisd.sads2.wechat.api.types.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 15.12.16
 * Time: 15:37
 * To change this template use File | Settings | File Templates.
 */
public class WechatRequest extends StoredHttpRequest implements LoggableExternalRequest {

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WechatRequest.class);

  private final String serviceId;
  private String toUserName;
  private String fromUserName;
  private long createTime;
  private MessageType msgType;
  private String textContent;
  private String msgId;
  private String mediaId;
  private String picUrl;
  private Double locationX;
  private Double locationY;
  private String scale;
  private String label;

  private transient Profile profile;
  private transient Event event;

  public WechatRequest(HttpServletRequest req) throws IOException {
    super(req);
    final String[] parts = getRequestURI().split("/");
    serviceId = parts[parts.length - 1];
    parseContent(getContent());
    //content;
  }

  private void parseContent(String contentString) throws IOException {
    // TODO: cache builder in ThreadLocal?
    try {
      log.debug("Parsing content: " + contentString);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new ByteArrayInputStream(contentString.getBytes("UTF-8")));
      Element root = document.getDocumentElement();
      if (!root.getTagName().equals("xml")) throw new RuntimeException("Unknown root element " + root.getTagName());
      NodeList nodes = root.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);
        if (node instanceof Text) {
          Text text = (Text) node;
          if (!text.getWholeText().trim().isEmpty()) log.warn("Unexpected text in xml: " + text.getWholeText());
        } else if(node instanceof Element) {
          Element element = (Element) node;
          String tag = element.getTagName();
          if (tag.equals("ToUserName")) {
            toUserName = readElementContent(element);
          } else if(tag.equals("FromUserName")) {
            fromUserName = readElementContent(element);
          } else if(tag.equals("CreateTime")) {
            createTime = Long.parseLong(readElementContent(element));
          } else if(tag.equals("MsgType")) {
            msgType = MessageType.get(readElementContent(element));
          } else if(tag.equals("Content")) {
            textContent = readElementContent(element);
          } else if(tag.equals("MsgId")) {
            msgId = readElementContent(element);
          } else if(tag.equals("MediaId")) {
            mediaId = readElementContent(element);
          } else if(tag.equals("PicUrl")) {
            picUrl = readElementContent(element);
          } else if(tag.equals("Location_X")) {
            String locationXString = readElementContent(element);
            if (locationXString != null) locationX = Double.parseDouble(locationXString);
          } else if(tag.equals("Location_Y")) {
            String locationYString = readElementContent(element);
            if(locationYString != null) locationY = Double.parseDouble(locationYString);
          } else if(tag.equals("Scale")) {
            scale = readElementContent(element);
          } else if(tag.equals("Label")) {
            label = readElementContent(element);
          } else {
            log.warn("Unknown element " + element.getTagName());
          }

        } else {
          //log.debug();
        }
      }

    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private static String readElementContent(Element element) {
    NodeList nodes = element.getChildNodes();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node instanceof Text) {
        Text text = (Text) node;
        sb.append(text.getWholeText());
      } else if(node instanceof Element) {
        throw new RuntimeException("Unexpected element " + ((Element) node).getTagName());
      } else {
        throw new RuntimeException("Unexpected element " + node.getClass().getName());
      }
    }
    return sb.toString();
  }

  public String getFromUserName() {
    return fromUserName;
  }

  public String getToUserName() {
    return toUserName;
  }

  public String getTextContent() {
    return textContent;
  }

  public Double getLocationX() {
    return locationX;
  }

  public Double getLocationY() {
    return locationY;
  }

  public String getLabel() {
    return label;
  }

  public MessageType getMsgType() {
    return msgType;
  }

  public String getPicUrl() {
    return picUrl;
  }

  public Profile getProfile() {
    return profile;
  }

  public void setProfile(Profile profile) {
    this.profile = profile;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  @Override
  public Object getLoggableData() {
    // TODO:...
    return getTextContent();
  }

  public String getServiceId() {
    return serviceId;
  }
}
