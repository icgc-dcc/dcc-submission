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
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Set;

import lombok.NoArgsConstructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Utilities for working with ICGC feature types.
 * <p>
 * For clinical file types, see {@link FileTypes} instead.
 */
@NoArgsConstructor(access = PRIVATE)
public final class FeatureTypes {

  /**
   * Feature types.
   */
  public static final String SSM_TYPE = "ssm";
  public static final String CNSM_TYPE = "cnsm";
  public static final String STSM_TYPE = "stsm";
  public static final String SGV_TYPE = "sgv";
  public static final String CNGV_TYPE = "cngv";
  public static final String STGV_TYPE = "stgv";
  public static final String PEXP_TYPE = "pexp";
  public static final String EXP_TYPE = "exp";
  public static final String METH_TYPE = "meth";
  public static final String MIRNA_TYPE = "mirna";
  public static final String JCN_TYPE = "jcn";

  /** From the ICGC Submission Manual */
  private static final List<String> FEATURE_TYPES = ImmutableList.of(
      SSM_TYPE, SGV_TYPE, CNSM_TYPE, CNGV_TYPE, STSM_TYPE, STGV_TYPE,
      MIRNA_TYPE, METH_TYPE, EXP_TYPE, PEXP_TYPE, JCN_TYPE);

  /** Subset of {@link #FEATURE_TYPES} that relates to somatic mutations */
  private static final List<String> SOMATIC_FEATURE_TYPES = ImmutableList.of(
      SSM_TYPE, CNSM_TYPE, STSM_TYPE);

  private static final Set<String> SOMATIC_FEATURE_TYPES_SET = ImmutableSet.copyOf(SOMATIC_FEATURE_TYPES);

  /** Subset of {@link #FEATURE_TYPES} that relates to survey-based features */
  private static final List<String> SURVEY_FEATURE_TYPES = ImmutableList.of(
      EXP_TYPE, MIRNA_TYPE, JCN_TYPE, METH_TYPE, PEXP_TYPE);

  /** Feature types whose sample ID isn't called analyzed_sample_id in older dictionaries */
  private static final List<String> DIFFERENT_SAMPLE_ID_FEATURE_TYPES = ImmutableList.of(
      EXP_TYPE, MIRNA_TYPE, JCN_TYPE, PEXP_TYPE);

  /**
   * Features types that are small enough to be stored in mongodb (as exposed to exported to hdfs only).
   */
  public static final List<String> MONGO_FRIENDLY_FEATURE_TYPES = of(SSM_TYPE, SGV_TYPE, CNSM_TYPE);

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

  public static boolean hasDifferentSampleId(String type) {
    return DIFFERENT_SAMPLE_ID_FEATURE_TYPES.contains(type);
  }

}
