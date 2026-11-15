/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.rest;

import java.util.Arrays;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.web.model.ErrorDtos.ClientErrorDto;

/**
 * The person endpoints errors, as a {@link ClientErrorDto}: clients tell them apart (e.g. the two 409s) and translate
 * them by their message template; the message is the English text.
 */
final class PersonErrors {

  static final String MISSING = "server.error.person.missing";
  static final String ID_NOT_ALLOWED = "server.error.person.id-not-allowed";
  static final String ID_MISMATCH = "server.error.person.id-mismatch";
  static final String VERSION_MISSING = "server.error.person.version-missing";
  static final String MEMBERSHIP_REQUIRED = "server.error.person.membership-required";
  static final String NO_SUCH_STUDY = "server.error.person.no-such-study";
  static final String NO_SUCH_NETWORK = "server.error.person.no-such-network";
  static final String INVALID_ROLE = "server.error.person.invalid-role";
  static final String INVALID = "server.error.person.invalid";
  static final String EMAIL_USED = "server.error.person.email-used";
  static final String EMAIL_USED_LINK_EXISTING = "server.error.person.email-used-link-existing";
  static final String VERSION_CONFLICT = "server.error.person.version-conflict";

  private PersonErrors() {}

  static BadRequestException missing() {
    return badRequest(MISSING, "The person is missing.");
  }

  static BadRequestException idNotAllowed() {
    return badRequest(ID_NOT_ALLOWED, "A new person has no id.");
  }

  static BadRequestException idMismatch() {
    return badRequest(ID_MISMATCH, "The person id does not match.");
  }

  static BadRequestException versionMissing() {
    return badRequest(VERSION_MISSING, "The person version is missing.");
  }

  static BadRequestException membershipRequired() {
    return badRequest(MEMBERSHIP_REQUIRED, "A new person must be a member of a study or a network.");
  }

  static BadRequestException noSuchStudy(String id) {
    return badRequest(NO_SUCH_STUDY, "No such study: " + id, id);
  }

  static BadRequestException noSuchNetwork(String id) {
    return badRequest(NO_SUCH_NETWORK, "No such network: " + id, id);
  }

  /** throws {@link #invalidRole(String)} when the role is not one of the configured roles */
  static void checkRole(MicaConfigService micaConfigService, String role) {
    if (!micaConfigService.getRoles().contains(role)) throw invalidRole(role);
  }

  static BadRequestException invalidRole(String role) {
    return badRequest(INVALID_ROLE, String.format("'%s' is not a valid role", role), role);
  }

  /** bean validation failures, as "property message" pairs */
  static BadRequestException invalid(String violations) {
    return badRequest(INVALID, "The person is not valid: " + violations, violations);
  }

  static ClientErrorException emailUsed() {
    return conflict(EMAIL_USED, "The email is already used by another person.");
  }

  /** on a scoped create: the existing person is to be linked instead */
  static ClientErrorException emailUsedLinkExisting() {
    return conflict(EMAIL_USED_LINK_EXISTING, "The email is already used by another person: add the existing person instead.");
  }

  /** the stored person changed since the client read it (version), the client is to reload it */
  static ClientErrorException versionConflict() {
    return conflict(VERSION_CONFLICT, "Someone else modified this person in the meantime. Edit it again.");
  }

  private static BadRequestException badRequest(String template, String message, String... arguments) {
    return new BadRequestException(message, response(Response.Status.BAD_REQUEST, template, message, arguments));
  }

  private static ClientErrorException conflict(String template, String message, String... arguments) {
    return new ClientErrorException(message, response(Response.Status.CONFLICT, template, message, arguments));
  }

  private static Response response(Response.Status status, String template, String message, String... arguments) {
    ClientErrorDto error = ClientErrorDto.newBuilder()
      .setCode(status.getStatusCode())
      .setMessageTemplate(template)
      .setMessage(message)
      .addAllArguments(Arrays.asList(arguments))
      .build();
    return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(error).build();
  }
}
