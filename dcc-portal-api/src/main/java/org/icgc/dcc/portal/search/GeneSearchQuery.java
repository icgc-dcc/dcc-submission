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

package org.icgc.dcc.portal.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.elasticsearch.search.sort.SortOrder;

@EqualsAndHashCode(callSuper = false)
@Data
public class GeneSearchQuery extends SearchQuery {

  private static final String DEFAULT_SORT = "start";
  private static final SortOrder DEFAULT_ORDER = SortOrder.DESC;

  public GeneSearchQuery(final int from, final int size, final String sort, final String order) {
    super(from, size);
    this.sort = sort != null ? sort : DEFAULT_SORT;
    this.order = order != null ? SortOrder.valueOf(order.toUpperCase()) : SortOrder.DESC;
  }

  public GeneSearchQuery(String filters, String score, Integer from, int size, String sort, String order) {
    super(from, size);
    this.sort = sort != null ? sort : DEFAULT_SORT;
    this.order = order != null ? SortOrder.valueOf(order.toUpperCase()) : DEFAULT_ORDER;
    this.filters = filters == null ? null : jsonifyString(filters);
    this.score = score;
  }
}
