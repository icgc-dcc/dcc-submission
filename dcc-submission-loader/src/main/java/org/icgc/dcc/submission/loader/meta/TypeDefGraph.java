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

import static com.google.common.collect.Maps.uniqueIndex;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.icgc.dcc.submission.loader.model.TypeDef;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TypeDefGraph {

  private final Map<String, TypeDef> index;
  private final DirectedAcyclicGraph<TypeDef, DefaultEdge> graph;

  public TypeDefGraph(@NonNull Collection<TypeDef> typeDefs) {
    this.index = uniqueIndex(typeDefs, (def) -> def.getType());
    this.graph = resolveGraph(typeDefs);
  }

  public Iterator<TypeDef> topologicalOrder() {
    return graph.iterator();
  }

  @SneakyThrows
  private DirectedAcyclicGraph<TypeDef, DefaultEdge> resolveGraph(Collection<TypeDef> typeDefs) {
    log.debug("Resolving types graph...");

    val graph = new DirectedAcyclicGraph<TypeDef, DefaultEdge>(DefaultEdge.class);

    // Add vertices
    typeDefs.stream().forEach(td -> graph.addVertex(td));

    // Add edges
    for (val typeDef : typeDefs) {
      for (val parentType : typeDef.getParent()) {
        val from = findParent(parentType);
        val to = typeDef;
        log.debug("{} -> {}", to, from);

        graph.addDagEdge(from, to);
      }
    }

    return graph;
  }

  private TypeDef findParent(String parentType) {
    return index.get(parentType);
  }

}
