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
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.transform;
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
import static org.icgc.dcc.core.model.FileTypes.FileSubType.SECONDARY_SUBTYPE;
import static org.icgc.dcc.core.util.Joiners.UNDERSCORE;
import static org.icgc.dcc.core.util.Optionals.ABSENT_FILE_TYPE;
import static org.icgc.dcc.core.util.Proposition.Propositions.from;
import static org.icgc.dcc.core.util.Strings2.EMPTY_STRING;
import static org.icgc.dcc.core.util.Strings2.removeTarget;

import java.util.List;
import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileSubType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Proposition;

import com.google.common.base.Optional;
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

  private enum SummaryType implements Identifiable {
    COUNT, EXISTS;

    @Override
    public String getId() {
      return name().toLowerCase();
    }

  }

  /**
   * Represents a type of observation data, see {@link ClinicalType} for the clinical counterpart.
   */
  public enum FeatureType implements DataType {

    /** From the ICGC Submission Manual */
    SSM_TYPE(SummaryType.COUNT),
    SGV_TYPE(SummaryType.EXISTS),
    CNSM_TYPE(SummaryType.EXISTS),
    CNGV_TYPE(SummaryType.EXISTS),
    STSM_TYPE(SummaryType.EXISTS),
    STGV_TYPE(SummaryType.EXISTS),
    METH_ARRAY_TYPE(SummaryType.EXISTS),
    METH_SEQ_TYPE(SummaryType.EXISTS),
    MIRNA_SEQ_TYPE(SummaryType.EXISTS),
    EXP_ARRAY_TYPE(SummaryType.EXISTS),
    EXP_SEQ_TYPE(SummaryType.EXISTS),
    PEXP_TYPE(SummaryType.EXISTS),
    JCN_TYPE(SummaryType.EXISTS);

    private FeatureType(@NonNull final SummaryType summaryType) {
      this.summaryType = summaryType;
    }

    private final SummaryType summaryType;

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

    @Override
    public FileType getTopLevelFileType() {
      return getPrimaryFileType();
    }

    @Override
    public String getId() {
      return removeTarget(name(), TYPE_SUFFIX).toLowerCase();
    }

    public String getSummaryFieldName() {
      return UNDERSCORE.join(EMPTY_STRING, this.getId(), summaryType.getId());
    }

    public boolean isSsm() {
      return this == SSM_TYPE;
    }

    public boolean isSgv() {
      return this == SGV_TYPE;
    }

    public boolean isSimple() {
      return isSsm() || isSgv();
    }

    public boolean isCountSummary() {
      return isSsm();
    }

    public boolean hasSequencingStrategy() {
      return FeatureTypes.hasSequencingStrategy(this).evaluate();
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
     * Returns the file sub types corresponding to the feature type.
     * <p>
     * TODO: move to {@link FileTypes} rather
     */
    public Set<FileSubType> getCorrespondingFileSubTypes() {
      return newLinkedHashSet(transform(
          getCorrespondingFileTypes(),
          FileType.getGetSubTypeFunction()));
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

    public Optional<FileType> getSecondaryFileType() {
      return getCorrespondingFileSubTypes().contains(SECONDARY_SUBTYPE) ?
          Optional.of(getFileType(SECONDARY_SUBTYPE)) :
          ABSENT_FILE_TYPE;
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

  private static final Set<FeatureType> TYPES_WITH_RAW_SEQUENCE_DATA = ImmutableSet.of(
      METH_ARRAY_TYPE,
      EXP_ARRAY_TYPE,
      PEXP_TYPE);

  private static final Set<FeatureType> TYPES_WITH_SEQUENCING_STRATEGY = TYPES_WITH_RAW_SEQUENCE_DATA;

  public static Iterable<FeatureType> withRawSequenceData(@NonNull final Iterable<FeatureType> items) {
    return filter(items, hasRawSequenceData());
  }

  public static Iterable<FeatureType> withSequencingStrategy(@NonNull final FeatureType[] items) {
    return withSequencingStrategy(ImmutableSet.copyOf(items));
  }

  public static Iterable<FeatureType> withSequencingStrategy(@NonNull final Iterable<FeatureType> items) {
    return filter(items, hasSequencingStrategy());
  }

  public static Proposition hasRawSequenceData(@NonNull final FeatureType featureType) {
    return from(hasRawSequenceData(), featureType);
  }

  public static Proposition hasSequencingStrategy(@NonNull final FeatureType featureType) {
    return from(hasSequencingStrategy(), featureType);
  }

  public static Proposition hasControlSampleId(@NonNull final FeatureType featureType) {
    return from(hasControlSampleId(), featureType);
  }

  private static Predicate<FeatureType> hasRawSequenceData() {
    return not(in(TYPES_WITH_RAW_SEQUENCE_DATA));
  }

  private static Predicate<FeatureType> hasSequencingStrategy() {
    return not(in(TYPES_WITH_SEQUENCING_STRATEGY));
  }

  private static Predicate<FeatureType> hasControlSampleId() {
    return in(CONTROL_SAMPLE_FEATURE_TYPES);
  }

}
