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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.CNSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.METH_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.STSM_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.META_SUBTYPE;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileType;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

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
  public enum FeatureType implements DataType, DeletionType {

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

    @Override
    public boolean isAllDeletionType() {
      return false;
    }

    @Override
    public boolean isErroneousDeletionType() {
      return false;
    }

    /**
     * Returns the file types corresponding to the feature type.
     * <p>
     * TODO: move to {@link FileTypes} rather
     */
    public Set<FileType> getCorrespondingFileTypes() {
      val dataType = this;
      return newLinkedHashSet(Iterables.filter(
          newArrayList(FileType.values()),
          new Predicate<FileType>() {

            @Override
            public boolean apply(FileType fileType) {
              return fileType.getDataType() == dataType;
            }
          }));
    }

    /**
     * Returns the file type whose presence indicates that the type is considered as "present" and therefore to be
     * processed.
     * <p>
     * TODO: move to {@link FileTypes} rather
     */
    public FileType getFileTypeFlagship() {
      FileType fileTypeFlagship = null;
      for (val fileType : getCorrespondingFileTypes()) {
        checkState(fileType.getDataType() == this, "'%s' != '%s'", fileType, this); // By design
        if (fileType.getSubType() == META_SUBTYPE) {
          fileTypeFlagship = fileType;
          break;
        }
      }
      return checkNotNull(fileTypeFlagship,
          "There should be at least one file type that acts as type presence flagship for the feature type '%s'", this);
    }

    /**
     * TODO
     */
    public static boolean hasMatch(String typeName) {
      for (val featureType : values()) {
        if (featureType.name().equals(format(typeName))) {
          return true;
        }
      }
      return false;
    }

    /**
     * Returns an enum matching the type like "ssm", "meth", ...
     */
    public static FeatureType from(String typeName) {
      return valueOf(format(typeName));
    }

    /**
     * Returns the complement of the feature types provided, i.e. the feature types not provided in the list.
     */
    public static Set<FeatureType> complement(Set<FeatureType> featureTypes) {
      List<FeatureType> complement = newArrayList(values());
      complement.removeAll(featureTypes);
      return newLinkedHashSet(complement);
    }

    /**
     * TODO
     */
    private static String format(String typeName) {
      return String.format("%s_%s", typeName.toUpperCase(), TYPE_SUFFIX);
    }
  }

  /**
   * Feature types for which there is a control sample ID.
   */
  private static final Set<FeatureType> CONTROL_SAMPLE_FEATURE_TYPES = of(
      SSM_TYPE, CNSM_TYPE, STSM_TYPE, METH_TYPE);

  /**
   * Features types for which mutations will be aggregated.
   */
  static final Set<FeatureType> AGGREGATED_FEATURE_TYPES = of(SSM_TYPE);

  public static boolean isAggregatedType(FeatureType type) {
    return AGGREGATED_FEATURE_TYPES.contains(type);
  }

  public static boolean hasControlSampleId(FeatureType type) {
    return CONTROL_SAMPLE_FEATURE_TYPES.contains(type);
  }

}
