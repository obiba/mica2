package org.obiba.mica.web.controller;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Serves the custom pages deployed by the administrator (MICA_HOME/conf/templates). The built-in templates are not
 * reachable through this controller: they have their own controllers and access rules.
 */
@Controller
public class PagesController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(PagesController.class);

  private static final Pattern PAGE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");

  private final Set<String> builtInTemplates = ConcurrentHashMap.newKeySet();

  private volatile boolean builtInTemplatesLoaded = false;

  @GetMapping("/page/{page}")
  public ModelAndView get(HttpServletRequest request, @PathVariable String page) {
    if (!PAGE_NAME_PATTERN.matcher(page).matches() || isBuiltInTemplate(page))
      throw new NoSuchElementException("No such page: " + page);

    ModelAndView mv = new ModelAndView(page);

    String qs = request.getQueryString();
    if (!Strings.isNullOrEmpty(qs)) {
      Map<String, String> query = Maps.newHashMap();
      for (String param : Splitter.on("&").split(qs)) {
        String[] tokens = param.split("=");
        if (tokens.length>1) {
          try {
            // values are HTML-encoded: custom pages are not expected to escape them
            query.put(ESAPI.encoder().encodeForHTML(URLDecoder.decode(tokens[0], "UTF-8")),
              ESAPI.encoder().encodeForHTML(URLDecoder.decode(tokens[1], "UTF-8")));
          } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            // not supposed to happen / malformed encoding: ignore the parameter
          }
        }
      }
      if (!query.isEmpty()) mv.getModel().put("query", query);
    }

    return mv;
  }

  private boolean isBuiltInTemplate(String page) {
    if (!builtInTemplatesLoaded) {
      synchronized (builtInTemplates) {
        if (!builtInTemplatesLoaded) {
          builtInTemplates.addAll(loadBuiltInTemplateNames());
          builtInTemplatesLoaded = true;
        }
      }
    }
    return builtInTemplates.contains(page);
  }

  private Set<String> loadBuiltInTemplateNames() {
    Set<String> names = ConcurrentHashMap.newKeySet();
    try {
      for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:_templates/*.ftl")) {
        String filename = resource.getFilename();
        if (filename != null && filename.endsWith(".ftl"))
          names.add(filename.substring(0, filename.length() - ".ftl".length()));
      }
    } catch (IOException e) {
      log.warn("Cannot list built-in templates", e);
      return Collections.emptySet();
    }
    return names;
  }

}
