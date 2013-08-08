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
import static com.google.common.collect.Lists.newArrayList;
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

import java.util.Iterator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;

/**
 * Contains names for file schemata (eg. "ssm_p", "cnsm_s", "exp_g", "N/A", ...)
 */
@NoArgsConstructor(access = PRIVATE)
public final class FileSchemaNames {

  /**
   * TODO: migrate all constants below to this enum (DCC-1452).
   */
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

    private FileSchemaType(String typeName) {
      this.typeName = typeName;
    }

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
  public enum SubmissionFileSubType {
    META,
    PRIMARY,
    SECONDARY,
    GENE,

    DONOR,
    SPECIMEN,
    SAMPLE;

    private static final String SUFFIX_SEPARATOR = "_";
    private static final Joiner JOINER = Joiner.on(SUFFIX_SEPARATOR);
    private static final Splitter SPLITTER = Splitter.on(SUFFIX_SEPARATOR);
    private static final Optional<SubmissionFileSubType> ABSENT = Optional.<SubmissionFileSubType> absent();

    public String getAbbreviation() {
      checkState(!newArrayList(DONOR, SPECIMEN, SAMPLE).contains(this),
          "Clinical sub types do not use abbreviations, attempt was made on %s", this);
      return getFirstCharacter().toLowerCase();
    }

    // @formatter:off
    /**
     * TODO:
     * <li>return enum rather (DCC-1452)</li>
     * <li>change to accept {@link SubmissionFileType} rather + handle clinical as well.</li>
     */
    // @formatter:on
    public String getFileSchemaName(FeatureType type) {
      return JOINER.join(
          type.getTypeName(),
          getAbbreviation());
    }

    public static Optional<SubmissionFileSubType> fromAbbreviation(String abbreviation) {
      for (val item : values()) {
        if (item.getAbbreviation().equals(abbreviation)) {
          return Optional.<SubmissionFileSubType> of(item);
        }
      }
      return ABSENT;
    }

    public static SubmissionFileSubType fromFileSchemaName(String fileSchemaName) {
      return fromFileSchemaType(FileSchemaType.fromTypeName(fileSchemaName));
    }

    public static SubmissionFileSubType fromFileSchemaType(FileSchemaType type) {
      Iterator<String> iterator = SPLITTER.split(type.typeName).iterator();
      checkState(iterator.hasNext(), "Expecting at least one element from %s", type);
      String first = iterator.next();
      Optional<SubmissionFileSubType> subType = iterator.hasNext() ?
          fromAbbreviation(iterator.next()) :
          Optional.of(valueOf(first.toUpperCase()));
      checkState(subType.isPresent(),
          "Could not find any match for: %s", type); // TODO? we may need to address that case in the future
      return subType.get();
    }

    private String getFirstCharacter() {
      return name().substring(0, 1);
    }
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
