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

import static java.lang.String.format;
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
import static org.icgc.dcc.core.model.FileSchemaNames.SubmissionFileSubType.GENE;
import static org.icgc.dcc.core.model.FileSchemaNames.SubmissionFileSubType.META;
import static org.icgc.dcc.core.model.FileSchemaNames.SubmissionFileSubType.PRIMARY;
import static org.icgc.dcc.core.model.FileSchemaNames.SubmissionFileSubType.SECONDARY;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;

/**
 * Contains names for file schemata (eg. "ssm_p", "cnsm_s", "exp_g", "N/A", ...)
 */
@NoArgsConstructor(access = PRIVATE)
public final class FileSchemaNames {

  /**
   * TODO: migrate all constants below to this enum (DCC-1452).
   */
  @RequiredArgsConstructor(access = PRIVATE)
  public enum FileSchemaType implements SubmissionFileType {

    SSM_M(META.getFileSchemaName(SSM_TYPE)),
    SSM_P(PRIMARY.getFileSchemaName(SSM_TYPE)),
    SSM_S(SECONDARY.getFileSchemaName(SSM_TYPE)),

    CNSM_M(META.getFileSchemaName(CNSM_TYPE)),
    CNSM_P(PRIMARY.getFileSchemaName(CNSM_TYPE)),
    CNSM_S(SECONDARY.getFileSchemaName(CNSM_TYPE)),

    STSM_M(META.getFileSchemaName(STSM_TYPE)),
    STSM_P(PRIMARY.getFileSchemaName(STSM_TYPE)),
    STSM_S(SECONDARY.getFileSchemaName(STSM_TYPE)),

    SGV_M(META.getFileSchemaName(SGV_TYPE)),
    SGV_P(PRIMARY.getFileSchemaName(SGV_TYPE)),

    CNGV_M(META.getFileSchemaName(CNGV_TYPE)),
    CNGV_P(PRIMARY.getFileSchemaName(CNGV_TYPE)),
    CNGV_S(SECONDARY.getFileSchemaName(CNGV_TYPE)),

    STGV_M(META.getFileSchemaName(STGV_TYPE)),
    STGV_P(PRIMARY.getFileSchemaName(STGV_TYPE)),
    STGV_S(SECONDARY.getFileSchemaName(STGV_TYPE)),

    PEXP_M(META.getFileSchemaName(PEXP_TYPE)),
    PEXP_P(PRIMARY.getFileSchemaName(PEXP_TYPE)),

    METH_M(META.getFileSchemaName(METH_TYPE)),
    METH_P(PRIMARY.getFileSchemaName(METH_TYPE)),
    METH_S(SECONDARY.getFileSchemaName(METH_TYPE)),

    MIRNA_M(META.getFileSchemaName(MIRNA_TYPE)),
    MIRNA_P(PRIMARY.getFileSchemaName(MIRNA_TYPE)),
    MIRNA_S(SECONDARY.getFileSchemaName(MIRNA_TYPE)),

    JCN_M(META.getFileSchemaName(JCN_TYPE)),
    JCN_P(PRIMARY.getFileSchemaName(JCN_TYPE)),

    EXP_M(META.getFileSchemaName(EXP_TYPE)),
    EXP_G(GENE.getFileSchemaName(EXP_TYPE)),

    DONOR(FileType.DONOR_TYPE.getTypeName()),
    SPECIMEN(FileType.SPECIMEN_TYPE.getTypeName()),
    SAMPLE(FileType.SAMPLE_TYPE.getTypeName()),

    BIOMARKER(FileType.BIOMARKER.getTypeName()),
    FAMILY(FileType.FAMILY.getTypeName()),
    EXPOSURE(FileType.EXPOSURE.getTypeName()),
    SURGERY(FileType.SURGERY.getTypeName()),
    THERAPY(FileType.THERAPY.getTypeName());

    @Getter
    private final String typeName;

    /**
     * Returns an enum matching the type like "ssm_p", "meth_s", ...
     */
    public static FileSchemaType fromTypeName(String typeName) {
      return valueOf(typeName.toUpperCase());
    }

  }

