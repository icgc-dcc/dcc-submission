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
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.CNSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.EXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.JCN_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.METH_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.MIRNA_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.PEXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.STSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.from;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.google.common.collect.ImmutableSet;

/**
 * Utilities for working with ICGC feature types.
 * <p>
 * For clinical file types, see {@link FileTypes} instead.
 */
@NoArgsConstructor(access = PRIVATE)
public final class FeatureTypes {

  /**
   * Represents a type of observation data, see {@link ClinicalType} for the clinical counterpart.
   */
  public enum FeatureType implements SubmissionDataType {

    /** From the ICGC Submission Manual */
    SSM_TYPE("ssm", "_ssm_count"),
    SGV_TYPE("sgv", "_sgv_exists"),
    CNSM_TYPE("cnsm", "_cnsm_exists"),
    CNGV_TYPE("cngv", "_cngv_exists"),
    STSM_TYPE("stsm", "_stsm_exists"),
    STGV_TYPE("stgv", "_stgv_exists"),
    METH_TYPE("meth", "_meth_exists"),
    MIRNA_TYPE("mirna", "_mirna_exists"),
    EXP_TYPE("exp", "_exp_exists"),
    PEXP_TYPE("pexp", "_pexp_exists"),
    JCN_TYPE("jcn", "_jcn_exists");

    private FeatureType(String typeName) {
      this(typeName, null);
    }

    private FeatureType(String typeName, String summaryFieldName) {
      this.typeName = typeName;
      this.summaryFieldName = summaryFieldName;
    }

    @Getter
    private final String typeName;

    @Getter
    private final String summaryFieldName;

    @Override
    public boolean isClinicalType() {
      return false;
    }

    @Override
    public boolean isFeatureType() {
      return true;
    }

    @Override
    public ClinicalType asClinicalType() {
      checkState(false, "Not a '%s': '%s'",
          ClinicalType.class.getSimpleName(), this);
      return null;
    }

    @Override
    public FeatureType asFeatureType() {
      return this;
    }

    public boolean isSsm() {
      return this == SSM_TYPE;
    }

    public boolean isCountSummary() {
      return isSsm();
    }

    /**
     * Returns an enum matching the type like "ssm", "meth", ...
     */
    public static FeatureType from(String typeName) {
      return valueOf(typeName.toUpperCase() + TYPE_SUFFIX);
    }

    /**
     * Returns the complement of the feature types provided, i.e. the feature types not provided in the list.
     */
    public static Set<FeatureType> complement(Set<FeatureType> featureTypes) {
      List<FeatureType> complement = newArrayList(values());
      complement.removeAll(featureTypes);
      return newLinkedHashSet(complement);
    }

  }

  /** Subset of {@link #FEATURE_TYPES} that relates to somatic mutations */
  private static final Set<FeatureType> SOMATIC_FEATURE_TYPES_SET = ImmutableSet.<FeatureType> of(
      SSM_TYPE, CNSM_TYPE, STSM_TYPE);

  /** Subset of {@link #FEATURE_TYPES} that relates to survey-based features */
  private static final Set<FeatureType> SURVEY_FEATURE_TYPES = of(
      EXP_TYPE, MIRNA_TYPE, JCN_TYPE, METH_TYPE, PEXP_TYPE);

  /** Feature types whose sample ID isn't called analyzed_sample_id in older dictionaries */
  private static final Set<FeatureType> DIFFERENT_SAMPLE_ID_FEATURE_TYPES = of(
      EXP_TYPE, MIRNA_TYPE, JCN_TYPE, PEXP_TYPE);

  /**
   * Feature types for which there is a control sample ID.
   */
  private static final Set<FeatureType> CONTROL_SAMPLE_FEATURE_TYPES = of(
      SSM_TYPE, CNSM_TYPE, STSM_TYPE, METH_TYPE);

  /**
   * Features types for which mutations will be aggregated.
   */
  private static final Set<FeatureType> AGGREGATED_FEATURE_TYPES = of(SSM_TYPE);

  /**
   * Features types that are small enough to be loaded in mongodb (as exposed to exported to hdfs only).
   */
  private static final Set<FeatureType> MONGO_LOADED_FEATURE_TYPES = copyOf(AGGREGATED_FEATURE_TYPES);

  public static boolean isSomaticType(String type) { // TODO: use enum
    return SOMATIC_FEATURE_TYPES_SET.contains(from(type));
  }

  public static boolean isSurveyType(String type) { // TODO: use enum
    return SURVEY_FEATURE_TYPES.contains(from(type));
  }

  public static boolean isAggregatedType(FeatureType type) {
    return AGGREGATED_FEATURE_TYPES.contains(type);
  }

  public static boolean hasDifferentSampleId(String type) { // TODO: use enum
    return DIFFERENT_SAMPLE_ID_FEATURE_TYPES.contains(from(type));
  }

  public static boolean hasControlSampleId(String type) { // TODO: use enum
    return CONTROL_SAMPLE_FEATURE_TYPES.contains(from(type));
  }

  public static boolean isMongoLoaded(String type) { // TODO: use enum
    return MONGO_LOADED_FEATURE_TYPES.contains(from(type));
  }

}
