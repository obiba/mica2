package org.obiba.mica.web.controller;

import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DashboardController {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Searcher searcher;

  @GetMapping("/dashboard")
  public ModelAndView dashboard() {

    int from = 0;
    int limit = 10;
    String sort = "name";
    String order = "asc";
    String locale = "en";

    String rql = RQLQueryBuilder.newInstance().target(
      RQLQueryBuilder.TargetQueryBuilder.studyInstance().exists("id").limit(from, limit).sort(sort, order).build())
      .locale(locale).buildArgsAsString();

    JoinQueryExecutor joinQueryExecutor = applicationContext.getBean(JoinQueryExecutor.class);
    MicaSearch.JoinQueryResultDto result = joinQueryExecutor.query(QueryType.STUDY, searcher.makeJoinQuery(rql));

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("stats", result);
    return new ModelAndView("dashboard", params);
  }

}
