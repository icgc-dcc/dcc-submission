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

/**
 * Contains names for file schemata (eg. "ssm_p", "cnsm_s", "exp_g", "N/A", ...)
 */
public class FileSchemaNames {

  private static final String SEPARATOR = "_";

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

  // @formatter:off
  public static final String SSM_M = SSM_TYPE + META_SUFFIX;
  public static final String SSM_P = SSM_TYPE + PRIMARY_SUFFIX;
  public static final String SSM_S = SSM_TYPE + SECONDARY_SUFFIX;
  
  public static final String CNSM_M = CNSM_TYPE + META_SUFFIX;
  public static final String CNSM_P = CNSM_TYPE + PRIMARY_SUFFIX;
  public static final String CNSM_S = CNSM_TYPE + SECONDARY_SUFFIX;
  
  public static final String STSM_M = STSM_TYPE + META_SUFFIX;
  public static final String STSM_P = STSM_TYPE + PRIMARY_SUFFIX;
  public static final String STSM_S = STSM_TYPE + SECONDARY_SUFFIX;

  public static final String SGV_M = SGV_TYPE + META_SUFFIX;
  public static final String SGV_P = SGV_TYPE + PRIMARY_SUFFIX;
  public static final String SGV_S = SGV_TYPE + SECONDARY_SUFFIX;
  
  public static final String CNGV_M = CNGV_TYPE + META_SUFFIX;
  public static final String CNGV_P = CNGV_TYPE + PRIMARY_SUFFIX;
  public static final String CNGV_S = CNGV_TYPE + SECONDARY_SUFFIX;
  
  public static final String STGV_M = STGV_TYPE + META_SUFFIX;
  public static final String STGV_P = STGV_TYPE + PRIMARY_SUFFIX;
  public static final String STGV_S = STGV_TYPE + SECONDARY_SUFFIX;
  
  public static final String PEXP_M = PEXP_TYPE + META_SUFFIX;
  public static final String PEXP_P = PEXP_TYPE + PRIMARY_SUFFIX;
  public static final String PEXP_S = PEXP_TYPE + SECONDARY_SUFFIX;
  
  public static final String METH_M = METH_TYPE + META_SUFFIX;
  public static final String METH_P = METH_TYPE + PRIMARY_SUFFIX;
  public static final String METH_S = METH_TYPE + SECONDARY_SUFFIX;

  public static final String MIRNA_M = MIRNA_TYPE + META_SUFFIX;
  public static final String MIRNA_P = MIRNA_TYPE + PRIMARY_SUFFIX;
  public static final String MIRNA_S = MIRNA_TYPE + SECONDARY_SUFFIX;

  public static final String JCN_M = JCN_TYPE + META_SUFFIX;
  public static final String JCN_P = JCN_TYPE + PRIMARY_SUFFIX;
  public static final String JCN_S = JCN_TYPE + SECONDARY_SUFFIX;

  public static final String EXP_M = EXP_TYPE + META_SUFFIX;
  public static final String EXP_G = EXP_TYPE + GENE_SUFFIX;
  // @formatter:on
}
