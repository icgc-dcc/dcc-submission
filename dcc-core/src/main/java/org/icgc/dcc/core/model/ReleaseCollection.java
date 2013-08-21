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

import static org.icgc.dcc.core.model.FieldNames.DONOR_ID;
import static org.icgc.dcc.core.model.FieldNames.GENE_ID;
import static org.icgc.dcc.core.model.FieldNames.MUTATION_ID;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_ID;
import static org.icgc.dcc.core.model.FieldNames.PROJECT_ID;
import static org.icgc.dcc.core.model.FieldNames.RELEASE_ID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a collection in the the MongoDB data model.
 */
@RequiredArgsConstructor
@Getter
public enum ReleaseCollection {

  RELEASE_COLLECTION("Release", RELEASE_ID),
  PROJECT_COLLECTION("Project", PROJECT_ID),
  DONOR_COLLECTION("Donor", DONOR_ID),
  GENE_COLLECTION("Gene", GENE_ID),
  OBSERVATION_COLLECTION("Observation", OBSERVATION_ID),
  MUTATION_COLLECTION("Mutation", MUTATION_ID);

  /**
   * The name of the collection.
   */
  final private String name;

  /**
   * The primary key of the collection.
   */
  final private String key;

  @Override
  public String toString() {
    return name;
  }

}
