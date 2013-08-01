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

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FeatureTypes.CNGV_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.CNSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.EXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.JCN_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.METH_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.MIRNA_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.PEXP_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.SGV_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.SSM_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.STGV_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.STSM_TYPE;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;

/**
 * Contains names for file schemata (eg. "ssm_p", "cnsm_s", "exp_g", "N/A", ...)
 */
@NoArgsConstructor(access = PRIVATE)
public final class FileSchemaNames {

  /**
   * TODO: migrate all constants below to this enum (DCC-1452).
   */
  @RequiredArgsConstructor(access = PRIVATE)
  public enum FileSchemaType implements IcgcFileType {

    SSM_M(buildMetaFileSchemaName(SSM_TYPE)),
    SSM_P(buildPrimaryFileSchemaName(SSM_TYPE)),
    SSM_S(buildSecondaryFileSchemaName(SSM_TYPE)),

    CNSM_M(buildMetaFileSchemaName(CNSM_TYPE)),
    CNSM_P(buildPrimaryFileSchemaName(CNSM_TYPE)),
    CNSM_S(buildSecondaryFileSchemaName(CNSM_TYPE)),

    STSM_M(buildMetaFileSchemaName(STSM_TYPE)),
    STSM_P(buildPrimaryFileSchemaName(STSM_TYPE)),
    STSM_S(buildSecondaryFileSchemaName(STSM_TYPE)),

    SGV_M(buildMetaFileSchemaName(SGV_TYPE)),
    SGV_P(buildPrimaryFileSchemaName(SGV_TYPE)),

    CNGV_M(buildMetaFileSchemaName(CNGV_TYPE)),
    CNGV_P(buildPrimaryFileSchemaName(CNGV_TYPE)),
    CNGV_S(buildSecondaryFileSchemaName(CNGV_TYPE)),

    STGV_M(buildMetaFileSchemaName(STGV_TYPE)),
    STGV_P(buildPrimaryFileSchemaName(STGV_TYPE)),
    STGV_S(buildSecondaryFileSchemaName(STGV_TYPE)),

    PEXP_M(buildMetaFileSchemaName(PEXP_TYPE)),
    PEXP_P(buildPrimaryFileSchemaName(PEXP_TYPE)),

    METH_M(buildMetaFileSchemaName(METH_TYPE)),
    METH_P(buildPrimaryFileSchemaName(METH_TYPE)),
    METH_S(buildSecondaryFileSchemaName(METH_TYPE)),

    MIRNA_M(buildMetaFileSchemaName(MIRNA_TYPE)),
    MIRNA_P(buildPrimaryFileSchemaName(MIRNA_TYPE)),
    MIRNA_S(buildSecondaryFileSchemaName(MIRNA_TYPE)),

    JCN_M(buildMetaFileSchemaName(JCN_TYPE)),
    JCN_P(buildPrimaryFileSchemaName(JCN_TYPE)),
    JCN_S(buildSecondaryFileSchemaName(JCN_TYPE)),

    EXP_M(buildMetaFileSchemaName(EXP_TYPE)),
    EXP_G(buildFileSchemaName(EXP_TYPE, GENE_SUFFIX)), ;

    @Getter
    private final String typeName;

    /**
     * Returns an enum matching the type like "ssm_p", "meth_s", ...
     */
    public static FileSchemaType fromTypeName(String typeName) {
      return valueOf(typeName.toUpperCase());
    }

  }

  private static final String SEPARATOR = "_";

  // TODO: make those private as well (must fix data generator)
  public static final String META_ABBREVIATION = "m";
  public static final String PRIMARY_ABBREVIATION = "p";
  public static final String SECONDARY_ABBREVIATION = "s";
  public static final String GENE_ABBREVIATION = "g";

  private static final String META_SUFFIX = SEPARATOR + META_ABBREVIATION;
  private static final String PRIMARY_SUFFIX = SEPARATOR + PRIMARY_ABBREVIATION;
  private static final String SECONDARY_SUFFIX = SEPARATOR + SECONDARY_ABBREVIATION;
  private static final String GENE_SUFFIX = SEPARATOR + GENE_ABBREVIATION;

