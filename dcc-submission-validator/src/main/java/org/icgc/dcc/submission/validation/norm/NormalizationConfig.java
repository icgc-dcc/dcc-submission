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
package org.icgc.dcc.submission.validation.norm;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.validation.norm.core.NormalizationStep;
import org.icgc.dcc.submission.validation.norm.steps.MaskedRowGeneration;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

/**
 * Handles configuration related matters for the normalizer component.
 * <p>
 * TODO: remove config from {@link NormalizationValidator} and hide it here.
 */
@NoArgsConstructor(access = PRIVATE)
public final class NormalizationConfig {

  /**
   * Interface to implement for {@link NormalizationStep}s that can be enabled/disabled.
   */
  public interface OptionalStep {}

  /**
   * Key to enable/disable {@link NormalizationStep}s that implement {@link OptionalStep}.
   */
  public static final String ENABLED = "enabled";

  /**
   * Key to set the error above which errors are reported in the normalisation. It defines the maximum ratio of
   * controlled to total observations.
   */
  public static final String ERROR_THRESHOLD = "error_threshold";

  /**
   * Naming variables.
   */
  private static final boolean ON = true;
  @SuppressWarnings("unused")
  private static final boolean OFF = false;

  /**
   * Default values.
   */
  private static final float CONFIDENTIAL_ERROR_THRESHOLD_DEFAULT_VALUE = 0.1f;
  private static final Map<Class<? extends OptionalStep>, Boolean> STEP_ENABLING_DEFAULT_VALUES =
      new ImmutableMap.Builder<Class<? extends OptionalStep>, Boolean>()
          .put(MaskedRowGeneration.class, ON)
          .build();

  /**
   * Checks whether a step is enabled or not. Non-optional step are always considered enabled.
   */
  public static boolean isEnabled(NormalizationStep step, Config config) {
    if (!(step instanceof OptionalStep)) {
      return ON;
    } else {
      val clazz = step.getClass();
      checkState(STEP_ENABLING_DEFAULT_VALUES.containsKey(clazz), "Could not find a default value for step '%s'",
          step.getClass());
      return getBooleanValue(config, getStepEnablingConfigKey(step), STEP_ENABLING_DEFAULT_VALUES.get(clazz));
    }
  }

  /**
   * See {@link #CONFIDENTIAL_ERROR_THRESHOLD_CONFIG_KEY}.
   */
  public static float getConfidentialErrorThreshold(Config config) {
    return getFloatValue(config, ERROR_THRESHOLD, CONFIDENTIAL_ERROR_THRESHOLD_DEFAULT_VALUE);
  }

  private static String getStepEnablingConfigKey(NormalizationStep step) {
    return format("%s.%s", step.shortName(), ENABLED);
  }

  private static float getFloatValue(Config config, String key, float defaultValue) {
    return config.hasPath(key) ? config.getNumber(key).floatValue() : defaultValue;
  }

  private static boolean getBooleanValue(Config config, String key, boolean defaultValue) {
    return config.hasPath(key) ? config.getBoolean(key) : defaultValue;
  }

}
