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

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.Entity.GENE;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.experimental.FieldDefaults;

/**
 * ElasticSearch index types.
 */
@RequiredArgsConstructor(access = PRIVATE)
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Getter
@ToString
public enum IndexType {

  /**
   * Project type(s).
   */
  PROJECT_TYPE(Entity.PROJECT, "project", Classifier.BASIC),

  /**
   * Donor type(s).
   */
  DONOR_TYPE(Entity.DONOR, "donor", Classifier.BASIC),
  DONOR_CENTRIC_TYPE(Entity.DONOR, "donor-centric", Classifier.BASIC),

  /**
   * Gene type(s).
   */
  GENE_TYPE(GENE, "gene", Classifier.BASIC),
  GENE_CENTRIC_TYPE(GENE, "gene-centric", Classifier.CENTRIC),

  /**
   * Observation type(s).
   */
  OBSERVATION_CENTRIC_TYPE(Entity.OBSERVATION, "observation-centric", Classifier.CENTRIC),

  /**
   * Mutation type(s).
   */
  MUTATION_CENTRIC_TYPE(Entity.OBSERVATION, "mutation-centric", Classifier.CENTRIC);

  /**
   * The corresponding entity of the index type.
   */
  Entity entity;

  /**
   * The name of the index type.
   */
  String name;

  /**
   * The classifier of the index type.
   */
  Classifier classifier;

  public static IndexType byName(String name) {
    checkArgument(name != null, "Target name for class '%s' cannot be null", IndexType.class.getName());

    for (val value : values()) {
      if (name.equals(value.name)) {
        return value;
      }
    }

    throw new IllegalArgumentException("No " + IndexType.class.getName() + " value with name '" + name + "' found");
  }

  public enum Classifier {
    BASIC,
    CENTRIC;
  }

}
