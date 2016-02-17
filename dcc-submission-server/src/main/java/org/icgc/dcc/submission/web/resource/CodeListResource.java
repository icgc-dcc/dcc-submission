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
import static javax.ws.rs.core.Response.ok;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.ALREADY_EXISTS;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NAME_MISMATCH;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.UNPROCESSABLE_ENTITY;
import static org.icgc.dcc.submission.web.util.Responses.badRequest;
import static org.icgc.dcc.submission.web.util.Responses.created;
import static org.icgc.dcc.submission.web.util.Responses.noContent;
import static org.icgc.dcc.submission.web.util.Responses.notFound;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.service.DictionaryService;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

@Slf4j
@Path("codeLists")
public class CodeListResource {

  @Inject
  private DictionaryService dictionaryService;

  /**
   * Open-access intentional (DCC-758)
   */
  @GET
  public Response getCodeLists() {
    List<CodeList> codeLists = dictionaryService.getCodeLists();
    if (codeLists == null) {
      codeLists = newArrayList();
    }

    return ok(codeLists).build();
  }

  @POST
  public Response addCodeLists(

      @Valid List<CodeList> codeLists,

      @Context SecurityContext securityContext

      )
  {
    log.info("Adding codelists: {}", codeLists);
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    for (val codeList : codeLists) {
      val codes = Sets.<String> newHashSet();
      val values = Sets.<String> newHashSet();
      for (val term : codeList.getTerms()) {
        if (codes.contains(term.getCode()) || values.contains(term.getValue())) {
          log.warn("Code or value of {} is duplicated in {}", term, codeList);
          return Response
              .status(UNPROCESSABLE_ENTITY)
              .header(DictionaryResource.VALIDATION_ERROR_HEADER,
                  "Code or value in " + term + " is duplicated in " + codeList)
              .build();
        } else {
          codes.add(term.getCode());
          values.add(term.getValue());
        }
      }
    }

    checkArgument(codeLists != null);
    dictionaryService.addCodeList(codeLists);

    return created();
  }

  @GET
  @Path("{name}")
  public Response getCodeList(

      @PathParam("name") String name

      )
  {
    // No authorization check necessary
    log.debug("Getting codelist: {}", name);
    checkArgument(name != null);

    val optional = dictionaryService.getCodeList(name);
    if (!optional.isPresent()) {
      return notFound(name);
    }

    return ResponseTimestamper
        .ok(optional.get())
        .build();
  }

  /**
   * NOTE: If allow more than updating label at some point, must reset Submission states...
   */
  @PUT
  @Path("{name}")
  public Response updateCodeList(

      @PathParam("name") String name,

      @Valid CodeList newCodeList,

      @Context Request request,

      @Context SecurityContext securityContext

      )
  {
    log.info("Updating codelist: {} with {}", name, newCodeList);
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    checkArgument(name != null);
    checkArgument(newCodeList != null);

    val optional = dictionaryService.getCodeList(name);
    if (!optional.isPresent()) {
      return notFound(name);
    } else if (newCodeList.getName().equals(name) == false) {
      return badRequest(NAME_MISMATCH, newCodeList.getName(), name);
    }

    ResponseTimestamper.evaluate(request, optional.get());
    dictionaryService.updateCodeList(newCodeList);

    // DCC-820: add ResponseTimestamper back here?
    return noContent();
  }

  @POST
  @Path("{name}/terms")
  public Response addTerms(

      @PathParam("name") String name,

      @Valid List<Term> terms,

      @Context Request request,

      @Context SecurityContext securityContext

      )
  {
    log.info("Adding term {} to codelist {}", terms, name);
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    checkArgument(name != null);
    checkArgument(terms != null);
    val optional = dictionaryService.getCodeList(name);
    if (optional.isPresent() == false) {
      return notFound(name);
    }

    val codeList = optional.get();
    ResponseTimestamper.evaluate(request, codeList);

    // First check if the terms exist. The DictionaryService addTerm method checks too, but we don't want to add some of
    // the list and then have it fail part way through
    for (val term : terms) {
      if (codeList.containsTerm(term)) {
        return badRequest(ALREADY_EXISTS, term.getCode());
      }
    }

    for (val term : terms) {
      dictionaryService.addCodeListTerm(name, term);
    }

    return created();
  }

}
