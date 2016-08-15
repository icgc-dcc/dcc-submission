/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.web.resource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.icgc.dcc.submission.dictionary.model.DictionaryState.OPENED;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.MISSING_REQUIRED_DATA;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NAME_MISMATCH;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NO_SUCH_ENTITY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.RESOURCE_CLOSED;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.UNPROCESSABLE_ENTITY;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.icgc.dcc.submission.dictionary.DictionaryValidator;
import org.icgc.dcc.submission.dictionary.DictionaryValidator.DictionaryConstraintViolations;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.service.DictionaryService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("dictionaries")
public class DictionaryResource {

  /**
   * Custom HTTP headers for validation.
   */
  public static final String VALIDATION_ERROR_HEADER = "X-Validation-Error";
  public static final String VALIDATION_WARNING_HEADER = "X-Validation-Warning";

  @Autowired
  private DictionaryService dictionaryService;

  private final boolean validate = true;

  @GET
  public Response getDictionaryService() {
    // No authorization check necessary
    log.debug("Getting dictionaryService");
    List<Dictionary> dictionaries = dictionaryService.getDictionaries();
    if (dictionaries == null) {
      dictionaries = newArrayList();
    }

    return Response.ok(dictionaries).build();
  }

  @GET
  @Path("/versions")
  public Response getDictionaryVersions() {
    val response = dictionaryService.getVersions();
    if (response == null) {
      return Response.ok(newArrayList()).build();
    }
    return Response.ok(response).build();
  }

  @GET
  @Path("/current")
  public Response getCurrentDictionary() {
    val response = dictionaryService.getCurrentDictionary();
    return Response.ok(response).build();
  }

  @GET
  @Path("{version}")
  public Response getDictionary(

      @PathParam("version") String version

  ) {
    // No authorization check necessary
    log.debug("Getting dictionary: {}", version);
    Dictionary dict = this.dictionaryService.getDictionaryByVersion(version);
    if (dict == null) {
      return Response.status(NOT_FOUND)
          .entity(new ServerErrorResponseMessage(NO_SUCH_ENTITY, version)).build();
    }

    return ResponseTimestamper.ok(dict).build();
  }

  /**
   * See {@link DictionaryService#addDictionary(Dictionary)} for details.
   */
  @POST
  public Response addDictionary(

      @Valid Dictionary dict,

      @Context SecurityContext securityContext

  ) {
    log.info("Adding dictionary: {}", dict == null ? null : dict.getVersion());
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    val violations = validateDictionary(dict);
    if (violations.hasErrors()) {
      val errors = new StringBuilder("The request entity had the following errors:\n");
      for (val error : violations.getErrors()) {
        errors.append("  * ").append(error).append('\n');
      }

      return Response
          .status(UNPROCESSABLE_ENTITY)
          .header(VALIDATION_ERROR_HEADER, errors)
          .build();
    }

    dictionaryService.addDictionary(dict);

    val url = UriBuilder.fromResource(DictionaryResource.class).path(dict.getVersion()).build();

    if (violations.hasWarnings()) {
      val warnings = new StringBuilder("Created, but request entity had the following warnings:\n");
      for (val error : violations.getErrors()) {
        warnings.append("  * ").append(error).append('\n');
      }

      return Response
          .created(url)
          .header(VALIDATION_WARNING_HEADER, warnings)
          .build();
    }

    return Response.created(url).build();
  }

  @PUT
  @Path("{version}")
  public Response updateDictionary(

      @PathParam("version") String version,

      @QueryParam("reset") @DefaultValue("true") boolean reset, // Reset by default

      @Valid Dictionary newDictionary,

      @Context Request request,

      @Context SecurityContext securityContext

  ) {
    checkArgument(version != null);
    checkArgument(newDictionary != null);
    checkArgument(newDictionary.getVersion() != null);

    log.info("Updating dictionary: {} with {}", version, newDictionary.getVersion());
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    val oldDictionary = dictionaryService.getDictionaryByVersion(version);
    if (oldDictionary == null) {
      return Response
          .status(NOT_FOUND)
          .entity(new ServerErrorResponseMessage(NO_SUCH_ENTITY, version))
          .build();
    } else if (oldDictionary.getState() != OPENED) {
      // TODO: move check to dictionaryService.update() instead
      return Response
          .status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(RESOURCE_CLOSED, version))
          .build();
    } else if (newDictionary.getVersion() == null) {
      return Response
          .status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(MISSING_REQUIRED_DATA, "dictionary version"))
          .build();
    } else if (newDictionary.getVersion().equals(version) == false) {
      return Response
          .status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(NAME_MISMATCH, version, newDictionary.getVersion()))
          .build();
    }

    ResponseTimestamper.evaluate(request, oldDictionary);

    val violations = validateDictionary(newDictionary);
    if (violations.hasErrors()) {
      val errors = new StringBuilder("The request entity had the following errors:\n");
      for (val error : violations.getErrors()) {
        errors.append("  * ").append(error).append('\n');
      }

      return Response
          .status(UNPROCESSABLE_ENTITY)
          .header(VALIDATION_ERROR_HEADER, errors)
          .build();
    }

    dictionaryService.updateDictionary(newDictionary, reset);

    if (violations.hasWarnings()) {
      val warnings = new StringBuilder("Created, but request entity had the following warnings:\n");
      for (val error : violations.getErrors()) {
        warnings.append("  * ").append(error).append('\n');
      }

      return Response
          .status(NO_CONTENT)
          .header(VALIDATION_WARNING_HEADER, warnings)
          .build();
    }

    // http://stackoverflow.com/questions/797834/should-a-restful-put-operation-return-something
    return Response
        .status(NO_CONTENT)
        .build();
  }

  private DictionaryConstraintViolations validateDictionary(Dictionary dictionary) {
    if (validate) {
      val validator = new DictionaryValidator(dictionary, dictionaryService.getCodeLists());
      return validator.validate();
    } else {
      val empty = Collections.<DictionaryValidator.DictionaryConstraintViolation> emptySet();
      return new DictionaryConstraintViolations(empty, empty);
    }
  }

}
