/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.tools.javac.util.List;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.icgc.dcc.core.Gene;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
@Slf4j
public class GeneDao {
    private static final String GET_ALL_LOG = "Returning all Genes";
    private static final String GET_ONE_LOG = "Returning Gene %s";
    private static final String INDEX = "blog";

    private final Client store;

    @Inject
    public GeneDao(Client es) {
        this.store = es;
    }

    public final Gene getOne(String Id) {
        GeneDao.log.info(String.format(GET_ONE_LOG, Id));
        store.prepareSearch(INDEX)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(matchAllQuery())
                .setFrom(0).setSize(1).setExplain(true)
                .execute()
                .actionGet()
                .toString();

        return new Gene();
    }

    public final List<Gene> getAll() {
        GeneDao.log.info(GET_ALL_LOG);
        store.prepareSearch(INDEX)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(matchAllQuery())
                .setFrom(0).setSize(2).setExplain(true)
                .execute()
                .actionGet()
                .toString();

        return List.of(new Gene());
    }
}
