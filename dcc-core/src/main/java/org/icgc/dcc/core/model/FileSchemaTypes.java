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
import static com.google.common.collect.Lists.newArrayList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType.META_SUBTYPE;
import static org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType.PRIMARY_SUBTYPE;
import static org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType.SECONDARY_SUBTYPE;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;

import com.google.common.base.Joiner;

/**
 * Contains names for file schemata (eg. "ssm_p", "cnsm_s", "exp_g", "N/A", ...)
 */
@NoArgsConstructor(access = PRIVATE)
public final class FileSchemaTypes {

  /**
   * Used as placeholder in the loader for imported fields.
   */
  public static final String NOT_APPLICABLE = "N/A";

  /**
   * TODO: migrate all constants below to this enum (DCC-1452).
   * <p>
   * According to https://wiki.oicr.on.ca/display/DCCINT/Submission+File+Format, this would have to be called "FileType"
   * as well, like "donor", "specimen", ... This seems quite confusing however.
   */
  public enum SubmissionSubType {
    META_SUBTYPE,
    PRIMARY_SUBTYPE,
    SECONDARY_SUBTYPE,
    GENE_SUBTYPE,

    DONOR_SUBTYPE,
    SPECIMEN_SUBTYPE,
    SAMPLE_SUBTYPE,

    BIOMARKER_SUBTYPE,
    FAMILY_SUBTYPE,
    EXPOSURE_SUBTYPE,
    SURGERY_SUBTYPE,
    THERAPY_SUBTYPE;

    private static final String SUBTYPE_SUFFIX = "_SUBTYPE";

    /**
     * See {@link #usedAsAbbrevatiation()}.
     */
    private static final List<SubmissionSubType> TYPES_USED_AS_ABBREVIATION =
        newArrayList(META_SUBTYPE, PRIMARY_SUBTYPE, SECONDARY_SUBTYPE, GENE_SUBTYPE);

    public String getAbbreviation() {
      checkState(usedAsAbbrevatiation(),
          "Clinical sub types do not use abbreviations, attempt was made on %s", this);
      return getFirstCharacter().toLowerCase();
    }

    public String getFullName() {
      checkState(!usedAsAbbrevatiation(),
          "Non-clinical sub types use abbreviations, attempt was made on %s", this);
      return name().replace(SUBTYPE_SUFFIX, "").toLowerCase();
    }

    private String getFirstCharacter() {
      return name().substring(0, 1);
    }

    /**
     * Determines whether the sub-type is used as abbreviation for further qualification (for instance "meta" is used as
     * the "_m" suffix) or not (for instance "donor").
     */
    private boolean usedAsAbbrevatiation() {
      return TYPES_USED_AS_ABBREVIATION.contains(this);
    }
  }

  public enum FileSchemaType {

    SSM_M_TYPE(FeatureType.SSM_TYPE, SubmissionSubType.META_SUBTYPE),
    SSM_P_TYPE(FeatureType.SSM_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),
    SSM_S_TYPE(FeatureType.SSM_TYPE, SubmissionSubType.SECONDARY_SUBTYPE),

    CNSM_M_TYPE(FeatureType.CNSM_TYPE, SubmissionSubType.META_SUBTYPE),
    CNSM_P_TYPE(FeatureType.CNSM_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),
    CNSM_S_TYPE(FeatureType.CNSM_TYPE, SubmissionSubType.SECONDARY_SUBTYPE),

    STSM_M_TYPE(FeatureType.STSM_TYPE, META_SUBTYPE),
    STSM_P_TYPE(FeatureType.STSM_TYPE, PRIMARY_SUBTYPE),
    STSM_S_TYPE(FeatureType.STSM_TYPE, SECONDARY_SUBTYPE),

    SGV_M_TYPE(FeatureType.SGV_TYPE, SubmissionSubType.META_SUBTYPE),
    SGV_P_TYPE(FeatureType.SGV_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),

    CNGV_M_TYPE(FeatureType.CNGV_TYPE, SubmissionSubType.META_SUBTYPE),
    CNGV_P_TYPE(FeatureType.CNGV_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),
    CNGV_S_TYPE(FeatureType.CNGV_TYPE, SubmissionSubType.SECONDARY_SUBTYPE),

    STGV_M_TYPE(FeatureType.STGV_TYPE, SubmissionSubType.META_SUBTYPE),
    STGV_P_TYPE(FeatureType.STGV_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),
    STGV_S_TYPE(FeatureType.STGV_TYPE, SubmissionSubType.SECONDARY_SUBTYPE),

    PEXP_M_TYPE(FeatureType.PEXP_TYPE, SubmissionSubType.META_SUBTYPE),
    PEXP_P_TYPE(FeatureType.PEXP_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),

    METH_M_TYPE(FeatureType.METH_TYPE, SubmissionSubType.META_SUBTYPE),
    METH_P_TYPE(FeatureType.METH_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),
    METH_S_TYPE(FeatureType.METH_TYPE, SubmissionSubType.SECONDARY_SUBTYPE),

    MIRNA_M_TYPE(FeatureType.MIRNA_TYPE, SubmissionSubType.META_SUBTYPE),
    MIRNA_P_TYPE(FeatureType.MIRNA_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),
    MIRNA_S_TYPE(FeatureType.MIRNA_TYPE, SubmissionSubType.SECONDARY_SUBTYPE),

    JCN_M_TYPE(FeatureType.JCN_TYPE, SubmissionSubType.META_SUBTYPE),
    JCN_P_TYPE(FeatureType.JCN_TYPE, SubmissionSubType.PRIMARY_SUBTYPE),

    EXP_M_TYPE(FeatureType.EXP_TYPE, SubmissionSubType.META_SUBTYPE),
    EXP_G_TYPE(FeatureType.EXP_TYPE, SubmissionSubType.GENE_SUBTYPE),

    DONOR_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.DONOR_SUBTYPE),
    SPECIMEN_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.SPECIMEN_SUBTYPE),
    SAMPLE_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.SAMPLE_SUBTYPE),

    BIOMARKER_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.BIOMARKER_SUBTYPE),
    FAMILY_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.FAMILY_SUBTYPE),
    EXPOSURE_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.EXPOSURE_SUBTYPE),
    SURGERY_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.SURGERY_SUBTYPE),
    THERAPY_TYPE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.THERAPY_SUBTYPE);

    private static String TYPE_SUFFIX = "_TYPE";

    private static final Joiner JOINER = Joiner.on("_");

    private FileSchemaType(SubmissionDataType type) {
      this(type, null);
    }

    private FileSchemaType(SubmissionDataType type, SubmissionSubType subType) {
      this.dataType = checkNotNull(type);
      this.subType = subType;
    }

    @Getter
    private final SubmissionDataType dataType;

    @Getter
    private final SubmissionSubType subType;

    public String getTypeName() {
      return subType.usedAsAbbrevatiation() ?
          JOINER.join(dataType.getTypeName(), subType.getAbbreviation()) :
          subType.getFullName();
    }

    /**
     * Returns an enum matching the type like "ssm_p", "meth_s", ...
     * <p>
     * TODO: phase out as Strings are replaced with enums.
     */
    public static FileSchemaType from(String typeName) {
      return valueOf(typeName.toUpperCase() + TYPE_SUFFIX);
    }

  }
}
