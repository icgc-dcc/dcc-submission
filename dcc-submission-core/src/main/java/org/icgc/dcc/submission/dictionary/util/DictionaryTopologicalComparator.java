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
package org.icgc.dcc.submission.dictionary.util;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.Comparator;
import java.util.List;

import lombok.val;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;

public class DictionaryTopologicalComparator implements Comparator<FileSchema> {

  /**
   * Compares according to {@link Relation}.
   * @return -1 if {@code this} is a parent of the {@code other}<br>
   * 0 if they are equal<br>
   * 1 if {@code this} is a child of the {@code other}
   */
  @Override
  public int compare(FileSchema left, FileSchema right) {
    // Relations point to parents
    val leftRelations = left.getRelations();
    val leftRelationFileTypes = getRelationsFileType(leftRelations);
    val rightFileType = FileType.from(right.getName());
    if (leftRelationFileTypes.contains(rightFileType)) {
      return 1;
    }

    val leftFileType = FileType.from(left.getName());
    val rightRelations = right.getRelations();
    val rightRelationFileTypes = getRelationsFileType(rightRelations);
    if (rightRelationFileTypes.contains(leftFileType)) {
      return -1;
    }

    // Schemas are already equal at this point, but we will try to compare them by proximity to the top in the
    // hierarchy. E.g. meth_array_m and meth_array_probe are equal, but it better to put meth_array_m first.
    val leftRelationsSize = Integer.valueOf(leftRelations.size());
    val rightRelationsSize = Integer.valueOf(rightRelations.size());

    // This is the right comparison order. The 'smaller' FileSchema has more relations
    return rightRelationsSize.compareTo(leftRelationsSize);
  }

  private static List<FileType> getRelationsFileType(List<Relation> relations) {
    return relations.stream()
        .map(relation -> relation.getOtherFileType())
        .collect(toImmutableList());
  }

}
