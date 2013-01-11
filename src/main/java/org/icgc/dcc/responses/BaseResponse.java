/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.responses;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Data
@NoArgsConstructor
public abstract class BaseResponse {

  private static final String SELF = "_self";

  private static final String URI_FORMAT = "%s://%s:%d%s?%s";

  private final List<LinkedEntity> links = Lists.newArrayList();

  public BaseResponse(HttpServletRequest httpServletRequest) {
    this.addSelfLink(httpServletRequest);
  }

  private void addSelfLink(HttpServletRequest httpServletRequest) {
    this.addLink(SELF, httpServletRequest);
  }

  public final void addLink(final String name, final HttpServletRequest hsr) {
    String uri =
        String.format(URI_FORMAT, hsr.getScheme(), hsr.getServerName(), hsr.getLocalPort(), hsr.getRequestURI(),
            hsr.getQueryString());
    this.links.add(new LinkedEntity(name, hsr.getMethod(), uri));
  }

  public final ImmutableList<LinkedEntity> getLinks() {
    return ImmutableList.copyOf(this.links);
  }

  @Data
  private static final class LinkedEntity {
    private final String name;

    private final String method;

    private final String uri;
  }
}
