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
import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.icgc.dcc.portal.search.SearchQuery;

import java.util.List;
import java.util.Map;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

@Data
public final class GetManyResponse {

  private final ImmutableList<ResponseHit> hits;
  private final ImmutableList<ResponseFacet> facets;
  private final Pagination pagination;

  public GetManyResponse(final SearchResponse response, final SearchQuery searchQuery) {
    this.hits = buildResponseHits(response.getHits().getHits());
    this.facets = buildFacets(response.getFacets());
    this.pagination = new Pagination(response.getHits(), searchQuery);
  }

  private ImmutableList<ResponseFacet> buildFacets(Facets facets) {
    ImmutableList.Builder<ResponseFacet> lb = new ImmutableList.Builder<ResponseFacet>();
    for (Facet facet : facets) {
      lb.add(new ResponseFacet((TermsFacet) facet));
    }
    return lb.build();
  }

  private ImmutableList<ResponseHit> buildResponseHits(SearchHit[] hits) {
    ImmutableList.Builder<ResponseHit> lb = new ImmutableList.Builder<ResponseHit>();
    for (SearchHit hit : hits) {
      lb.add(new ResponseHit(hit));
    }
    return lb.build();
  }

  @Data
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private final class Pagination {
    private final int count;
    private final long total;
    private final int size;
    private final int from;
    private final double page;
    private final double pages;
    private final String sort;
    private final String order;

    public Pagination(final SearchHits hits, SearchQuery searchQuery) {
      this.count = hits.getHits().length;
      this.total = hits.getTotalHits();
      this.size = searchQuery.getSize();
      this.from = searchQuery.getFrom() + 1;
      this.sort = searchQuery.getSort();
      this.order = searchQuery.getOrder().toString().toLowerCase();
      this.page = floor(from / size) + 1;
      this.pages = ceil(total / size);
    }
  }

  @Data
  private final class ResponseHit {
    private final String id;
    private final String type;
    private final float score;
    private final ImmutableList<ResponseHitField> fields;


    public ResponseHit(SearchHit hit) {
      this.id = hit.getId();
      this.type = hit.getType();
      this.score = Float.isNaN(hit.getScore()) ? 0.0f : hit.getScore();
      this.fields = getHitFields(hit.getFields());
    }

    private ImmutableList<ResponseHitField> getHitFields(Map<String, SearchHitField> fields) {
      ImmutableList.Builder<ResponseHitField> l = new ImmutableList.Builder<ResponseHitField>();
      for (SearchHitField field : fields.values()) {
        String name = field.getName();
        Object value = field.getValues().toArray()[0];
        ResponseHitField rhf = new ResponseHitField(name, value);
        l.add(rhf);
      }
      return l.build();
    }

    @Data
    private class ResponseHitField {
      private final String name;
      private final Object value;
    }
  }

  @Data
  private class ResponseFacet {
    private final String name;
    private final String type;
    private final long missing;
    private final long total;
    private final long other;
    private final ImmutableList<Term> terms;


    public ResponseFacet(TermsFacet facet) {
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
      private final String name;
      private final int value;
    }
  }
}
