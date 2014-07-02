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
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.CNSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.EXP_ARRAY_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.METH_ARRAY_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.PEXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.STSM_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.META_SUBTYPE;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.PRIMARY_SUBTYPE;
import static org.icgc.dcc.core.util.Joiners.UNDERSCORE;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileSubType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Proposition;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * Utilities for working with ICGC feature types.
 * <p>
 * For clinical file types, see {@link FileTypes} instead.
 * <p>
 * Only expose with*() and {@link Proposition} methods, no {@link Predicate}s.
 */
@NoArgsConstructor(access = PRIVATE)
public final class FeatureTypes {

  /**
   * Represents a type of observation data, see {@link ClinicalType} for the clinical counterpart.
   */
  public enum FeatureType implements DataType {

    /** From the ICGC Submission Manual */
    SSM_TYPE("ssm", "_ssm_count"),
    SGV_TYPE("sgv", "_sgv_exists"),
    CNSM_TYPE("cnsm", "_cnsm_exists"),
    CNGV_TYPE("cngv", "_cngv_exists"),
    STSM_TYPE("stsm", "_stsm_exists"),
    STGV_TYPE("stgv", "_stgv_exists"),
    METH_ARRAY_TYPE("meth_array", "_meth_array_exists"),
    METH_SEQ_TYPE("meth_seq", "_meth_seq_exists"),
    MIRNA_SEQ_TYPE("mirna_seq", "_mirna_seq_exists"),
    EXP_ARRAY_TYPE("exp_array", "_exp_array_exists"),
    EXP_SEQ_TYPE("exp_seq", "_exp_seq_exists"),
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

    public boolean hasSequencingStrategy() {
      return TYPES_WITH_SEQUENCING_STRATEGY.apply(this);
    }

    /**
     * Returns the file types corresponding to the feature type.
     * <p>
     * TODO: move to {@link FileTypes} rather
     */
    public Set<FileType> getCorrespondingFileTypes() {
      val dataType = this;
      return newLinkedHashSet(filter(
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
    public FileType getDataTypePresenceIndicator() {
      return checkNotNull(
          getMetaFileType(),
          "There should be at least one file type that acts as type presence flagship for the feature type '%s'", this);
    }

    public FileType getMetaFileType() {
      return getFileType(META_SUBTYPE);
    }

    public FileType getPrimaryFileType() {
      return getFileType(PRIMARY_SUBTYPE);
    }

    private FileType getFileType(final FileSubType fileSubType) {
      return find( // MUST have a match (by design)
          getCorrespondingFileTypes(),
          new Predicate<FileType>() {

            @Override
            public boolean apply(FileType fileType) {
              return fileType.getSubType() == fileSubType;
            }

          });
    }

    /**
     * Returns an enum matching the type like "ssm", "meth_seq", ...
     */
    public static FeatureType from(String typeName) {
      return valueOf(UNDERSCORE.join(typeName.toUpperCase(), TYPE_SUFFIX));
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

  /**
   * Feature types for which there is a control sample ID.
   */
  private static final Set<FeatureType> CONTROL_SAMPLE_FEATURE_TYPES = of(SSM_TYPE, CNSM_TYPE, STSM_TYPE);

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

  private static Predicate<FeatureType> TYPES_WITH_RAW_SEQUENCE_DATA = new Predicate<FeatureType>() {

    private final Set<FeatureType> TYPES = ImmutableSet.<FeatureType> of(
        METH_ARRAY_TYPE,
        EXP_ARRAY_TYPE,
        PEXP_TYPE);

    @Override
    public boolean apply(FeatureType featureType) {
      return !TYPES.contains(featureType);
    }

  };

  private static Predicate<FeatureType> TYPES_WITH_SEQUENCING_STRATEGY = TYPES_WITH_RAW_SEQUENCE_DATA;

  public static final Proposition HAS_RAW_SEQUENCE_DATA(
      @NonNull final FeatureType featureType) {

    return new Proposition() {

      @Override
      public boolean evaluate() {
        return TYPES_WITH_SEQUENCING_STRATEGY.apply(featureType);
      }

    };

  }

  public static final Proposition HAS_SEQUENCING_STRATEGY(
      @NonNull final FeatureType featureType) {

    return HAS_RAW_SEQUENCE_DATA(featureType);

  }

  public static Iterable<FeatureType> withRawSequenceData(@NonNull final Iterable<FeatureType> items) {
    return filter(items, TYPES_WITH_RAW_SEQUENCE_DATA);
  }

  public static Iterable<FeatureType> withSequencingStrategy(@NonNull final Iterable<FeatureType> items) {
    return filter(items, TYPES_WITH_SEQUENCING_STRATEGY);
  }

}
