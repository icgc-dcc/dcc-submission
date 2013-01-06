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

package org.icgc.dcc.resources;

import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.yammer.dropwizard.testing.ResourceTest;
import org.icgc.dcc.core.Gene;
import org.icgc.dcc.dao.GeneDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class GeneResourceTest extends ResourceTest {

	private final Gene gene1 = new Gene("L", 1L);
	private final List<Gene> genes = newArrayList();

	@Mock
	private GeneDao geneDao;

	@Override
	protected final void setUpResources() throws Exception {
		when(geneDao.getOne(anyString())).thenReturn(gene1);
		when(geneDao.getAll()).thenReturn(genes);
		addResource(new GeneResource(geneDao));
	}

	@Test
	public final void testGetAll() throws Exception {
		assertThat(client().resource("/genes").get(List.class)).isEqualTo(genes);

		verify(geneDao).getAll();
	}

	@Test
	public final void testGetOne() throws Exception {
		assertThat(client().resource("/genes/1").get(Gene.class)).isEqualTo(gene1);

		verify(geneDao).getOne("1");
	}

}
