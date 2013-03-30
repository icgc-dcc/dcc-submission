/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;

import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Relation;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;

/**
 * Solves paths between {@code FileSchema} nodes in a {@code Dictionary}.
 */
public class FileSchemaPathSolver {

  private final DirectedGraph<String, Relation> dictionaryGraph;

  public FileSchemaPathSolver(Dictionary dictionary) {
    checkArgument(dictionary != null);
    DictionaryGraphVisitor visitor = new DictionaryGraphVisitor();
    dictionary.accept(visitor);
    this.dictionaryGraph = visitor.getGraph();
  }

  /**
   * Returns true if a {@code Relation} path exists from {@code start} to {@code end}, false otherwise.
   */
  public boolean hasPath(String start, String end) {
    return solvePath(start, end).isEmpty() == false;
  }

  /**
   * Returns the list of {@code Relation} between {@code start} and {@code end} nodes. This method returns an empty list
   * of no such path exists.
   * 
   * @param start the start node of the path to solve.
   * @param end the end node of the path to solve
   * @return the list of {@code Relation} instances between {@code start} and {@code end}.
   */
  public List<Relation> solvePath(String start, String end) {
    checkArgument(start != null);
    checkArgument(end != null);
    DijkstraShortestPath<String, Relation> solver =
        new DijkstraShortestPath<String, Relation>(dictionaryGraph, start, end);
    List<Relation> relations = solver.getPathEdgeList();
    if(relations == null) {
      relations = Collections.emptyList();
    }
    return relations;
  }

}
