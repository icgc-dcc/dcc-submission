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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Throwables.propagate;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.Collection;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.collect.ImmutableList;

@NoArgsConstructor(access = PRIVATE)
public final class KVDynamicDictionaryHelper {

  public static Iterable<KVFileType> getTopologicallyOrderedFileTypes(Dictionary dictionary) {
    val fileTypeRelations = dictionary.getFiles().stream()
        .map(KVDynamicDictionaryHelper::createFileTypeRelation)
        .collect(toImmutableList());

    val graph = createGraph(fileTypeRelations);
    return ImmutableList.copyOf(graph.iterator());

    // Currently donor supplementary types are in the end of the list which is not efficient to keep references
    // to donor primary keys till the very end of processing of the donor and it's children could be verified first
    // TODO: rework order

    // val root = graph.iterator().next();
    // val rootEdges = graph.outgoingEdgesOf(root);
    //
    // val terminalEdges = rootEdges.stream()
    // .map(edge -> edge.getTarget())
    // // Add first those who doesn't have children
    // .filter(child -> graph.outgoingEdgesOf(child).isEmpty())
    // .collect(toImmutableList());
    //
    // val orderedFileTypes = Sets.newLinkedHashSet(root);
    // orderedFileTypes.addAll(terminalEdges);
    //
    // // Add the other elements
    // stream(graph.iterator())
    // .forEach(fileType -> orderedFileTypes.add(fileType));
    //
    // return orderedFileTypes;
  }

  private static DirectedAcyclicGraph<KVFileType, Edge> createGraph(Iterable<KVFileTypeRelation> fileTypeRelations) {
    val graph = new DirectedAcyclicGraph<KVFileType, Edge>(Edge.class);

    fileTypeRelations.forEach(fileTypeRelation -> graph.addVertex(fileTypeRelation.getFileType()));

    fileTypeRelations.forEach(fileTypeRelation -> {
      KVFileType child = fileTypeRelation.fileType;
      fileTypeRelation.parents.stream()
          .forEach(parent -> {
            try {
              graph.addDagEdge(parent, child);
            } catch (Exception e) {
              propagate(e);
            }
          });
    });

    return graph;
  }

  private static KVFileTypeRelation createFileTypeRelation(FileSchema schema) {
    val fileType = KVFileType.from(schema.getFileType());
    val parents = schema.getRelations().stream()
        .map(relation -> relation.getOtherFileType())
        .map(KVFileType::from)
        .collect(toImmutableList());

    return new KVFileTypeRelation(fileType, parents);
  }

  public static class Edge extends DefaultEdge {

    @Override
    public KVFileType getSource() {
      return (KVFileType) super.getSource();
    }

    @Override
    public KVFileType getTarget() {
      return (KVFileType) super.getTarget();
    }

  }

  @Value
  private static class KVFileTypeRelation {

    KVFileType fileType;
    Collection<KVFileType> parents;

  }

}
