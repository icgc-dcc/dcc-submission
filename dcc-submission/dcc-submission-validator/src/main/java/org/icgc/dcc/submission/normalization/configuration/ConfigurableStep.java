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
package org.icgc.dcc.submission.normalization.configuration;

import static java.lang.String.format;
import static org.icgc.dcc.submission.normalization.configuration.ConfigKey.SWITCH;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.normalization.configuration.ConfigKey.ConfigValueEnum;
import org.icgc.dcc.submission.normalization.configuration.ConfigKey.SwitchValue;

import com.google.common.base.Optional;
import com.typesafe.config.Config;

/**
 * 
 */
public interface ConfigurableStep {

  /**
   * TODO
   */
  String NORMALIZER_CONFIG_PARAM = "normalizer";

  String getConfigurableStepKey();

  /**
   * 
   */
  String getParameterValue(ConfigKey configKey);

  /**
   * 
   */
  @Slf4j
  public static final class ConfigurableSteps {

    /**
     * 
     */
    public static String getParameterValue(
        Config config,
        ConfigKey configKey,
        ConfigurableStep configurableStep) {

      return getParameterValue(
          config,
          configKey.getSubKey(),
          configurableStep.getConfigurableStepKey());
    }

    /**
     * 
     */
    public static String getParameterValue(
        Config config,
        ConfigKey paramType,
        OptionalStep optionalStep) {

      return getParameterValue(
          config,
          paramType.getSubKey(),
          optionalStep.getOptionalStepKey());
    }

    /**
     * 
     */
    private static String getParameterValue(
        Config config,
        String subKey,
        String stepShortName) {

      String paramKey =
          buildParamKey(
              stepShortName,
              subKey);

      return config.getString(paramKey);
    }

    private static String buildParamKey(String parentKey, String subKey) {
      return format("%s.%s", parentKey, subKey);
    }

    /**
     * 
     */
    public static Optional<? extends Enum<?>> getMatchingValueEnum(
        ConfigValueEnum<? extends Enum<?>> configValueEnum,
        String configuredValue) {

      Optional<? extends Enum<?>> match = null;
      try {
        Enum<?> enumValueOf = configValueEnum.enumValueOf(configuredValue.toUpperCase());

        match = Optional.of(
            enumValueOf);
      } catch (IllegalArgumentException e) {
        log.error(
            "Unknown configuration value specified: '{}', valid ones are: '{}' (case insensitive)",
            configuredValue,
            SwitchValue.values());
        match = Optional.<Enum<?>> absent();
      }
      return match;
    }

    /**
     * 
     */
    public static float getConfidentialErrorThreshold(Config config) {
      return config
          .getNumber("masking.error_threshold")
          .floatValue();
    }
  }

  /**
   * 
   */
  public interface OptionalStep {

    /**
     * 
     */
    boolean isEnabled(Config config);

    /**
     * 
     */
    SwitchValue getDefaultSwitchValue();

    /**
     * @return
     */
    String getOptionalStepKey();

    /**
     * 
     */
    public static final class OptionalSteps {

      /**
       * 
       */
      public static boolean isEnabled(Config config, OptionalStep optionalStep) {
        String configuredValue = ConfigurableSteps.getParameterValue(
            config,
            SWITCH,
            optionalStep);

        Optional<? extends Enum<?>> match = ConfigurableSteps.getMatchingValueEnum(
            SwitchValue.getAnyInstance(),
            configuredValue);

        Enum<?> value = match.isPresent() ?
            match.get() :
            optionalStep.getDefaultSwitchValue();

        return SwitchValue.ENABLED == value;
      }
    }
  }
}