  /**
   * Used as placeholder in the loader for imported fields.
   */
  public static final String NOT_APPLICABLE = "N/A";

  public static final String SSM_M = buildMetaFileSchemaName(SSM_TYPE);
  public static final String SSM_P = buildPrimaryFileSchemaName(SSM_TYPE);
  public static final String SSM_S = buildSecondaryFileSchemaName(SSM_TYPE);

  public static final String CNSM_M = buildMetaFileSchemaName(CNSM_TYPE);
  public static final String CNSM_P = buildPrimaryFileSchemaName(CNSM_TYPE);
  public static final String CNSM_S = buildSecondaryFileSchemaName(CNSM_TYPE);

  public static final String STSM_M = buildMetaFileSchemaName(STSM_TYPE);
  public static final String STSM_P = buildPrimaryFileSchemaName(STSM_TYPE);
  public static final String STSM_S = buildSecondaryFileSchemaName(STSM_TYPE);

  public static final String SGV_M = buildMetaFileSchemaName(SGV_TYPE);
  public static final String SGV_P = buildPrimaryFileSchemaName(SGV_TYPE);

  public static final String CNGV_M = buildMetaFileSchemaName(CNGV_TYPE);
  public static final String CNGV_P = buildPrimaryFileSchemaName(CNGV_TYPE);
  public static final String CNGV_S = buildSecondaryFileSchemaName(CNGV_TYPE);

  public static final String STGV_M = buildMetaFileSchemaName(STGV_TYPE);
  public static final String STGV_P = buildPrimaryFileSchemaName(STGV_TYPE);
  public static final String STGV_S = buildSecondaryFileSchemaName(STGV_TYPE);

  public static final String PEXP_M = buildMetaFileSchemaName(PEXP_TYPE);
  public static final String PEXP_P = buildPrimaryFileSchemaName(PEXP_TYPE);

  public static final String METH_M = buildMetaFileSchemaName(METH_TYPE);
  public static final String METH_P = buildPrimaryFileSchemaName(METH_TYPE);
  public static final String METH_S = buildSecondaryFileSchemaName(METH_TYPE);

  public static final String MIRNA_M = buildMetaFileSchemaName(MIRNA_TYPE);
  public static final String MIRNA_P = buildPrimaryFileSchemaName(MIRNA_TYPE);
  public static final String MIRNA_S = buildSecondaryFileSchemaName(MIRNA_TYPE);

  public static final String JCN_M = buildMetaFileSchemaName(JCN_TYPE);
  public static final String JCN_P = buildPrimaryFileSchemaName(JCN_TYPE);
  public static final String JCN_S = buildSecondaryFileSchemaName(JCN_TYPE);

  public static final String EXP_M = buildMetaFileSchemaName(EXP_TYPE);
  public static final String EXP_G = buildFileSchemaName(EXP_TYPE, GENE_SUFFIX);

  public static String buildMetaFileSchemaName(FeatureType type) {
    return buildFileSchemaName(type.getTypeName(), META_SUFFIX);
  }

  public static String buildPrimaryFileSchemaName(FeatureType type) {
    return buildFileSchemaName(type.getTypeName(), PRIMARY_SUFFIX);
  }

  public static String buildSecondaryFileSchemaName(FeatureType type) {
    return buildFileSchemaName(type.getTypeName(), SECONDARY_SUFFIX);
  }

  /**
   * TODO: Remove those once {@link FeatureType} if fully adopted.
   */
  public static String buildMetaFileSchemaName(String type) {
    return buildFileSchemaName(type, META_SUFFIX);
  }

  public static String buildPrimaryFileSchemaName(String type) {
    return buildFileSchemaName(type, PRIMARY_SUFFIX);
  }

  public static String buildSecondaryFileSchemaName(String type) {
    return buildFileSchemaName(type, SECONDARY_SUFFIX);
  }

  private static String buildFileSchemaName(String type, String suffix) {
    return type + suffix;
  }
}
