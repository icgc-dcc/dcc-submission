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

import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.util.List;

@Data
public class ResponseFacet {
  private final String name;
  private final String type;
  private final long missing;
  private final long total;
  private final long other;
  private final ImmutableList<Term> terms;

  public ResponseFacet(Facet f) {
    TermsFacet facet = (TermsFacet) f;
    this.name = facet.getName();
    this.type = facet.getType();
    this.missing = facet.getMissingCount();
    this.total = facet.getTotalCount();
    this.other = facet.getOtherCount();
    this.terms = buildTerms(facet.getEntries());
  }

  private ImmutableList<Term> buildTerms(List<? extends TermsFacet.Entry> entries) {
    ImmutableList.Builder<Term> l = new ImmutableList.Builder<Term>();
    for (TermsFacet.Entry entry : entries) {
      String name = entry.getTerm();
      int value = entry.getCount();
      Term term = new Term(name, value);
      l.add(term);
    }
    return l.build();
  }

  @Data
  private class Term {
    private final String term;
    private final int count;
  }
}
