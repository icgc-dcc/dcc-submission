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
import static org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType.META;
import static org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType.PRIMARY;
import static org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType.SECONDARY;

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
    META,
    PRIMARY,
    SECONDARY,
    GENE,

    DONOR,
    SPECIMEN,
    SAMPLE,

    BIOMARKER,
    FAMILY,
    EXPOSURE,
    SURGERY,
    THERAPY;

    /**
     * See {@link #usedAsAbbrevatiation()}.
     */
    private static final List<SubmissionSubType> TYPES_USED_AS_ABBREVIATION =
        newArrayList(META, PRIMARY, SECONDARY, GENE);

    public String getAbbreviation() {
      checkState(usedAsAbbrevatiation(),
          "Clinical sub types do not use abbreviations, attempt was made on %s", this);
      return getFirstCharacter().toLowerCase();
    }

    public String getFullName() {
      checkState(!usedAsAbbrevatiation(),
          "Non-clinical sub types use abbreviations, attempt was made on %s", this);
      return name().toLowerCase();
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

    SSM_M(FeatureType.SSM_TYPE, SubmissionSubType.META),
    SSM_P(FeatureType.SSM_TYPE, SubmissionSubType.PRIMARY),
    SSM_S(FeatureType.SSM_TYPE, SubmissionSubType.SECONDARY),

    CNSM_M(FeatureType.CNSM_TYPE, SubmissionSubType.META),
    CNSM_P(FeatureType.CNSM_TYPE, SubmissionSubType.PRIMARY),
    CNSM_S(FeatureType.CNSM_TYPE, SubmissionSubType.SECONDARY),

    STSM_M(FeatureType.STSM_TYPE, META),
    STSM_P(FeatureType.STSM_TYPE, PRIMARY),
    STSM_S(FeatureType.STSM_TYPE, SECONDARY),

    SGV_M(FeatureType.SGV_TYPE, SubmissionSubType.META),
    SGV_P(FeatureType.SGV_TYPE, SubmissionSubType.PRIMARY),

    CNGV_M(FeatureType.CNGV_TYPE, SubmissionSubType.META),
    CNGV_P(FeatureType.CNGV_TYPE, SubmissionSubType.PRIMARY),
    CNGV_S(FeatureType.CNGV_TYPE, SubmissionSubType.SECONDARY),

    STGV_M(FeatureType.STGV_TYPE, SubmissionSubType.META),
    STGV_P(FeatureType.STGV_TYPE, SubmissionSubType.PRIMARY),
    STGV_S(FeatureType.STGV_TYPE, SubmissionSubType.SECONDARY),

    PEXP_M(FeatureType.PEXP_TYPE, SubmissionSubType.META),
    PEXP_P(FeatureType.PEXP_TYPE, SubmissionSubType.PRIMARY),

    METH_M(FeatureType.METH_TYPE, SubmissionSubType.META),
    METH_P(FeatureType.METH_TYPE, SubmissionSubType.PRIMARY),
    METH_S(FeatureType.METH_TYPE, SubmissionSubType.SECONDARY),

    MIRNA_M(FeatureType.MIRNA_TYPE, SubmissionSubType.META),
    MIRNA_P(FeatureType.MIRNA_TYPE, SubmissionSubType.PRIMARY),
    MIRNA_S(FeatureType.MIRNA_TYPE, SubmissionSubType.SECONDARY),

    JCN_M(FeatureType.JCN_TYPE, SubmissionSubType.META),
    JCN_P(FeatureType.JCN_TYPE, SubmissionSubType.PRIMARY),

    EXP_M(FeatureType.EXP_TYPE, SubmissionSubType.META),
    EXP_G(FeatureType.EXP_TYPE, SubmissionSubType.GENE),

    DONOR(ClinicalType.CLINICAL_TYPE, SubmissionSubType.DONOR),
    SPECIMEN(ClinicalType.CLINICAL_TYPE, SubmissionSubType.SPECIMEN),
    SAMPLE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.SAMPLE),

    BIOMARKER(ClinicalType.CLINICAL_TYPE, SubmissionSubType.BIOMARKER),
    FAMILY(ClinicalType.CLINICAL_TYPE, SubmissionSubType.FAMILY),
    EXPOSURE(ClinicalType.CLINICAL_TYPE, SubmissionSubType.EXPOSURE),
    SURGERY(ClinicalType.CLINICAL_TYPE, SubmissionSubType.SURGERY),
    THERAPY(ClinicalType.CLINICAL_TYPE, SubmissionSubType.THERAPY);

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
      return valueOf(typeName.toUpperCase());
    }

  }
}
