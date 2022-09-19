package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.core.service.AgateServerConfigService;
import org.obiba.mica.core.service.UserAuthService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.controller.domain.AuthConfiguration;
import org.obiba.mica.web.controller.domain.OidcProvider;
import org.obiba.oidc.OIDCAuthProviderSummary;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class SignController extends BaseController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private UserAuthService userAuthService;

  @Inject
  protected AgateServerConfigService agateServerConfigService;

  @GetMapping("/signin")
  public ModelAndView signin(HttpServletRequest request, @RequestParam(value = "redirect", required = false) String redirect,
                             @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    ModelAndView mv = new ModelAndView("signin");

    String lang = getLang(locale, language);
    List<OidcProvider> providers = getOidcProviders(lang, false).stream()
      .map(o -> new OidcProvider(o, getOidcSigninUrl(o.getName(), request, redirect))).collect(Collectors.toList());
    mv.getModel().put("oidcProviders", providers);
    return mv;
  }

  @GetMapping("/signup")
  public ModelAndView signup(HttpServletRequest request, @RequestParam(value = "redirect", required = false) String redirect,
                             @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    if (!micaConfigService.getConfig().isSignupEnabled())
      return new ModelAndView("redirect:" + micaConfigService.getContextPath() + "/");

    ModelAndView mv = new ModelAndView("signup");

    String lang = getLang(locale, language);
    List<OidcProvider> providers = getOidcProviders(lang, true).stream()
      .map(o -> new OidcProvider(o, getOidcSignupUrl(o.getName(), request, redirect))).collect(Collectors.toList());
    mv.getModel().put("oidcProviders", providers);
    mv.getModel().put("authConfig", getAuthConfiguration());

    return mv;
  }

  @GetMapping("/signup-with")
  public ModelAndView signupWith(@CookieValue(value = "u_auth", required = false, defaultValue = "{}") String uAuth) {
    if (!micaConfigService.getConfig().isSignupEnabled())
      return new ModelAndView("redirect:" + micaConfigService.getContextPath() + "/");

    ModelAndView mv = new ModelAndView("signup-with");
    JSONObject uAuthObj;
    try {
      String fixedUAuth = uAuth.replaceAll("\\\\", "");
      uAuthObj = new JSONObject(fixedUAuth);
    } catch (JSONException e) {
      uAuthObj = new JSONObject();
    }

    if (uAuthObj.has("username")) {
      mv.getModel().put("uAuth", uAuthObj);
      mv.getModel().put("authConfig", getAuthConfiguration());
      return mv;
    }
    return new ModelAndView("redirect:/signup");
  }

  @GetMapping("/signout")
  public ModelAndView signout(@RequestParam(value = "cb", required = false) boolean callback) {
    Subject subject = SecurityUtils.getSubject();
    if (!subject.isAuthenticated()) {
      ModelAndView mv = new ModelAndView("signout");
      mv.getModel().put("authenticated", false);
      mv.getModel().put("postLogoutRedirectUri", null);
      return mv;
    }

    String postLogoutRedirectUri = null;
    if (!callback && subject.getPrincipals().getRealmNames().contains("obiba-realm")) {
      // mica signout callback url
      UriBuilder micaBuilder = UriBuilder.fromUri(String.format("%s/signout", micaConfigService.getPublicUrl()))
        .queryParam("cb", "true");
      // agate signout url
      UriBuilder agateBuilder = UriBuilder.fromUri(String.format("%s/signout", agateServerConfigService.getAgateUrl()))
        .queryParam("post_logout_redirect_uri", micaBuilder.build());
      postLogoutRedirectUri = agateBuilder.build().toString();
    }

    ModelAndView mv = new ModelAndView("signout");
    mv.getModel().put("authenticated", true);
    mv.getModel().put("postLogoutRedirectUri", postLogoutRedirectUri);
    return mv;
  }


    @GetMapping("/just-registered")
  public ModelAndView justRegistered(@RequestParam(value = "signin", required = false, defaultValue = "false") boolean canSignin) {
    ModelAndView mv = new ModelAndView("just-registered");
    mv.getModel().put("canSignin", canSignin);
    return mv;
  }

  //
  // Private methods
  //

  private List<OIDCAuthProviderSummary> getOidcProviders(String locale, boolean signupOnly) {
    try {
      return userAuthService.getOidcProviders(locale, signupOnly);
    } catch (Exception e) {
      return Lists.newArrayList();
    }
  }

  private AuthConfiguration getAuthConfiguration() {
    try {
      JSONObject authConfig = userAuthService.getPublicConfiguration();
      JSONObject clientConfig = userAuthService.getClientConfiguration();
      return new AuthConfiguration(authConfig, clientConfig);
    } catch (Exception e) {
      return new AuthConfiguration(new JSONObject(), new JSONObject());
    }
  }

  private String getOidcSigninUrl(String oidcName, HttpServletRequest request, String redirect) {
    // http://localhost:8081/auth/signin/kc-test?redirect=http://localhost:8082

    String agateUrl = agateServerConfigService.getAgateUrl();

    String requestUrl = request.getRequestURL().toString();
    String requestUri = request.getRequestURI();
    String baseUrl = requestUrl.replaceFirst(requestUri, "");

    String contextPath = micaConfigService.getContextPath();

    String redirectUrl = baseUrl;
    if (!Strings.isNullOrEmpty(redirect))
      redirectUrl = String.format("%s%s", baseUrl, redirect.startsWith("/") ? redirect : "/" + redirect);
    else
      redirectUrl = String.format("%s%s", baseUrl, contextPath);

    String signinErrorUrl = baseUrl + contextPath + "/signup-with";

    try {
      return String.format("%s/auth/signin/%s?redirect=%s&signin_error=%s", agateUrl, oidcName, URLEncoder.encode(redirectUrl, "UTF-8"), URLEncoder.encode(signinErrorUrl, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // not supposed to happen
      return String.format("%s/auth/signin/%s", agateUrl, oidcName);
    }
  }

  private String getOidcSignupUrl(String oidcName, HttpServletRequest request, String redirect) {
    // http://localhost:8081/auth/signup/kc-test?redirect=http://localhost:8082

    String agateUrl = agateServerConfigService.getAgateUrl();

    String requestUrl = request.getRequestURL().toString();
    String requestUri = request.getRequestURI();
    String baseUrl = requestUrl.replaceFirst(requestUri, "") + micaConfigService.getContextPath();

    String redirectUrl = baseUrl + "/signup-with";
    if (!Strings.isNullOrEmpty(redirect))
      redirectUrl = String.format("%s%s", baseUrl, redirect.startsWith("/") ? redirect : "/" + redirect);

    String errorUrl = baseUrl + "/error";

    try {
      return String.format("%s/auth/signup/%s?redirect=%s&error=%s", agateUrl, oidcName, URLEncoder.encode(redirectUrl, "UTF-8"), URLEncoder.encode(errorUrl, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // not supposed to happen
      return String.format("%s/auth/signup/%s", agateUrl, oidcName);
    }
  }

}
