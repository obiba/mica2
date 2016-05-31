package org.obiba.mica.micaConfig.rest;

import com.google.common.base.Splitter;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Service
public class DataAccessPermissionsConfigurationService {

  @Inject
  private SubjectAclService subjectAclService;

  public enum ROLES {
    READER
  }

  public void onSaveDataAccessForm(Map<String, String> old, Map<String, String> current) {
    old.entrySet().stream().forEach(e -> removeDataAccessActionsFromType(e.getKey(), e.getValue()));
    current.entrySet().stream().forEach(e -> applyDataAccessActionsToType(e.getKey(), e.getValue()));
  }

  public void onSaveProjectForm(Map<String, String> old, Map<String, String> current) {
    old.entrySet().stream().forEach(e -> removeProjectActionsFromType(e.getKey(), e.getValue()));
    current.entrySet().stream().forEach(e -> applyProjectActionsToType(e.getKey(), e.getValue()));
  }

  private List<String> separateKey(String key) {
    return Splitter.on(':').splitToList(key);
  }

  private void applyDataAccessActionsToType(@NotNull String key, @NotNull String role) {
    List<String> principalAndType = separateKey(key);
    switch (ROLES.valueOf(role)) {
      case READER:
        subjectAclService.addSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/data-access-request", "VIEW", "*");
        subjectAclService.addSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/file", "VIEW", "/data-access-request");
        break;
    }
  }

  private void removeDataAccessActionsFromType(@NotNull String key, @NotNull String role) {
    List<String> principalAndType = separateKey(key);
    switch (ROLES.valueOf(role)) {
      case READER:
        subjectAclService.removeSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/data-access-request", "VIEW", "*");
        subjectAclService.removeSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/file", "VIEW", "/data-access-request");
        break;
    }
  }

  private void applyProjectActionsToType(@NotNull String key, @NotNull String role) {
    List<String> principalAndType = separateKey(key);
    switch (ROLES.valueOf(role)) {
      case READER:
        subjectAclService.addSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/project", "VIEW", "*");
        subjectAclService.addSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/file", "VIEW", "/project");
        subjectAclService.addSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/draft/project", "VIEW", "*");
        subjectAclService.addSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/draft/file", "VIEW", "/project");
        break;
    }
  }

  private void removeProjectActionsFromType(@NotNull String key, @NotNull String role) {
    List<String> principalAndType = separateKey(key);
    switch (ROLES.valueOf(role)) {
      case READER:
        subjectAclService.removeSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/project", "VIEW", "*");
        subjectAclService.removeSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/file", "VIEW", "/project");
        subjectAclService.removeSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/draft/project", "VIEW", "*");
        subjectAclService.removeSubjectPermission(SubjectAcl.Type.valueOf(principalAndType.get(1)), principalAndType.get(0), "/draft/file", "VIEW", "/project");
        break;
    }
  }
}
