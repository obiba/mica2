package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import org.obiba.mica.core.service.AgateServerConfigService;
import org.obiba.mica.core.service.OidcProvidersService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
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
public class SigninController {

  @Inject
  private OidcProvidersService oidcProvidersService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  protected AgateServerConfigService agateServerConfigService;

  @GetMapping("/signin")
  public ModelAndView get(HttpServletRequest request, @RequestParam(value = "redirect", required = false) String redirect,
                          @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                          @RequestParam(value = "language", required = false) String language) {
    ModelAndView mv = new ModelAndView("signin");

    String lang = language == null ? locale : language;
    List<OidcProvider> providers = oidcProvidersService.getProviders(lang).stream()
      .map(o -> new OidcProvider(o, getOidcSigninUrl(o.getName(), request, redirect))).collect(Collectors.toList());
    mv.getModel().put("oidcProviders", providers);
    return mv;
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
