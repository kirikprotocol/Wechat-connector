package com.eyelinecom.whoisd.sads2.wechat.connector;

import com.eyelinecom.whoisd.sads2.Protocol;
import com.eyelinecom.whoisd.sads2.common.InitUtils;
import com.eyelinecom.whoisd.sads2.common.ProfileUtil;
import com.eyelinecom.whoisd.sads2.common.SADSUrlUtils;
import com.eyelinecom.whoisd.sads2.common.UrlUtils;
import com.eyelinecom.whoisd.sads2.connector.ChatCommand;
import com.eyelinecom.whoisd.sads2.connector.SADSRequest;
import com.eyelinecom.whoisd.sads2.connector.SADSResponse;
import com.eyelinecom.whoisd.sads2.connector.Session;
import com.eyelinecom.whoisd.sads2.events.Event;
import com.eyelinecom.whoisd.sads2.events.LinkEvent;
import com.eyelinecom.whoisd.sads2.events.MessageEvent;
import com.eyelinecom.whoisd.sads2.eventstat.MarshalUtils;
import com.eyelinecom.whoisd.sads2.exception.NotFoundResourceException;
import com.eyelinecom.whoisd.sads2.executors.connector.AbstractHTTPPushConnector;
import com.eyelinecom.whoisd.sads2.executors.connector.ProfileEnabledMessageConnector;
import com.eyelinecom.whoisd.sads2.executors.connector.SADSExecutor;
import com.eyelinecom.whoisd.sads2.input.AbstractInputType;
import com.eyelinecom.whoisd.sads2.input.InputFile;
import com.eyelinecom.whoisd.sads2.input.InputLocation;
import com.eyelinecom.whoisd.sads2.profile.Profile;
import com.eyelinecom.whoisd.sads2.registry.ServiceConfig;
import com.eyelinecom.whoisd.sads2.session.SessionManager;
import com.eyelinecom.whoisd.sads2.wechat.api.types.MessageType;
import com.eyelinecom.whoisd.sads2.wechat.registry.WechatServiceRegistry;
import com.eyelinecom.whoisd.sads2.wechat.registry.WechatToken;
import com.eyelinecom.whoisd.sads2.wechat.resource.WeChatApi;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.dom4j.Document;
import org.dom4j.Element;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.eyelinecom.whoisd.sads2.Protocol.WECHAT;
import static com.eyelinecom.whoisd.sads2.common.ProfileUtil.inProfile;
import static com.eyelinecom.whoisd.sads2.connector.ChatCommand.*;
import static com.eyelinecom.whoisd.sads2.wstorage.profile.QueryRestrictions.property;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 15.12.16
 * Time: 15:08
 * To change this template use File | Settings | File Templates.
 */
public class WechatMessageConnector extends HttpServlet {

  private final static Log log = new Log4JLogger(org.apache.log4j.Logger.getLogger(WechatMessageConnector.class));

  private WechatMessageConnectorImpl connector;

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    connector = new WechatMessageConnectorImpl();

