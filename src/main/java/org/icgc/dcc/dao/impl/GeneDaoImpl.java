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

package org.icgc.dcc.dao.impl;

import java.io.IOException;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.google.inject.Inject;

import org.icgc.dcc.core.Gene;
import org.icgc.dcc.dao.GeneDao;
import org.icgc.dcc.responses.BaseResponse;

public class GeneDaoImpl implements GeneDao {
  private static final String INDEX = "blog";

  private final SearchRequestBuilder search;

  private final MongoCollection db;

  @Inject
  public GeneDaoImpl(Client es, Jongo mongo) {
    this.search = es.prepareSearch(INDEX).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
    this.db = mongo.getCollection("genes");
  }

  @Override
  public final JsonSchema getSchema() throws JsonMappingException {
    return new ObjectMapper().generateJsonSchema(Gene.class);
  }

  @Override
  public final BaseResponse getOne(String id) throws IOException {
    SearchResponse sr = search.setQuery(QueryBuilders.matchAllQuery()).setFrom(0).setSize(1).execute().actionGet();
    // return String.valueOf(sr);
    // return new ObjectMapper().readValue(sr.getHits().getHits()[0].getSourceAsString(),
    // Gene.class);
    return new BaseResponse(sr.getHits());
  }

  @Override
  public final String getAll() {
    return search.setQuery(QueryBuilders.matchAllQuery()).setFrom(0).setSize(2).setExplain(true).execute().actionGet()
        .toString();
  }
}
