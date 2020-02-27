package org.obiba.mica.web.controller.domain;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AuthConfiguration {

  private final JSONObject publicConfiguration;

  private final JSONObject clientConfiguration;

  private final List<UserAttribute> userAttributes;

  private final List<String> languages;

  public AuthConfiguration(JSONObject publicConfiguration, JSONObject clientConfiguration) {
    this.publicConfiguration = publicConfiguration;
    this.clientConfiguration = clientConfiguration;
    this.userAttributes = Lists.newArrayList();
    try {
      JSONArray array = publicConfiguration.getJSONArray("userAttributes");
      for (int i=0; i<array.length(); i++) {
        userAttributes.add(new UserAttribute(array.getJSONObject(i)));
      }
    } catch (JSONException e) {
      // ignore
    }
    this.languages = Lists.newArrayList();
    try {
      JSONArray array = publicConfiguration.getJSONArray("languages");
      for (int i=0; i<array.length(); i++) {
        languages.add(array.getString(i));
      }
    } catch (JSONException e) {
      // ignore
    }
  }

  public boolean getJoinWithUsername() {
    try {
      return publicConfiguration.getBoolean("joinWithUsername");
    } catch (JSONException e) {
      return false;
    }
  }

  public String getPublicUrl() {
    try {
      return publicConfiguration.getString("publicUrl");
    } catch (JSONException e) {
      return "";
    }
  }

  public String getUserAccountUrl() {
    String url = getPublicUrl();
    if (!url.endsWith("/")) url = url + "/";
    url = url + "#/profile";
    return url;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public List<UserAttribute> getUserAttributes() {
    return userAttributes;
  }

  public String getReCaptchaKey() {
    try {
      return clientConfiguration.getString("reCaptchaKey");
    } catch (JSONException e) {
      return "";
    }
  }

  public class UserAttribute {

    private final JSONObject attribute;

    private final List<String> values;

    public UserAttribute(JSONObject attribute) {
      this.attribute = attribute;
      this.values = Lists.newArrayList();
      if (attribute.has("values")) {
        try {
          JSONArray array = attribute.getJSONArray("values");
          for (int i=0; i<array.length(); i++) {
            values.add(array.getString(i));
          }
        } catch (JSONException e) {
          // ignore
        }
      }
    }

    public String getName() {
      try {
        return attribute.getString("name");
      } catch (JSONException e) {
        return "";
      }
    }

    public boolean getRequired() {
      try {
        return attribute.getBoolean("required");
      } catch (JSONException e) {
        return false;
      }
    }

    public String getInputType() {
      try {
        String type = attribute.getString("type");
        if ("INTEGER".equals(type) || "NUMBER".equals(type)) return "number";
        if ("BOOLEAN".equals(type)) return "checkbox";
      } catch (JSONException e) {

      }
      return "text";
    }

    public List<String> getValues() {
      return values;
    }
  }

}
