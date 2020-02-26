package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import org.json.JSONObject;
import org.obiba.mica.core.service.AgateServerConfigService;
import org.obiba.mica.core.service.UserAuthService;
import org.obiba.mica.web.controller.domain.AuthConfiguration;
import org.obiba.mica.web.controller.domain.OidcProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SignController {

  @Inject
  private UserAuthService userAuthService;

  @Inject
  protected AgateServerConfigService agateServerConfigService;

  @GetMapping("/signin")
  public ModelAndView signin(HttpServletRequest request, @RequestParam(value = "redirect", required = false) String redirect,
                             @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    ModelAndView mv = new ModelAndView("signin");

    String lang = getLang(language, locale);
    List<OidcProvider> providers = userAuthService.getOidcProviders(lang).stream()
      .map(o -> new OidcProvider(o, getOidcSigninUrl(o.getName(), request, redirect))).collect(Collectors.toList());
    mv.getModel().put("oidcProviders", providers);
    return mv;
  }

  @GetMapping("/signup")
  public ModelAndView signup(HttpServletRequest request, @RequestParam(value = "redirect", required = false) String redirect,
                             @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    ModelAndView mv = new ModelAndView("signup");

    String lang = getLang(language, locale);
    List<OidcProvider> providers = userAuthService.getOidcProviders(lang, true).stream()
      .map(o -> new OidcProvider(o, getOidcSigninUrl(o.getName(), request, redirect))).collect(Collectors.toList());
    mv.getModel().put("oidcProviders", providers);

    JSONObject authConfig = userAuthService.getPublicConfiguration();
    JSONObject clientConfig = userAuthService.getClientConfiguration();
    mv.getModel().put("authConfig", new AuthConfiguration(authConfig, clientConfig));

    return mv;
  }

  private String getLang(String language, String locale) {
    return language == null ? locale : language;
  }

  private String getOidcSigninUrl(String oidcName, HttpServletRequest request, String redirect) {
    // http://localhost:8081/auth/signin/kc-test?redirect=http://localhost:8082

    String agateUrl = agateServerConfigService.getAgateUrl();

    String requestUrl = request.getRequestURL().toString();
    String requestUri = request.getRequestURI();
    String baseUrl = requestUrl.replaceFirst(requestUri, "");

    String redirectUrl = baseUrl;
    if (!Strings.isNullOrEmpty(redirect))
      redirectUrl = String.format("%s/%s", baseUrl, redirect);

    try {
      return String.format("%s/auth/signin/%s?redirect=%s", agateUrl, oidcName, URLEncoder.encode(redirectUrl, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // not supposed to happen
      return String.format("%s/auth/signin/%s", agateUrl, oidcName);
    }
  }

}
