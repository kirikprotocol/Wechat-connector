package com.eyelinecom.whoisd.sads2.wechat.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: zoldorn
 * Date: 02.08.16
 * Time: 9:09
 * To change this template use File | Settings | File Templates.
 */
public class MarshalUtils {

  private static final ObjectMapper mapper = new ObjectMapper();
  static {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static JsonNode parse(String json) throws IOException {
    return mapper.readTree(json);
  }

  public static <T> T unmarshal(JsonNode obj, Class<T> clazz)
      throws IOException {
    //noinspection unchecked
    return (T) mapper.readerFor(clazz).readValue(obj);
  }

  public static <T> T unmarshal(String json, Class<T> clazz)
      throws IOException {
    //noinspection unchecked
    return (T) mapper.readerFor(clazz).readValue(parse(json));
  }

  public static <T> String marshal(T obj) throws JsonProcessingException {
    return mapper.writer().writeValueAsString(obj);
  }

}
