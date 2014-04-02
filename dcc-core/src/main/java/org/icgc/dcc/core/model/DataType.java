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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import lombok.val;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Represents an ICGC data type, such as "donor", "specimen", "ssm", "meth", ...
 * <p>
 * <<<<<<< HEAD:dcc-core/src/main/java/org/icgc/dcc/core/model/DataType.java Careful not to confuse this with
 * {@link FileType} which represents the ICGC file types, such as "donor", "specimen", "ssm_m", "meth_m", ... They have
 * the clinical ones in common. ======= Careful not to confuse this with {@link FileType} which represents the ICGC file
 * types, such as "donor", "specimen", "ssm_m", "meth_m", ... They have the clinical ones in common. >>>>>>>
 * feature/submission-incremental:dcc-core/src/main/java/org/icgc/dcc/core/model/DataType.java
 */
public interface DataType {

  String TYPE_SUFFIX = "TYPE";

  /**
   * Not really used anywhere (but here for consistency).
   */
  String CLINICAL_OPTIONAL_TYPE_NAME = "optional";

  String name();

  String getTypeName();

  boolean isClinicalType();

  boolean isFeatureType();

  ClinicalType asClinicalType();

  FeatureType asFeatureType();

  public static class DataTypes {

    /**
     * These types are always provided for a submission to be {@link SubmissionState#VALID}.
     */
    private static Set<DataType> MANDATORY_TYPES =
        new ImmutableSet.Builder<DataType>()
            .add(ClinicalType.CLINICAL_CORE_TYPE)
            .build();

    /**
     * Features types that are small enough to be loaded in mongodb (as exposed to exported to hdfs only).
     */
    private static final Set<DataType> MONGO_LOADED_FEATURE_TYPES =
        new ImmutableSet.Builder<DataType>()
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
    public static DataType from(String typeName) {
      DataType type = null;
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

      checkArgument(type != null, "Could not find a match for type %s", typeName);

      return null;
    }

    /**
     * Returns an enum matching the supplied name
     */
    public static DataType valueOf(String name) {
      DataType type = null;
      try {
        return FeatureType.valueOf(name);
      } catch (IllegalArgumentException e) {
        // Do nothing
      }
      try {
        return ClinicalType.valueOf(name);
      } catch (IllegalArgumentException e) {
        // Do nothing
      }

      return checkNotNull(type, "Could not find a match for name %s", name);
    }

    /**
     * Returns the values for all enums that implements the interface.
     */
    public static List<DataType> values() {
      val builder = new ImmutableList.Builder<DataType>();
      for (val type : FeatureType.values()) {
        builder.add(type);
      }
      for (val type : ClinicalType.values()) {
        builder.add(type);
      }

      return builder.build();
    }

    /**
     * Returns the corresponding sorted set of {@link DataType}s (by name).
     * <p>
     * @param dataTypes A list of {@link DataType} which cannot not contain null.
     */
    public static Set<DataType> getSortedSet(Iterable<DataType> dataTypes) {
      val list = newArrayList(dataTypes);
      checkArgument(!list.contains(null), "'null' is not allowed in: '{}'", dataTypes);
      Collections.sort(list, new Comparator<DataType>() {

        @Override
        public int compare(DataType dataType1, DataType dataType2) {
          return dataType1.name().compareTo(dataType2.name());
        }
      });

      return Sets.<DataType> newLinkedHashSet(list);
    }

    /**
     * Checks whether a particular schema is small enough to be stored in mongodb.
     */
    public static boolean isMongoSinkable(DataType type) {
      return MONGO_LOADED_FEATURE_TYPES.contains(type);
    }

    /**
     * Determines whether the type provided is one that must always be included in submissions or not.
     */
    public static boolean isMandatoryType(DataType dataType) {
      return MANDATORY_TYPES.contains(dataType);
    }

    /**
     * Determines whether the type provided is one that has a control counterpart or not.
     */
    public static boolean hasControlSampleId(DataType dataType) {
      return dataType.isFeatureType() &&
          FeatureTypes.hasControlSampleId(dataType.asFeatureType());
    }

    /**
     * Determines whether the type provided is one that is experimental and aggregated, or not.
     */
    public static boolean isAggregatedType(DataType dataType) {
      return dataType.isFeatureType() &&
          FeatureTypes.isAggregatedType(dataType.asFeatureType());
    }

  }

}
