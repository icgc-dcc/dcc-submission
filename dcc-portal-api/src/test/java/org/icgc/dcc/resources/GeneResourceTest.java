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

package org.icgc.dcc.resources;

import static org.fest.assertions.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.api.client.WebResource;
import com.yammer.dropwizard.testing.ResourceTest;

import org.icgc.dcc.repositories.impl.SearchRepositoryImpl;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class GeneResourceTest extends ResourceTest {

  @Mock
  private SearchRepositoryImpl store;

  @Override
  protected final void setUpResources() throws Exception {
    // when(store.search()).thenReturn();
    addResource(new GeneResource(store));
  }

  @Test
  public final void testGetAll() throws Exception {
    WebResource wr = client().resource("/genes");
    assertThat(wr.get(Response.class).getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    // verify(store).getAll();
  }

  // @Test
  public final void testGetOne() throws Exception {
    // assertThat(client().resource("/genes/1").get(Gene.class)).isEqualTo(gene1);

    // verify(store).getOne("1");
  }

}
