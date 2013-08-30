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
import lombok.Getter;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileSubType;

/**
 * Represents a (the only one for now) type of clinical data, see {@link FeatureType} for the observation counterpart.
 * <p>
 * The "donor" name is reused here (which makes things a bit confusing...).
 */
public enum ClinicalType implements SubmissionDataType {

  CLINICAL_CORE_TYPE(SubmissionFileSubType.DONOR_SUBTYPE.getFullName()),
  CLINICAL_OPTIONAL_TYPE(CLINICAL_OPTIONAL_TYPE_NAME);

  private ClinicalType(String typeName) {
    this.typeName = typeName;
  }

  @Getter
  private final String typeName;

  @Override
  public boolean isClinicalType() {
    return true;
  }

  @Override
  public boolean isFeatureType() {
    return false;
  }

  @Override
  public ClinicalType asClinicalType() {
    return this;
  }

  @Override
  public FeatureType asFeatureType() {
    checkState(false, "Not a '%s': '%s'",
        FeatureType.class.getSimpleName(), this);
    return null;
  }

  /**
   * Returns an enum matching the type name provided.
   */
  public static SubmissionDataType from(String typeName) {
    checkState(CLINICAL_CORE_TYPE.getTypeName().equals(typeName),
        "Only '%s' is allowed for now, '{}' provided instead",
        CLINICAL_CORE_TYPE.getTypeName(), typeName);
    return CLINICAL_CORE_TYPE;
  }

}
