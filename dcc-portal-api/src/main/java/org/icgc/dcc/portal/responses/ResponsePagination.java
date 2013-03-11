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

package org.icgc.dcc.portal.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.request.RequestSearchQuery;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponsePagination {
  private final int count;
  private final long total;
  private final int size;
  private final int from;
  private final int page;
  private final int pages;
  private final String sort;
  private final String order;

  public ResponsePagination(final SearchHits hits, final RequestSearchQuery requestSearchQuery) {
    this.count = hits.getHits().length;
    this.total = hits.getTotalHits();
    this.size = requestSearchQuery.getSize();
    this.from = requestSearchQuery.getFrom() + 1;
    this.sort = requestSearchQuery.getSort();
    this.order = requestSearchQuery.getOrder();
    this.page = this.size == 0 ? 1 : (int) (floor(from / size) + 1);
    this.pages = this.size == 0 ? 1 : (int) ceil(total / size);
  }
}
