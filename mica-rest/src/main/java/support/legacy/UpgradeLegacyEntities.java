package support.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class UpgradeLegacyEntities {

  static private void upgradeMemberships(JsonNode jsonNode) {
    List<JsonNode> memberships = jsonNode.findValues("memberships");
    memberships.forEach(membership -> {
      membership.findValues("studyMemberships").forEach(studyMembership -> studyMembership.forEach(study -> {
        JsonNode metaType = study.get("obiba.mica.PersonDto.StudyMembershipDto.meta").get("type");
        if (metaType.asText().equals("harmonization-study")) {
          ((ObjectNode) study).put("type", "INITIATIVE");
        } else if (metaType.asText().equals("individual-study")) {
          ((ObjectNode) study).put("type", "STUDY");
        }

        ((ObjectNode) study).remove("obiba.mica.PersonDto.StudyMembershipDto.meta");
      }));

      membership.findValues("networkMemberships").forEach(networkMembership -> networkMembership.forEach(network -> {
        ((ObjectNode) network).put("type", "NETWORK");
      }));
    });
  }


  static public String  upgradeStudy(String content) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = null;
    try {
      jsonNode = mapper.readTree(content);

      if (jsonNode.has("obiba.mica.CollectionStudyDto.type")) {
        ((ObjectNode) jsonNode).remove("obiba.mica.CollectionStudyDto.type");
      } else if (jsonNode.has("obiba.mica.HarmonizationStudyDto.type")) {
        ((ObjectNode) jsonNode).remove("obiba.mica.HarmonizationStudyDto.type");
      }

      upgradeMemberships(jsonNode);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return jsonNode.toString();
  }

  static public String upgradeNetwork(String content) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = null;
    try {
      jsonNode = mapper.readTree(content);
      upgradeMemberships(jsonNode);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return jsonNode.toString();
  }
}