    try {
      final Properties properties = AbstractHTTPPushConnector.buildProperties(servletConfig);
      connector.init(properties);

    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void service(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
    // TODO: confirmation

    String echostr = req.getParameter("echostr");
    if (echostr != null) {
      // This is verification request, responding with echostr
      // TODO: check signature
      resp.getWriter().write(echostr);
      resp.getWriter().flush();
      return;
    }
    WechatRequest request = new WechatRequest(req);
    connector.process(request);

    // TODO: implement 5 second delay and looking for responces on same requests (check "MsgId")
    String responseString = connector.getApi().waitForResponse(request.getToUserName(), request.getFromUserName());
    log.debug("Responding wechat request with " + responseString);
    byte[] data = responseString.getBytes("UTF-8");
    resp.setContentType("text/xml; charset=UTF-8");
    resp.setContentLength(data.length);
    resp.getOutputStream().write(data);
  }


  private class WechatMessageConnectorImpl extends ProfileEnabledMessageConnector<WechatRequest> {

    @Override
    protected SADSResponse buildQueuedResponse(WechatRequest wechatRequest, SADSRequest sadsRequest) {
      return buildCallbackResponse(200, "");
    }

    @Override
    protected SADSResponse buildQueueErrorResponse(Exception e, WechatRequest wechatRequest, SADSRequest sadsRequest) {
      return buildCallbackResponse(500, "");
    }

    @Override
    protected Log getLogger() {
      return log;
    }

    @Override
    protected String getSubscriberId(WechatRequest req) throws Exception {

      if (req.getProfile() != null) {
        return req.getProfile().getWnumber();
      }

      final String userId = String.valueOf(req.getFromUserName());
      final String incoming = req.getTextContent();

      if (ChatCommand.match(getServiceId(req), incoming, WECHAT) == CLEAR_PROFILE) {
        // Reset profile of the current user.
        final Profile profile = getProfileStorage()
          .query()
          .where(property("wechat", "id").eq(userId))
          .get();
        if (profile != null) {
          final boolean isDevModeEnabled = inProfile(profile).getDeveloperMode(req.getServiceId());
          if (isDevModeEnabled) {
            inProfile(profile).clear();
            inProfile(profile).setDeveloperMode(getServiceId(req), true);

            // Also clear the session.
            final SessionManager sessionManager = getSessionManager(WECHAT, req.getServiceId());
            final Session session = sessionManager.getSession(profile.getWnumber(), false);
            if (session != null && !session.isClosed()) {
              session.close();
            }
          }
        }
      }

      final Profile profile = getProfileStorage()
        .query()
        .where(property("wechat", "id").eq(userId))
        .getOrCreate();

      req.setProfile(profile);
      return profile.getWnumber();

    }

    @Override
    protected String getServiceId(WechatRequest req) throws Exception {
      return req.getServiceId();
    }

    @Override
    protected String getGateway() {
      return "Wechat";
    }

    @Override
    protected String getGatewayRequestDescription(WechatRequest wechatRequest) {
      return "Wechat";
    }

    @Override
    protected boolean isTerminated(WechatRequest req) throws Exception {
      final String incoming = req.getTextContent();

      final boolean isDevModeEnabled = req.getProfile() != null &&
        ProfileUtil.inProfile(req.getProfile()).getDeveloperMode(req.getServiceId());

      final ChatCommand command = ChatCommand.match(getServiceId(req), incoming, WECHAT);
      return command == SET_DEVELOPER_MODE ||
        isDevModeEnabled && asList(SHOW_PROFILE, WHO_IS).contains(command);
    }

    // TODO: protected Long getEventOrder(WechatRequest req) {?

    @Override
    protected Protocol getRequestProtocol(ServiceConfig config, String subscriberId, WechatRequest request) {
      return WECHAT;
    }

    @Override
    protected String getRequestUri(ServiceConfig config,
                                   String wnumber,
                                   WechatRequest message) throws Exception {
      final String serviceId = config.getId();
      final String incoming = message.getTextContent();
      final SessionManager sessionManager = getSessionManager(serviceId);
      final Profile profile = getProfileStorage().find(wnumber);
      final boolean isDevModeEnabled = inProfile(profile).getDeveloperMode(serviceId);

      Session session = sessionManager.getSession(wnumber);
      final ChatCommand cmd = ChatCommand.match(serviceId, incoming, WECHAT);
      if (cmd == INVALIDATE_SESSION && isDevModeEnabled) {
        // Invalidate the current session.
        session.close();
        session = sessionManager.getSession(wnumber);

      } else {
        WeChatApi client = getClient();
        if (cmd == WHO_IS && isDevModeEnabled) {
          // TODO:...

          final WechatToken token = WechatServiceRegistry.getToken(config.getAttributes());
          final String userId = message.getFromUserName();

          final String msg =
              StringUtils.join(
                  new String[] {
                      "App ID: " + token.appId() + ".",
                      "Service: " + serviceId + ".",
                      "MiniApps host: " + getRootUri()
                  },
                  "\n");
          client.respond(msg, userId);
        } else if (cmd == SHOW_PROFILE && isDevModeEnabled) {
          final String userId = message.getFromUserName();
          client.respond(profile.dump(), userId);
        } else if (cmd == SET_DEVELOPER_MODE) {

          final String value = ChatCommand.getCommandValue(incoming);
          final Boolean devMode = BooleanUtils.toBooleanObject(value);
          final String userId = message.getFromUserName();

          if (devMode != null) {
            inProfile(profile).setDeveloperMode(serviceId, devMode);

            client.respond(
                "Developer mode is " + (devMode ? "enabled" : "disabled") + ".",
                userId);

          } else {
            client.respond(
                "Developer mode is " +
                    (inProfile(profile).getDeveloperMode(serviceId) ? "enabled" : "disabled") +
                    ".",
                userId);
          }
        }
      }
      final String prevUri = (String) session.getAttribute(ATTR_SESSION_PREVIOUS_PAGE_URI);
      if (prevUri == null) {
        // No previous page means this is an initial request, thus serve the start page.
        message.setEvent(new MessageEvent.TextMessageEvent(incoming));
        return super.getRequestUri(config, wnumber, message);
      } else {
        final Document prevPage =
          (Document) session.getAttribute(SADSExecutor.ATTR_SESSION_PREVIOUS_PAGE);
        String href = null;
        String inputName = null;

        // Look for a button with a corresponding label.
        //noinspection unchecked
        for (Element e : (List<Element>) prevPage.getRootElement().elements("button")) {
          final String btnLabel = e.getTextTrim();
          final String btnIndex = e.attributeValue("index");

          if (equalsIgnoreCase(btnLabel, incoming) || equalsIgnoreCase(btnIndex, incoming)) {
            final String btnHref = e.attributeValue("href");
            href = btnHref != null ? btnHref : e.attributeValue("target");

            message.setEvent(new LinkEvent(btnLabel, prevUri));
          }
        }

        // Look for input field if any.
        if (href == null) {
          final Element input = prevPage.getRootElement().element("input");
          if (input != null) {
            href = input.attributeValue("href");
            inputName = input.attributeValue("name");
          }
        }

        // Nothing suitable to handle user input found, consider it a bad command.
        if (href == null) {
          final String badCommandPage =
            InitUtils.getString("bad-command-page", "", config.getAttributes());
          href = UrlUtils.merge(prevUri, badCommandPage);
          href = UrlUtils.addParameter(href, "bad_command", incoming);
        }

        if (message.getEvent() == null) {
          message.setEvent(new MessageEvent.TextMessageEvent(incoming));
        }

        href = SADSUrlUtils.processUssdForm(href, StringUtils.trim(incoming));
        if (inputName != null) {
          href = UrlUtils.addParameter(href, inputName, incoming);
        }
        return UrlUtils.merge(prevUri, href);
      }
    }

    private WeChatApi getClient() throws NotFoundResourceException {
      return getResource("wechat-api");
    }


    @Override
    protected SADSResponse getOuterResponse(WechatRequest wechatRequest, SADSRequest request, SADSResponse response) {
      return buildCallbackResponse(200, "");
    }

    private SessionManager getSessionManager(String serviceId) throws Exception {
      return super.getSessionManager(WECHAT, serviceId);
    }

    @Override
    protected void fillSADSRequest(SADSRequest sadsRequest, WechatRequest request) {
      try {
        handleFileUpload(sadsRequest, request);

      } catch (Exception e) {
        getLog(request).error(e.getMessage(), e);
      }

      super.fillSADSRequest(sadsRequest, request);
    }

    @Override
    protected Profile getCachedProfile(WechatRequest req) {
      return req.getProfile();
    }

    @Override
    protected Event getEvent(WechatRequest req) {
      return req.getEvent();
    }

    private void handleFileUpload(SADSRequest sadsRequest, WechatRequest req) throws Exception {
      final List<? extends AbstractInputType> mediaList = extractMedia(req);
      if (isEmpty(mediaList)) return;

      req.setEvent(mediaList.iterator().next().asEvent());

      final Session session = sadsRequest.getSession();
      final Document prevPage = (Document) session.getAttribute(SADSExecutor.ATTR_SESSION_PREVIOUS_PAGE);
      final Element input = prevPage == null ? null : prevPage.getRootElement().element("input");
      final String inputName = input != null ? input.attributeValue("name") : "bad_command";

      final String mediaParameter = MarshalUtils.marshal(mediaList);
      sadsRequest.getParameters().put(inputName, mediaParameter);
      sadsRequest.getParameters().put("input_type", "json");
    }

    private List<? extends AbstractInputType> extractMedia(WechatRequest req) {
      final List<AbstractInputType> mediaList = new ArrayList<>();

      if (req.getMsgType() == MessageType.location && req.getLocationX() != null && req.getLocationY() != null) {
        final InputLocation location = new InputLocation();
        location.setLatitude(req.getLocationX());
        location.setLongitude(req.getLocationY());
        mediaList.add(location);
        // TODO: what to do with req.getLabel() ?
      } else if (req.getMsgType() == MessageType.image) {
        final InputFile file = new InputFile();
        file.setMediaType("photo");
        file.setUrl(req.getPicUrl());
        mediaList.add(file);
      }
      // TODO: video and audio...
      return mediaList;
    }

    private SADSResponse buildCallbackResponse(int statusCode, String body) {
      final SADSResponse rc = new SADSResponse();
      rc.setStatus(statusCode);
      rc.setHeaders(Collections.<String, String>emptyMap());
      rc.setMimeType("text/plain");
      rc.setData(body.getBytes());
      return rc;
    }

    private WeChatApi getApi() {
      try {
        return getResource("wechat-api");
      } catch (Exception e) {
        log.error(e);
        throw new RuntimeException(e);
      }
    }


  }

}
