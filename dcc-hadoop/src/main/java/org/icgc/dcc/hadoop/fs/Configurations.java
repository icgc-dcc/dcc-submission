/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.hadoop.fs;

import static com.google.common.base.Preconditions.checkNotNull;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY;

import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.icgc.dcc.core.util.Hosts;
import org.icgc.dcc.core.util.URIs;
import org.icgc.dcc.hadoop.util.HadoopConstants;

import com.google.common.collect.ImmutableMap;

/**
 * Util methods for {@link Configuration}.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Configurations {

  public static Configuration newConfiguration() {
    return new Configuration();
  }

  public static Configuration newConfiguration(@NonNull final String fsDefault) {
    return addFsDefault(newConfiguration(), fsDefault);
  }

  public static Configuration newConfiguration(
      @NonNull final String fsDefault,
      @NonNull final String jobTracker) {
    return addJobTracker(
        addFsDefault(
            newConfiguration(),
            fsDefault),
        jobTracker);
  }

  public static Configuration newConfiguration(@NonNull final Map<?, ?> properties) {
    val config = newConfiguration();
    for (val property : properties.entrySet()) {
      addProperty(
          config,
          String.valueOf(checkNotNull(
              property.getKey(),
              "Expecting valid key set, instead got '%s'",
              properties.keySet())),
          String.valueOf(checkNotNull(
              property.getValue(),
              "Expecting valid value set, instead got '%s'",
              properties.values())));
    }

    return config;
  }

  public static Configuration newDefaultLocalConfiguration() {
    return newConfiguration(getDefaultLocalPropertiesMap());
  }

  public static Configuration newDefaultDistributedConfiguration() {
    return newConfiguration(getDefaultDistributedPropertiesMap());
  }

  public static Configuration addFsDefault(
      @NonNull final Configuration config,
      @NonNull final String fsDefault) {
    return addProperty(config, FS_DEFAULT_NAME_KEY, fsDefault);
  }

  public static Configuration addJobTracker(
      @NonNull final Configuration config,
      @NonNull final String jobTracker) {
    return addProperty(config, MR_JOBTRACKER_ADDRESS_KEY, jobTracker);
  }

  public static Configuration addProperty(
      @NonNull final Configuration config,
      @NonNull final String key,
      @NonNull final String value) {
    config.set(key, value);

    return config;
  }

  public static Configuration fromMap(@NonNull final Map<?, ?> map) {
    return newConfiguration(map);
  }

  public static Map<?, ?> asMap(@NonNull final Configuration config) {
    val builder = ImmutableMap.builder();
    for (val entry : config) {
      builder.put(entry);
    }

    return builder.build();
  }

  public static Map<?, ?> getDefaultLocalPropertiesMap() {
    return getPropertiesMap(
        URIs.LOCAL_ROOT,
        Hosts.LOCALHOST);
  }

  public static Map<?, ?> getDefaultDistributedPropertiesMap() {
    return getPropertiesMap(
        URIs.HDFS_ROOT,
        Hosts.LOCALHOST);
  }

  public static Map<String, String> getPropertiesMap(
      @NonNull final String fsDefault,
      @NonNull final String jobTracker) {
    return ImmutableMap.of(
        CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, fsDefault,
        HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, jobTracker);
  }

}
