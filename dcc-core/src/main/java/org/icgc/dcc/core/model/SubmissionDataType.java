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
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Represents an ICGC data type, such as "donor", "specimen", "ssm", "meth", ...
 * <p>
 * Careful not to confuse this with {@link SubmissionFileType} which represents the ICGC file types, such as "donor",
 * "specimen", "ssm_m", "meth_m", ... They have the clinical ones in common.
 */
public interface SubmissionDataType {

  String TYPE_SUFFIX = "TYPE";

  /**
   * Not really used anywhere (but here for consistency).
   */
  String CLINICAL_OPTIONAL_TYPE_NAME = "optional";

  String getTypeName();

  boolean isClinicalType();

  boolean isFeatureType();

  ClinicalType asClinicalType();

  FeatureType asFeatureType();

  public static class SubmissionDataTypes {

    /**
     * These types are always provided for a submission to be {@link SubmissionState#VALID}.
     */
    private static Set<SubmissionDataType> MANDATORY_TYPES =
        new ImmutableSet.Builder<SubmissionDataType>()
            .add(ClinicalType.CLINICAL_CORE_TYPE)
            .build();

    /**
     * Features types that are small enough to be loaded in mongodb (as exposed to exported to hdfs only).
     */
    private static final Set<SubmissionDataType> MONGO_LOADED_FEATURE_TYPES =
        new ImmutableSet.Builder<SubmissionDataType>()
            .add(ClinicalType.CLINICAL_CORE_TYPE)
            .addAll( // All aggregated feature types are mongo sinkable
                Iterables.filter(newArrayList(FeatureType.values()), new Predicate<FeatureType>() {

                  @Override
                  public boolean apply(FeatureType type) {
                    return FeatureTypes.isAggregatedType(type);
                  }
                }))
            .build();

    /**
     * Returns an enum matching the type like "donor", "ssm", "meth", ...
     */
    public static SubmissionDataType from(String typeName) {
      SubmissionDataType type = null;
      try {
        return FeatureType.from(typeName);
      } catch (IllegalArgumentException e) {
        // Do nothing
      }
      try {
        return ClinicalType.from(typeName);
      } catch (IllegalArgumentException e) {
        // Do nothing
      }

      return checkNotNull(type, "Could not find a match for type %s", typeName);
    }

    /**
     * Returns the values for all enums that implements the interface.
     */
    public static List<SubmissionDataType> values() {
      Builder<SubmissionDataType> builder = new ImmutableList.Builder<SubmissionDataType>();
      for (FeatureType type : FeatureType.values()) {
        builder.add(type);
      }
      for (ClinicalType type : ClinicalType.values()) {
        builder.add(type);
      }
      return builder.build();
    }

    /**
     * Checks whether a particular schema is small enough to be stored in mongodb.
     */
    public static boolean isMongoSinkable(SubmissionDataType type) {
      return MONGO_LOADED_FEATURE_TYPES.contains(type);
    }

    /**
     * Determines whether the type provided is one that must always be included in submissions or not.
     */
    public static boolean isMandatoryType(SubmissionDataType dataType) {
      return MANDATORY_TYPES.contains(dataType);
    }

    /**
     * Determines whether the type provided is one that has a control counterpart or not.
     */
    public static boolean hasControlSampleId(SubmissionDataType dataType) {
      return dataType.isFeatureType() &&
          FeatureTypes.hasControlSampleId(dataType.asFeatureType());
    }

    /**
     * Determines whether the type provided is one that is experimental and aggregated, or not.
     */
    public static boolean isAggregatedType(SubmissionDataType dataType) {
      return dataType.isFeatureType() &&
          FeatureTypes.isAggregatedType(dataType.asFeatureType());
    }

  }

}
