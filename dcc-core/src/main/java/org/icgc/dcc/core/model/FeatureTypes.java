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

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.CNGV_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.CNSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.EXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.JCN_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.METH_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.MIRNA_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.PEXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SGV_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.STGV_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.STSM_TYPE;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;

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

  /** From the ICGC Submission Manual */
  public static final List<String> FEATURE_TYPES = of( // TODO: change to use enum directly
      SSM_TYPE.getTypeName(), SGV_TYPE.getTypeName(), CNSM_TYPE.getTypeName(),
      CNGV_TYPE.getTypeName(), STSM_TYPE.getTypeName(), STGV_TYPE.getTypeName(),
      MIRNA_TYPE.getTypeName(), METH_TYPE.getTypeName(), EXP_TYPE.getTypeName(),
      PEXP_TYPE.getTypeName(), JCN_TYPE.getTypeName());

  /** Subset of {@link #FEATURE_TYPES} that relates to somatic mutations */
  private static final List<String> SOMATIC_FEATURE_TYPES = of( // TODO: change to use enum directly
      SSM_TYPE.getTypeName(), CNSM_TYPE.getTypeName(), STSM_TYPE.getTypeName());

  private static final Set<String> SOMATIC_FEATURE_TYPES_SET = copyOf(SOMATIC_FEATURE_TYPES);

  /** Subset of {@link #FEATURE_TYPES} that relates to survey-based features */
  private static final List<String> SURVEY_FEATURE_TYPES = of( // TODO: change to use enum directly
      EXP_TYPE.getTypeName(), MIRNA_TYPE.getTypeName(), JCN_TYPE.getTypeName(),
      METH_TYPE.getTypeName(), PEXP_TYPE.getTypeName());

  /** Feature types whose sample ID isn't called analyzed_sample_id in older dictionaries */
  private static final List<String> DIFFERENT_SAMPLE_ID_FEATURE_TYPES = of( // TODO: change to use enum directly
      EXP_TYPE.getTypeName(), MIRNA_TYPE.getTypeName(), JCN_TYPE.getTypeName(),
      PEXP_TYPE.getTypeName());

  /**
   * Feature types for which there is a control sample ID.
   */
  private static final List<String> CONTROL_SAMPLE_FEATURE_TYPES = of( // TODO: change to use enum directly
      SSM_TYPE.getTypeName(), CNSM_TYPE.getTypeName(), STSM_TYPE.getTypeName(), METH_TYPE.getTypeName());

  /**
   * Features types for which mutations will be aggregated.
   */
  private static final List<String> AGGREGATED_FEATURE_TYPES = of(SSM_TYPE.getTypeName()); // TODO: change to use enum directly

  /**
   * Features types that are small enough to be stored in mongodb (as exposed to exported to hdfs only).
   */
  public static final List<String> MONGO_FRIENDLY_FEATURE_TYPES = newArrayList(AGGREGATED_FEATURE_TYPES);

  public static List<String> getTypes() {
    return FEATURE_TYPES;
  }

  public static List<String> getSomaticTypes() {
    return SOMATIC_FEATURE_TYPES;
  }

  public static List<String> getArrayTypes() {
    return SURVEY_FEATURE_TYPES;
  }

  public static boolean isType(String type) {
    return FEATURE_TYPES.contains(type);
  }

  public static boolean isSomaticType(String type) {
    return SOMATIC_FEATURE_TYPES_SET.contains(type);
  }

  public static boolean isSurveyType(String type) {
    return SURVEY_FEATURE_TYPES.contains(type);
  }

  public static boolean isAggregatedType(String type) {
    return AGGREGATED_FEATURE_TYPES.contains(type);
  }

  public static boolean hasDifferentSampleId(String type) {
    return DIFFERENT_SAMPLE_ID_FEATURE_TYPES.contains(type);
  }

  public static boolean hasControlSampleId(String type) {
    return CONTROL_SAMPLE_FEATURE_TYPES.contains(type);
  }

}
