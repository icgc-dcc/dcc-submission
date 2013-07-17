/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.web;

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.submission.web.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.submission.web.Authorizations.unauthorizedResponse;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Path("codeLists")
public class CodeListResource {

  private static final Logger log = LoggerFactory.getLogger(CodeListResource.class);

  @Inject
  private DictionaryService dictionaries;

  /**
   * Open-access intentional (DCC-758)
   */
  @GET
  public Response getCodeLists() {
    List<CodeList> codeLists = this.dictionaries.listCodeList();
    if (codeLists == null) {
      codeLists = Lists.newArrayList();
    }
    return Response.ok(codeLists).build();
  }

  @POST
  public Response addCodeLists(
      @Valid
      List<CodeList> codeLists,
      @Context
      SecurityContext securityContext) {
    log.info("Adding codelists: {}", codeLists);
    if (isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    checkArgument(codeLists != null);
    this.dictionaries.addCodeList(codeLists);
    return Response.status(Status.CREATED).build();
  }

  @GET
  @Path("{name}")
  public Response getCodeList(
      @PathParam("name")
      String name) {
    /* no authorization check necessary */

    log.debug("Getting codelist: {}", name);
    checkArgument(name != null);
    Optional<CodeList> optional = this.dictionaries.getCodeList(name);
    if (optional.isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    }
    return ResponseTimestamper.ok(optional.get()).build();
  }

  /**
   * NOTE: If allow more than updating label at some point, must reset Submission states...
   */
  @PUT
  @Path("{name}")
  public Response updateCodeList(
      @PathParam("name")
      String name,
      @Valid
      CodeList newCodeList,
      @Context
      Request req,
      @Context
      SecurityContext securityContext) {

    log.info("Updating codelist: {} with {}", name, newCodeList);
    if (isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    checkArgument(name != null);
    checkArgument(newCodeList != null);

    Optional<CodeList> optional = this.dictionaries.getCodeList(name);
    if (optional.isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    } else if (newCodeList.getName().equals(name) == false) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NAME_MISMATCH, newCodeList.getName(), name)).build();
    }
    ResponseTimestamper.evaluate(req, optional.get());
    this.dictionaries.updateCodeList(newCodeList);

    return Response.status(Status.NO_CONTENT).build(); // DCC-820: add ResponseTimestamper back here?
  }

  @POST
  @Path("{name}/terms")
  public Response addTerms(
      @PathParam("name")
      String name,
      @Valid
      List<Term> terms,
      @Context
      Request req,
      @Context
      SecurityContext securityContext) {

    log.info("Adding term {} to codelist {}", terms, name);
    if (isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    checkArgument(name != null);
    checkArgument(terms != null);
    Optional<CodeList> optional = this.dictionaries.getCodeList(name);
    if (optional.isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    }
    CodeList codeList = optional.get();
    ResponseTimestamper.evaluate(req, codeList);

    // First check if the terms exist. The DictionaryService addTerm method checks too, but we don't want to add some of
    // the list and then have it fail part way through
    for (Term term : terms) {
      if (codeList.containsTerm(term)) {
        return Response.status(Status.BAD_REQUEST)
            .entity(new ServerErrorResponseMessage(ServerErrorCode.ALREADY_EXISTS, term.getCode())).build();
      }
    }
    for (Term term : terms) {
      this.dictionaries.addTerm(name, term);
    }

    return Response.status(Status.CREATED).build();
  }
}