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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 */
@RequiredArgsConstructor
public enum ConfigKey {
  ALLELE_MASKING_MODE(AlleleMaskingModeValue.getAnyInstance()),
  SWITCH(SwitchValue.getAnyInstance());

  @Getter
  private final ConfigValueEnum<? extends Enum<?>> configValueEnum;

  /**
   * Returns the sub key for the configuration parameter, for instance "switch" in "normalizer.mutation.switch".
   */
  public String getSubKey() {
    return name().toLowerCase();
  }

  /**
   * 
   */
  @RequiredArgsConstructor
  public enum SwitchValue implements ConfigValueEnum<SwitchValue> {
    ENABLED,
    DISABLED;

    @Override
    public String getStringValue() {
      return ConfigValues.getStringValue(this);
    }

    @Override
    public SwitchValue enumValueOf(String value) {
      return valueOf(value.toUpperCase());
    }

    @Override
    public List<SwitchValue> enumValues() {
      return newArrayList(values());
    }

    /**
     * TODO!!
     */
    public static SwitchValue getAnyInstance() {
      return ENABLED;
    }
  }

  /**
   * 
   */
  public enum AlleleMaskingModeValue implements GloballyDefaultable<AlleleMaskingModeValue>, ConfigValueEnum<AlleleMaskingModeValue> {
    ALL, MARK_ONLY;

    @Override
    public String getStringValue() {
      return ConfigValues.getStringValue(this);
    }

    @Override
    public AlleleMaskingModeValue enumValueOf(String value) {
      return valueOf(value.toUpperCase());
    }

    @Override
    public List<AlleleMaskingModeValue> enumValues() {
      return newArrayList(values());
    }

    @Override
    public AlleleMaskingModeValue getGlobalDefaultValue() {
      return ALL;
    }

    /**
     * TODO!!
     */
    public static AlleleMaskingModeValue getAnyInstance() {
      return ALL;
    }
  }

  /**
   * 
   */
  public interface GloballyDefaultable<T> {

    T getGlobalDefaultValue();
  }

  /**
   * 
   */
  public interface ConfigValueEnum<T extends Enum<?>> {

    /**
     * 
     */
    String getStringValue();

    /**
     * 
     */
    T enumValueOf(String value);

    /**
     * 
     */
    List<T> enumValues();

    /**
     * 
     */
    static final class ConfigValues {

      public static String getStringValue(Enum<?> enumInstance) {
        return enumInstance.name();
      }
    }
  }
}
