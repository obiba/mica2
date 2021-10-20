package org.obiba.mica.rest;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.AuthorizationException;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.Collection;

public abstract class AbstractPublishedDocumentsSetResource<T extends DocumentSetService> {

  private final JoinQueryExecutor joinQueryExecutor;

  protected final MicaConfigService micaConfigService;

  protected final SubjectAclService subjectAclService;

  private final Searcher searcher;

  protected final Dtos dtos;

  protected final PersonService personService;

  protected AbstractPublishedDocumentsSetResource(JoinQueryExecutor joinQueryExecutor,
                                                  MicaConfigService micaConfigService,
                                                  SubjectAclService subjectAclService,
                                                  Searcher searcher,
                                                  Dtos dtos,
                                                  PersonService personService) {
    this.joinQueryExecutor = joinQueryExecutor;
    this.micaConfigService = micaConfigService;
    this.subjectAclService = subjectAclService;
    this.searcher = searcher;
    this.dtos = dtos;
    this.personService = personService;
  }

  protected abstract T getDocumentSetService();

  protected abstract boolean isCartEnabled(MicaConfig config);

  protected Mica.DocumentSetDto getDocumentSet(String id) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    getDocumentSetService().touch(documentSet);
    return dtos.asDto(documentSet);
  }

  protected void deleteDocumentSet(String id) {
    getDocumentSetService().delete(getSecuredDocumentSet(id));
  }

  protected Mica.DocumentSetDto importDocuments(String id, String body) {
    DocumentSet set = getSecuredDocumentSet(id);
    if (Strings.isNullOrEmpty(body)) return dtos.asDto(set);
    getDocumentSetService().addIdentifiers(id, getDocumentSetService().extractIdentifiers(body));
    return dtos.asDto(getDocumentSetService().get(id));
  }

  protected StreamingOutput exportDocuments(String id) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    getDocumentSetService().touch(documentSet);
    return toStream(documentSet.getIdentifiers());
  }

  protected Mica.DocumentSetDto deleteDocuments(String id, String body) {
    DocumentSet set = getSecuredDocumentSet(id);
    if (Strings.isNullOrEmpty(body)) return dtos.asDto(set);
    getDocumentSetService().removeIdentifiers(id, getDocumentSetService().extractIdentifiers(body));
    return dtos.asDto(getDocumentSetService().get(id));
  }

  protected void deleteDocuments(String id) {
    getDocumentSetService().setIdentifiers(id, Lists.newArrayList());
  }

  protected boolean hasDocument(String id, String documentId) {
    DocumentSet set = getSecuredDocumentSet(id);
    return set.getIdentifiers().contains(documentId);
  }

  protected DocumentSet getSecuredDocumentSet(String id) {
    DocumentSet documentSet = getDocumentSetService().get(id);
    if (!subjectAclService.isCurrentUser(documentSet.getUsername()) && !subjectAclService.isAdministrator() && !subjectAclService.isDataAccessOfficer())
      throw new AuthorizationException();
    boolean enabled = isCartEnabled(micaConfigService.getConfig());
    if (!enabled && !documentSet.hasName()) throw new AuthorizationException(); // cart
    if (enabled && !subjectAclService.hasMicaRole() && !documentSet.hasName())
      throw new AuthorizationException(); // cart
    if (documentSet.hasName() && !subjectAclService.hasMicaRole()) throw new AuthorizationException();
    getDocumentSetService().touch(documentSet);
    return documentSet;
  }

  protected MicaSearch.JoinQueryResultDto makeQuery(QueryType type, String query) {
    return joinQueryExecutor.query(type, searcher.makeJoinQuery(query));
  }

  private StreamingOutput toStream(Collection<String> items) {
    return os -> {
      items.forEach(item -> {
        try {
          os.write((item + "\n").getBytes());
        } catch (IOException e) {
          //
        }
      });
      os.flush();
    };
  }

}
