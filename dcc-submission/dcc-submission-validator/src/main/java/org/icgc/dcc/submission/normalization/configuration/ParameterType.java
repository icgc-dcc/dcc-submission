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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 */
@RequiredArgsConstructor
public enum ParameterType {

  ALLELE_MASKING_MODE(AlleleMaskingMode.ALL),
  SWITCH(Switch.ENABLED); // This default value isn't really used (TODO: make optional?)

  @Getter
  private final ParameterValue defaultValue;

  /**
   * Returns the sub key for the configuration parameter, for instance "switch" in "normalizer.mutation.switch".
   */
  public String getSubKey() {
    return name().toLowerCase();
  }

  public String getDefaultValueString() {
    return defaultValue.getStringValue();
  }

  /**
   * 
   */
  @RequiredArgsConstructor
  public enum Switch implements ParameterValue {
    ENABLED,
    DISABLED;

    @Override
    public String getStringValue() {
      return NormalizerConfigurationParameters.getStringValue(this);
    }
  }

  /**
   * 
   */
  public enum AlleleMaskingMode implements ParameterValue {
    ALL, MARK_ONLY;

    @Override
    public String getStringValue() {
      return NormalizerConfigurationParameters.getStringValue(this);
    }
  }

  /**
   * 
   */
  static final class NormalizerConfigurationParameters {

    public static String getStringValue(Enum<?> enuM) {
      return enuM.name();
    }
  }

  /**
   * 
   */
  private interface ParameterValue {

    /**
     * 
     */
    String getStringValue();
  }
}