  /**
   * TODO: migrate all constants below to this enum (DCC-1452).
   * <p>
   * According to https://wiki.oicr.on.ca/display/DCCINT/Submission+File+Format, this would have to be called "FileType"
   * as well, like "donor", "specimen", ... This seems quite confusing however.
   */
  @RequiredArgsConstructor(access = PRIVATE)
  public enum SubmissionFileSubType {
    META("m"),
    PRIMARY("p"),
    SECONDARY("s"),
    GENE("g");

    private static final String SUFFIX_SEPARATOR = "_";

    private String getFileSchemaName(FeatureType type) {
      return format("%s%s%s", type.getTypeName(), SUFFIX_SEPARATOR, abbrev);
    }

    @Getter
    private final String abbrev;
  }

  /**
   * Checks if schema name provided is that of a meta file schema for the given feature type.
   * <p>
   * TODO: Make this association explicit rather than by convention.
   */
  public static final boolean isMetaFileSchema(FileSchemaType fileSchemaType, FeatureType featureType) {
    return fileSchemaType.getTypeName().equals(META.getFileSchemaName(featureType));
  }

  /**
   * Used as placeholder in the loader for imported fields.
   */
  public static final String NOT_APPLICABLE = "N/A";

  /**
   * TODO: migrate all constants below to enum (DCC-1452).
   */
  public static final String SSM_M = META.getFileSchemaName(SSM_TYPE);
  public static final String SSM_P = PRIMARY.getFileSchemaName(SSM_TYPE);
  public static final String SSM_S = SECONDARY.getFileSchemaName(SSM_TYPE);

  public static final String CNSM_M = META.getFileSchemaName(CNSM_TYPE);
  public static final String CNSM_P = PRIMARY.getFileSchemaName(CNSM_TYPE);
  public static final String CNSM_S = SECONDARY.getFileSchemaName(CNSM_TYPE);

  public static final String STSM_M = META.getFileSchemaName(STSM_TYPE);
  public static final String STSM_P = PRIMARY.getFileSchemaName(STSM_TYPE);
  public static final String STSM_S = SECONDARY.getFileSchemaName(STSM_TYPE);

  public static final String SGV_M = META.getFileSchemaName(SGV_TYPE);
  public static final String SGV_P = PRIMARY.getFileSchemaName(SGV_TYPE);

  public static final String CNGV_M = META.getFileSchemaName(CNGV_TYPE);
  public static final String CNGV_P = PRIMARY.getFileSchemaName(CNGV_TYPE);
  public static final String CNGV_S = SECONDARY.getFileSchemaName(CNGV_TYPE);

  public static final String STGV_M = META.getFileSchemaName(STGV_TYPE);
  public static final String STGV_P = PRIMARY.getFileSchemaName(STGV_TYPE);
  public static final String STGV_S = SECONDARY.getFileSchemaName(STGV_TYPE);

  public static final String PEXP_M = META.getFileSchemaName(PEXP_TYPE);
  public static final String PEXP_P = PRIMARY.getFileSchemaName(PEXP_TYPE);

  public static final String METH_M = META.getFileSchemaName(METH_TYPE);
  public static final String METH_P = PRIMARY.getFileSchemaName(METH_TYPE);
  public static final String METH_S = SECONDARY.getFileSchemaName(METH_TYPE);

  public static final String MIRNA_M = META.getFileSchemaName(MIRNA_TYPE);
  public static final String MIRNA_P = PRIMARY.getFileSchemaName(MIRNA_TYPE);
  public static final String MIRNA_S = SECONDARY.getFileSchemaName(MIRNA_TYPE);

  public static final String JCN_M = META.getFileSchemaName(JCN_TYPE);
  public static final String JCN_P = PRIMARY.getFileSchemaName(JCN_TYPE);
  public static final String JCN_S = SECONDARY.getFileSchemaName(JCN_TYPE);

  public static final String EXP_M = META.getFileSchemaName(EXP_TYPE);
  public static final String EXP_G = GENE.getFileSchemaName(EXP_TYPE);
}
