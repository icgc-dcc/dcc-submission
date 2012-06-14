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
package org.icgc.dcc.model;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Utility class for interaction between responses and objects with {@code HasTimestamps}
 */
public final class ResponseTimestamper {

  /**
   * Sets the Last-Modified header in a ResponseBuilder object based on the Last Update property of a HasTimestamps
   * object. If the Last Update property is null, no time stamp will be added and any existing time stamp will be
   * removed.
   * 
   * @param responseBuilder
   * @param hasTimestamps
   * @return
   */
  public static ResponseBuilder setLastModified(ResponseBuilder responseBuilder, HasTimestamps hasTimestamps) {
    return responseBuilder.lastModified(hasTimestamps.getLastUpdate());
  }

  public static ResponseBuilder ok(HasTimestamps hasTimestamps) {
    return ResponseTimestamper.setLastModified(Response.ok(hasTimestamps), hasTimestamps);
  }

  public static void evaluate(Request request, HasTimestamps hasTimestamps) {
    ResponseBuilder rb = request.evaluatePreconditions(hasTimestamps.getLastUpdate());
    if(rb != null) {
      throw new UnsatisfiedPreconditionException(rb);
    }
  }
}
