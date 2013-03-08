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

package org.icgc.dcc.portal.resources;

import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.facet.Facets;
import org.icgc.dcc.portal.repositories.IGeneRepository;
import org.icgc.dcc.portal.search.GeneSearchQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
// @SuppressWarnings("unchecked")
public class GeneResourceTest extends ResourceTest {

  private final static String RESOURCE = "/genes";

  @Mock
  private IGeneRepository store;

  @Mock
  private SearchResponse searchResponse;

  @Mock
  private SearchHit searchHit;

  @Mock
  private SearchHits searchHits;

  @Mock
  private Facets searchFacets;

  @Mock
  private GetResponse getResponse;

  @Override
  protected final void setUpResources() throws Exception {
    addResource(new GeneResource(store));
  }


  @Test
  public final void test_getMany() throws Exception {
    when(store.getAll(any(GeneSearchQuery.class))).thenReturn(searchResponse);
    when(searchResponse.getHits()).thenReturn(searchHits);
    when(searchHits.getHits()).thenReturn(new SearchHit[] {searchHit});
    when(searchResponse.getFacets()).thenReturn(searchFacets);

    ClientResponse response = client().resource(RESOURCE).get(ClientResponse.class);
    verify(store).getAll(any(GeneSearchQuery.class));
    assertThat(response.getStatus()).isEqualTo(ClientResponse.Status.OK.getStatusCode());
  }

  @Test
  public final void test_getOne() throws Exception {
    when(store.getOne(any(String.class))).thenReturn(getResponse);
    when(getResponse.getId()).thenReturn("ENSG00000187939");
    when(getResponse.getType()).thenReturn("genes");
    when(getResponse.getFields()).thenReturn(new HashMap<String, GetField>());

    ClientResponse response = client().resource(RESOURCE).path("ENSG00000187939").get(ClientResponse.class);
    verify(store).getOne(any(String.class));
    assertThat(response.getStatus()).isEqualTo(ClientResponse.Status.OK.getStatusCode());
  }
}
