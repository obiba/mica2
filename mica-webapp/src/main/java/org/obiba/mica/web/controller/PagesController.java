package org.obiba.mica.web.controller;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

@Controller
public class PagesController extends BaseController {

  @GetMapping("/page/{page}")
  public ModelAndView get(HttpServletRequest request, @PathVariable String page) {
    ModelAndView mv = new ModelAndView(page);

    String qs = request.getQueryString();
    if (!Strings.isNullOrEmpty(qs)) {
      Map<String, String> query = Maps.newHashMap();
      for (String param : Splitter.on("&").split(qs)) {
        String[] tokens = param.split("=");
        if (tokens.length>1) {
          try {
            query.put(URLDecoder.decode(tokens[0], "UTF-8"), URLDecoder.decode(tokens[1], "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            // not supposed to happen
          }
        }
      }
      if (!query.isEmpty()) mv.getModel().put("query", query);
    }

    return mv;
  }

}
