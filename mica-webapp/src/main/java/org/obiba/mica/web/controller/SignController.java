package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.core.service.AgateServerConfigService;
import org.obiba.mica.core.service.UserAuthService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.AuthConfiguration;
import org.obiba.mica.web.controller.domain.OidcProvider;
import org.obiba.oidc.OIDCAuthProviderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SignController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(SignController.class);

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private UserAuthService userAuthService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  protected AgateServerConfigService agateServerConfigService;

  /**
   * Controller for redirect URL.
   *
   * @param redirect
   * @return
   */
  @GetMapping("/check")
  public ModelAndView check(@RequestParam(value = "redirect", required = false) String redirect) {
    String verifiedRedirect = verifyRedirect(redirect);
    return new ModelAndView("redirect:" + (Strings.isNullOrEmpty(verifiedRedirect) ? micaConfigService.getContextPath() + "/" : verifiedRedirect));
  }

  @GetMapping("/signin")
  public ModelAndView signin(HttpServletRequest request, @RequestParam(value = "redirect", required = false) String redirect,
                             @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    ModelAndView mv = new ModelAndView("signin");

    String lang = getLang(locale, language);
    List<OidcProvider> providers = getOidcProviders(lang, false).stream()
      .map(o -> new OidcProvider(o, getOidcSigninUrl(o.getName(), request, verifyRedirect(redirect)))).collect(Collectors.toList());
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
      .map(o -> new OidcProvider(o, getOidcSignupUrl(o.getName(), request, verifyRedirect(redirect)))).collect(Collectors.toList());
    mv.getModel().put("oidcProviders", providers);
    mv.getModel().put("authConfig", getAuthConfiguration());

    return mv;
  }

  @GetMapping("/signup-with")
  public ModelAndView signupWith(@CookieValue(value = "u_auth", required = false, defaultValue = "{}") String uAuth) {
    // second chance if cookie was not available yet
    return signupWithOrRedirect(uAuth, "/signup-with-u");
  }

  @GetMapping("/signup-with-u")
  public ModelAndView signupWithU(@CookieValue(value = "u_auth", required = false, defaultValue = "{}") String uAuth) {
    // if no cookie, go back to signup
    return signupWithOrRedirect(uAuth, "/signup");
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
    if (!callback) { // we are not in the logout callback
      if (subject.getPrincipals().getRealmNames().contains("obiba-realm")) {
        // not an ini realm user
        // check if agate has delegated to an external ID provider
        String username = subject.getPrincipal().toString();
        Map<String, Object> params = userProfileService.getProfileMap(username, true);
        if (params.containsKey("attributes")) {
          Map<String, String> attrs = (Map<String, String>) params.get("attributes");
          if (attrs.containsKey("realm") && !"agate-user-realm".equals(attrs.get("realm"))) {
            postLogoutRedirectUri = makePostLogoutRedirectUri();
          }
        }
      }
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

  private ModelAndView signupWithOrRedirect(String uAuth, String redirect) {
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

    log.debug("Signup with username {}", uAuth);
    if (uAuthObj.has("username")) {
      mv.getModel().put("uAuth", uAuthObj);
      mv.getModel().put("authConfig", getAuthConfiguration());
      return mv;
    }
    log.debug("Redirecting to " + redirect);
    return new ModelAndView("redirect:" + redirect);
  }

  private String makePostLogoutRedirectUri() {
    // mica signout callback url
    UriBuilder micaBuilder = UriBuilder.fromUri(String.format("%s/signout", micaConfigService.getPublicUrl()))
      .queryParam("cb", "true");
    // agate signout url
    UriBuilder agateBuilder = UriBuilder.fromUri(String.format("%s/signout", agateServerConfigService.getAgateUrl()))
      .queryParam("post_logout_redirect_uri", micaBuilder.build());
    return agateBuilder.build().toString();
  }

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

    String requestUrl = getRequestUrl(request);
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

    String requestUrl = getRequestUrl(request);
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

  /**
   * Extract request url from request object, and enforce https for non localhost server.
   *
   * @param request
   * @return
   */
  private String getRequestUrl(HttpServletRequest request) {
    String requestUrl = request.getRequestURL().toString();
    if (requestUrl.startsWith("http://") && !requestUrl.startsWith("http://localhost:") && !requestUrl.startsWith("http://127.0.0.1:")) {
      return requestUrl.replaceFirst("http://", "https://");
    }
    return requestUrl;
  }

  private String verifyRedirect(String redirect) {
    String agateUrl = agateServerConfigService.getAgateUrl();
    if (Strings.isNullOrEmpty(redirect) || redirect.startsWith("/") || redirect.startsWith(agateUrl)) return redirect;
    return "";
  }
}
