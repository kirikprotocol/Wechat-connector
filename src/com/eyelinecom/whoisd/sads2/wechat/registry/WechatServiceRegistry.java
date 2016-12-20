package com.eyelinecom.whoisd.sads2.wechat.registry;

import com.eyelinecom.whoisd.sads2.common.SADSInitUtils;
import com.eyelinecom.whoisd.sads2.exception.ConfigurationException;
import com.eyelinecom.whoisd.sads2.registry.Config;
import com.eyelinecom.whoisd.sads2.registry.ServiceConfig;
import com.eyelinecom.whoisd.sads2.registry.ServiceConfigListener;
import com.eyelinecom.whoisd.sads2.resource.ResourceFactory;
import com.eyelinecom.whoisd.sads2.wechat.resource.WeChatApi;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: gev
 * Date: 16.12.16
 * Time: 14:49
 * To change this template use File | Settings | File Templates.
 */
public class WechatServiceRegistry extends ServiceConfigListener {

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WechatServiceRegistry.class);

  public static final String CONF_TOKEN = "wechat.token";

  private final WeChatApi api;
  private final Map<String, WechatToken> serviceMap = new ConcurrentHashMap<>();
  private final Map<WechatToken, String> tokenMap = new ConcurrentHashMap<>();

  public WechatServiceRegistry(WeChatApi api) {
    this.api = api;
  }

  @Override
  protected void process(Config config) throws ConfigurationException {
    final String serviceId = config.getId();
    if (config.isEmpty()) {
      unregister(serviceId);
    } else if (config instanceof ServiceConfig) {
      final ServiceConfig serviceConfig = (ServiceConfig) config;
      WechatToken token = getToken(serviceConfig.getAttributes());
      if (token == null) unregister(serviceId);
      else register(serviceId, token);
    }
  }

  private void register(String serviceId, WechatToken token) {
    WechatToken mapToken = serviceMap.get(serviceId);
    if (mapToken != null && token.equals(mapToken)) {
      // token unchanged, do nothing
      log.debug("Service \"" + serviceId + "\" already registered in wechat api, token: " + token + "...");
      return;
    } else if (mapToken != null && !token.equals(mapToken)) {
      // token changed, what to do?
    }
    String mapService = tokenMap.get(token);
    if (mapService != null && !mapService.equals(serviceId)) {
      // there is already service for this token, can't register
      log.debug("Can't register token " + token + " for service \"" + serviceId + "\", token already registered for service \"" + mapService + "\"");
      return;
    }
    log.debug("registered for service" + serviceId + " token " + token);
    // do anything upon registration?
    serviceMap.put(serviceId, token);
    tokenMap.put(token, serviceId);

  }

  private void unregister(String serviceId) {
    WechatToken token = serviceMap.remove(serviceId);
    if (token != null) {
      log.debug("unregistered \"" + serviceId + "\", token: \"" + token + "\"");
      String mapService = tokenMap.remove(token);
      if (serviceId.equals(mapService)) {
        // do anything?
      }
    }
  }

  public static WechatToken getToken(Properties properties) {
    return WechatToken.get(properties.getProperty(WechatServiceRegistry.CONF_TOKEN));
  }

  public WechatToken getToken(String serviceId) {
    return serviceMap.get(serviceId);
  }

  public static class Factory implements ResourceFactory {

    @Override
    public Object build(String id, Properties properties, HierarchicalConfiguration config) throws Exception {
      WeChatApi api = SADSInitUtils.getResource("wechat-api", properties);
      return new WechatServiceRegistry(api);
    }

    @Override
    public boolean isHeavyResource() {
      return false;
    }
  }

}
