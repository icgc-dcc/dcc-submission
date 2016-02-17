/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.config;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.copyOf;
import static org.icgc.dcc.common.core.model.Configurations.HADOOP_KEY;
import static org.icgc.dcc.common.core.util.Strings2.unquote;

import java.util.Map;

import lombok.NonNull;

import org.icgc.dcc.common.core.collect.SerializableMaps;

import com.google.common.base.Function;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

/**
 * TODO: move to core? (would need typesafe config)
 */
public class Configs {

  /**
   * Does not currently support nesting.
   */
  public static Map<String, String> asStringMap(ConfigObject configObject) {
    return copyOf(SerializableMaps.transformMap(
        configObject.unwrapped(),
        new Function<String, String>() {

          @Override
          public String apply(@NonNull final String configKey) {
            return unquote(configKey);
          }

        },
        new Function<Object, String>() {

          @Override
          public String apply(@NonNull final Object configValue) {
            checkState(configValue instanceof String
                || configValue instanceof Number, configValue);
            return String.valueOf(configValue);
          }

        }));
  }

  public static Map<String, String> getHadoopProperties(@NonNull final Config config) {
    return Configs.asStringMap(config.getObject(HADOOP_KEY));
  }

}
