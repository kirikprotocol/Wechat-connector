package com.eyelinecom.whoisd.sads2.wechat.interceptors;

import com.eyelinecom.whoisd.sads2.RequestDispatcher;
import com.eyelinecom.whoisd.sads2.common.Initable;
import com.eyelinecom.whoisd.sads2.common.PageBuilder;
import com.eyelinecom.whoisd.sads2.common.SADSInitUtils;
import com.eyelinecom.whoisd.sads2.connector.SADSRequest;
import com.eyelinecom.whoisd.sads2.connector.SADSResponse;
import com.eyelinecom.whoisd.sads2.connector.Session;
import com.eyelinecom.whoisd.sads2.content.ContentRequestUtils;
import com.eyelinecom.whoisd.sads2.content.ContentResponse;
import com.eyelinecom.whoisd.sads2.content.attributes.AttributeSet;
import com.eyelinecom.whoisd.sads2.exception.InterceptionException;
import com.eyelinecom.whoisd.sads2.executors.connector.SADSExecutor;
import com.eyelinecom.whoisd.sads2.interceptor.BlankInterceptor;
import com.eyelinecom.whoisd.sads2.wechat.resource.WeChatApi;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static com.eyelinecom.whoisd.sads2.content.attributes.AttributeReader.getAttributes;
import static com.eyelinecom.whoisd.sads2.executors.connector.ProfileEnabledMessageConnector.ATTR_SESSION_PREVIOUS_PAGE_URI;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 16.12.16
 * Time: 14:00
 * To change this template use File | Settings | File Templates.
 */
public class WechatPushInterceptor extends BlankInterceptor implements Initable {

  private WeChatApi api;


  @Override
  public void afterResponseRender(SADSRequest request,
                                  ContentResponse content,
                                  SADSResponse response,
                                  RequestDispatcher dispatcher) throws InterceptionException {

    try {
      final Document doc = (Document) response.getAttributes().get(PageBuilder.VALUE_DOCUMENT);
      final String keyboard = getKeyboard(doc);
      String text = getText(doc);

      final boolean shouldCloseSession;
      {
        if (keyboard != null || !doc.getRootElement().elements("input").isEmpty()) {
          shouldCloseSession = false;

        } else {
          final AttributeSet pageAttributes = getAttributes(doc.getRootElement());
          shouldCloseSession = !pageAttributes.getBoolean("wechat.keep.session")
            .or(pageAttributes.getBoolean("keep.session"))
            .or(false);
        }
      }
      final Session session = request.getSession();
      if (!shouldCloseSession) {
        session.setAttribute(SADSExecutor.ATTR_SESSION_PREVIOUS_PAGE, doc);
        session.setAttribute(
          ATTR_SESSION_PREVIOUS_PAGE_URI,
          response.getAttributes().get(ContentRequestUtils.ATTR_REQUEST_URI));
      }
      String userId = request.getProfile().property("wechat", "id").getValue();
      api.respond(text + ((keyboard == null) ? "" : "\n" + keyboard), userId);

    } catch (Exception e) {
      throw new InterceptionException(e);
    }

  }


  public static String getText(final Document doc) throws DocumentException {
    final Collection<String> messages = new ArrayList<String>() {{
      //noinspection unchecked
      for (Element e : (List<Element>) doc.getRootElement().elements("message")) {
        add(getContent(e));
      }
    }};

    return StringUtils.join(messages, "\n").trim();
  }

  public static String getContent(Element element) throws DocumentException {
    final StringBuilder buf = new StringBuilder();

    final Element messageElement = new SAXReader()
      .read(new ByteArrayInputStream(element.asXML().getBytes(StandardCharsets.UTF_8)))
      .getRootElement();

    //noinspection unchecked
    for (Node e : (List<Node>) messageElement.selectNodes("//text()")) {
      if (!"pre".equals(e.getParent().getName())) {
        e.setText(e.getText().replaceAll("\\n\\s+", "\n"));
      }
    }

    //noinspection unchecked
    for (Node e : (Collection<Node>) IteratorUtils.toList(messageElement.nodeIterator())) {
      buf.append(e.asXML());
    }
    return buf.toString().trim();
  }

  public static String getKeyboard(Document doc) {

    @SuppressWarnings("unchecked")
    final List<Element> buttons = (List<Element>) doc.getRootElement().elements("button");
    if (isEmpty(buttons)) {
      return null;
    }

    final StringBuilder buf = new StringBuilder();

    for (Element button : buttons) {
      buf.append(button.attributeValue("index")).append("> ");
      buf.append(button.getTextTrim()).append("\n");
    }

    return buf.toString();
  }


  @Override
  public void init(Properties config) throws Exception {
    api = SADSInitUtils.getResource("client", config);
  }

  @Override
  public void destroy() {
  }
}
