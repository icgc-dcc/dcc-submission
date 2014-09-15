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
package org.icgc.dcc.submission.web.util;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NO_SUCH_ENTITY;

import javax.ws.rs.core.Response;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class Responses {

  public static final Response.StatusType UNPROCESSABLE_ENTITY = new Response.StatusType() {

    @Override
    public int getStatusCode() {
      return 422;
    }

    @Override
    public Response.Status.Family getFamily() {
      return Response.Status.Family.CLIENT_ERROR;
    }

    @Override
    public String getReasonPhrase() {
      return "Unprocessable Entity";
    }

  };

  public static Response noSuchEntityResponse(String... names) {
    return noSuchEntityResponse(false, names);
  }

  public static Response noSuchEntityResponse(boolean important, String... names) {
    ServerErrorResponseMessage errorMessage =
        new ServerErrorResponseMessage(NO_SUCH_ENTITY, (Object[]) names);
    if (important) {
      log.info("No such entity: {}", errorMessage);
    }
    return Response.status(NOT_FOUND).entity(errorMessage).build();
  }

  public static Response notFound(String name) {
    return Response
        .status(NOT_FOUND)
        .entity(new ServerErrorResponseMessage(NO_SUCH_ENTITY, name))
        .build();
  }

  public static Response badRequest(ServerErrorCode code, Object... args) {
    return Response
        .status(BAD_REQUEST)
        .entity(new ServerErrorResponseMessage(code, args))
        .build();
  }

  public static Response created() {
    return Response
        .status(CREATED)
        .build();
  }

  public static Response noContent() {
    return Response
        .status(NO_CONTENT)
        .build();
  }

  public static Response unauthorizedResponse() {
    return Responses.unauthorizedResponse(false);
  }

  public static Response unauthorizedResponse(boolean important) {
    ServerErrorResponseMessage errorMessage = new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED);
    if (important) {
      log.info("unauthorized action: {}", errorMessage);
    }
    return Response.status(UNAUTHORIZED).entity(errorMessage).build();
  }

}
