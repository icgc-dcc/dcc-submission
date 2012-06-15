/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.model.ResponseTimestamper;
import org.icgc.dcc.model.dictionary.CodeList;
import org.icgc.dcc.model.dictionary.DictionaryService;

import com.google.inject.Inject;

@Path("codeLists")
public class CodeListResource {
  @Inject
  private DictionaryService dictionaries;

  @GET
  public Response getCodeLists() {
    List<CodeList> codeLists = this.dictionaries.listCodeList();
    if(codeLists == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(codeLists).build();
  }

  @POST
  public Response createCodeList(String name) {
    checkArgument(name != null);
    CodeList c = this.dictionaries.createCodeList(name);
    return ResponseTimestamper.ok(c).build();
  }

  @GET
  @Path("{name}")
  public Response getCodeList(@PathParam("name") String name) {
    CodeList c = this.dictionaries.getCodeList(name);
    if(c == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return ResponseTimestamper.ok(c).build();
  }

  @PUT
  @Path("{name}")
  public Response updateCodeList(@PathParam("name") String name, CodeList newCodeList, @Context Request req) {
    CodeList oldCodeList = this.dictionaries.getCodeList(name);
    if(oldCodeList == null) {
      return Response.status(Status.NOT_FOUND).build();
    } else if(newCodeList.getName().equals(name) == false) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    ResponseTimestamper.evaluate(req, oldCodeList);
    this.dictionaries.updateCodeList(newCodeList);

    return ResponseTimestamper.ok(newCodeList).build();
  }
}
