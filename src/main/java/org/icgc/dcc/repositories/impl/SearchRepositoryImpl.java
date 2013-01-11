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

package org.icgc.dcc.repositories.impl;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;

import com.google.inject.Inject;

import org.icgc.dcc.core.Types;
import org.icgc.dcc.repositories.SearchRepository;
import org.icgc.dcc.search.SearchQuery;

@Slf4j
public class SearchRepositoryImpl implements SearchRepository {

  private final static String index = "blog"; // This should probably be set in a config

  private final Types type;

  private final Client client;

  @Inject
  public SearchRepositoryImpl(Client client) {
    this.client = client;
    this.type = Types.ALL;
  }

  public SearchRepositoryImpl(Client client, Types type) {
    this.client = client;
    this.type = type;
  }

  public final SearchRepositoryImpl withType(Types type) {
    return this.type.equals(type) ? this : new SearchRepositoryImpl(this.client, type);
  }

  // Returns one hit
  @Override
  public final GetResponse getOne(final String id) {
    return client.prepareGet(index, type.name(), id).execute().actionGet();
  }

  // Returns many hits by index
  @Override
  public final SearchHits getAll(final SearchQuery searchQuery) {
    return client.prepareSearch(index).setTypes(type.toString()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(QueryBuilders.matchAllQuery()).setFrom(searchQuery.getFrom()).setSize(searchQuery.getSize())
        .execute().actionGet().getHits();
  }

  // Text search
  @Override
  public final SearchHits search(final String text, final int from, final int size) {
    return client.prepareSearch(index).setTypes(type.toString()).setQuery(QueryBuilders.queryString(text))
        .setFrom(from).setSize(size).execute().actionGet().getHits();
  }
}
