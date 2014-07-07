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
package org.icgc.dcc.core.model;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.core.model.FieldNames.MONGO_INTERNAL_ID;
import static org.icgc.dcc.core.model.FieldNames.PATHWAY_REACTOME_ID;
import static org.icgc.dcc.core.model.FieldNames.RELEASE_ID;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.FieldNames.IdentifierFieldNames;
import org.icgc.dcc.core.model.FieldNames.LoaderFieldNames;

/**
 * Represents a collection in the the MongoDB data model.
 */
@RequiredArgsConstructor
@Getter
public enum ReleaseCollection {

  RELEASE_COLLECTION("Release", newArrayList(RELEASE_ID)),
  PROJECT_COLLECTION("Project", newArrayList(LoaderFieldNames.PROJECT_ID)),
  DONOR_COLLECTION("Donor", newArrayList(IdentifierFieldNames.SURROGATE_DONOR_ID)),
  GENE_COLLECTION("Gene", newArrayList(LoaderFieldNames.GENE_ID)),
  OBSERVATION_COLLECTION("Observation", newArrayList(
      IdentifierFieldNames.SURROGATE_DONOR_ID,
      IdentifierFieldNames.SURROGATE_MUTATION_ID)),
  MUTATION_COLLECTION("Mutation", newArrayList(IdentifierFieldNames.SURROGATE_MUTATION_ID)),
  PATHWAY_COLLECTION("Pathway", newArrayList(PATHWAY_REACTOME_ID));

  /**
   * The name of the collection.
   */
  private final String name;

  /**
   * The primary key of the collection.
   */
  private final List<String> primaryKey;

  /**
   * Returns the field that uniquely describes documents. If there is one field in the primary key, it returns that
   * field, otherwise it returns mongodb's internal "_id" field.
   */
  public String getSurrogateKey() {
    int size = primaryKey.size();
    checkState(size >= 1,
        "There should always be at least one field in the primary key, instead: '%s'", primaryKey);
    return size == 1 ?
        getFirstKey() :
        MONGO_INTERNAL_ID;
  }

  private String getFirstKey() {
    return primaryKey.get(0);
  }

  @Override
  public String toString() {
    return name;
  }

}
