/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.loader.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.loader.util.TypeDefs.TYPE_ORDER;

import java.util.List;

import lombok.val;

import org.icgc.dcc.submission.loader.model.TypeDef;
import org.icgc.dcc.submission.loader.util.TypeDefs;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TypeDefGraphTest {

  @Test
  public void testTopologicalOrder() throws Exception {
    val entityDefGraph = new TypeDefGraph(createTypeDefs());
    val order = entityDefGraph.topologicalOrder();

    TypeDef typeDef = null;
    int index = 0;
    while (order.hasNext()) {
      typeDef = order.next();
      assertOrder(typeDef.getType(), index++);
    }

    assertThat(index).isEqualTo(4);
  }

  private static void assertOrder(String type, int index) {
    assertThat(type).isEqualTo(TYPE_ORDER.get(index));
  }

  private static List<TypeDef> createTypeDefs() {
    val typeDefs = ImmutableList.<TypeDef> builder();
    typeDefs.add(TypeDefs.donor());
    typeDefs.add(TypeDefs.specimen());
    typeDefs.add(TypeDefs.biomarker());
    typeDefs.add(TypeDefs.sample());

    return typeDefs.build();
  }

}
