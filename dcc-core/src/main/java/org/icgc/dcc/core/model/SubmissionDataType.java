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

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileSchemaTypes.FileSchemaType;

/**
 * Represents an ICGC data type, such as "donor", "specimen", "ssm", "meth", ...
 * <p>
 * Careful not to confuse this with {@link FileSchemaType} which represents the ICGC file types, such as "donor",
 * "specimen", "ssm_m", "meth_m", ... They have the clinical ones in common.
 */
public interface SubmissionDataType {

  static final String TYPE_SUFFIX = "_TYPE";

  String getTypeName();

  public static class SubmissionDataTypes {

    /**
     * Returns an enum matching the type like "donor", "ssm", "meth", ...
     */
    public static SubmissionDataType fromTypeName(String typeName) {
      SubmissionDataType type = null;
      try {
        type = FeatureType.fromTypeName(typeName);
      } catch (IllegalArgumentException e) {
        // Do nothing
      }
      try {
        type = ClinicalType.fromTypeName(typeName);
      } catch (IllegalArgumentException e) {
        // Do nothing
      }
      return checkNotNull(type, "Could not find a match for type %s", typeName);
    }
  }

}
