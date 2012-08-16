/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.dictionary.graph;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.visitor.BaseDictionaryVisitor;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 * Builds a {@code DirectedGraph} with {@code FileSchema} name as nodes and {@code Relation} as edges. The {@code Graph}
 * is directed because the {@code Relation} are modelled on one side of the relation only.
 */
public class DictionaryGraphVisitor extends BaseDictionaryVisitor {

  private final DirectedGraph<String, Relation> graph = new DefaultDirectedGraph<String, Relation>(Relation.class);

  private String lastFileSchema;

  @Override
  public void visit(FileSchema fileSchema) {
    ensureVertex(fileSchema.getName());
    lastFileSchema = fileSchema.getName();
  }

  @Override
  public void visit(Relation relation) {
    ensureVertex(relation.getOther());
    graph.addEdge(lastFileSchema, relation.getOther(), relation);
  }

  public DirectedGraph<String, Relation> getGraph() {
    return graph;
  }

  private void ensureVertex(String fileSchema) {
    if(graph.containsVertex(fileSchema) == false) {
      graph.addVertex(fileSchema);
    }
  }
}
