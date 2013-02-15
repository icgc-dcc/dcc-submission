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
package org.icgc.dcc.web;

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

import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.shiro.ShiroSecurityContext;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

@Path("codeLists")
public class CodeListResource {
  @Inject
  private DictionaryService dictionaries;

  /**
   * Open-access intentional (DCC-758)
   */
  @GET
  public Response getCodeLists() {
    List<CodeList> codeLists = this.dictionaries.listCodeList();
    if(codeLists == null) {
      codeLists = Lists.newArrayList();
    }
    return Response.ok(codeLists).build();
  }

  @POST
  public Response addCodeLists(@Valid List<CodeList> codeLists, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.CODELIST_MODIFY.toString()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    checkArgument(codeLists != null);
    this.dictionaries.addCodeList(codeLists);
    return Response.status(Status.CREATED).build();
  }

  @GET
  @Path("{name}")
  public Response getCodeList(@PathParam("name") String name) {
    checkArgument(name != null);
    Optional<CodeList> optional = this.dictionaries.getCodeList(name);
    if(optional.isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    }
    return ResponseTimestamper.ok(optional.get()).build();
  }

  @PUT
  @Path("{name}")
  public Response updateCodeList(@PathParam("name") String name, @Valid CodeList newCodeList, @Context Request req,
      @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.CODELIST_MODIFY.toString()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    checkArgument(name != null);
    checkArgument(newCodeList != null);

    Optional<CodeList> optional = this.dictionaries.getCodeList(name);
    if(optional.isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    } else if(newCodeList.getName().equals(name) == false) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NAME_MISMATCH, newCodeList.getName(), name)).build();
    }
    ResponseTimestamper.evaluate(req, optional.get());
    this.dictionaries.updateCodeList(newCodeList);

    return ResponseTimestamper.ok(newCodeList).build();
  }

  @POST
  @Path("{name}/terms")
  public Response addTerms(@PathParam("name") String name, @Valid List<Term> terms, @Context Request req,
      @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.CODELIST_MODIFY.toString()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    checkArgument(name != null);
    checkArgument(terms != null);
    Optional<CodeList> optional = this.dictionaries.getCodeList(name);
    if(optional.isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    }
    CodeList codeList = optional.get();
    ResponseTimestamper.evaluate(req, codeList);

    // First check if the terms exist. The DictionaryService addTerm method checks too, but we don't want to add some of
    // the list and then have it fail part way through
    for(Term term : terms) {
      if(codeList.containsTerm(term)) {
        return Response.status(Status.BAD_REQUEST)
            .entity(new ServerErrorResponseMessage(ServerErrorCode.ALREADY_EXISTS, term.getCode())).build();
      }
    }
    for(Term term : terms) {
      this.dictionaries.addTerm(name, term);
    }

    return ResponseTimestamper.ok(codeList).build();
  }
}
