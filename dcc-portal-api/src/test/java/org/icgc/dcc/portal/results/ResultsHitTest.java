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

package org.icgc.dcc.portal.results;

import org.elasticsearch.search.SearchHit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResultsHitTest {

  @Mock
  SearchHit searchHit;

  @Test
  public final void test_Score_WhenHitScoreNaN() {
    when(searchHit.getScore()).thenReturn(Float.NaN);

    ResultsHit resultsHit = new ResultsHit(searchHit);

    // Score should default to 0
    assertThat(resultsHit.getScore()).isEqualTo(0.0f);
  }

  @Test
  public final void test_Score_WhenHitHasScore() {
    when(searchHit.getScore()).thenReturn(0.75f);

    ResultsHit resultsHit = new ResultsHit(searchHit);

    assertThat(resultsHit.getScore()).isEqualTo(0.75f);
  }

  @Test
  public final void test_Score_WhenHitFieldsAreNull() {
    when(searchHit.getFields()).thenReturn(null);

    ResultsHit resultsHit = new ResultsHit(searchHit);

    assertThat(resultsHit.getFields()).isNull();
  }
}
