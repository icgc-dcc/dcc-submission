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
package org.icgc.dcc.submission.validation.sample.core;

import java.util.Set;

import lombok.val;

import com.google.common.collect.ImmutableSet;

/**
 * Classification of reference sample type code list terms.
 * 
 * @see http://docs.icgc.org/controlled-vocabulary#GLOBAL.0.reference_sample_type.v1
 */
public enum ReferenceSampleTypeCategory {

  /**
   * Matched from donor
   */
  MATCHED,

  /**
   * Anything other than {@link ReferenceSampleTypeCategory#MATCHED}.
   */
  NON_MATCHED;

  /**
   * Constants.
   */
  private static final Set<String> MATCHED_TERM_CODES = ImmutableSet.of("1");
  private static final Set<String> NON_MATCHED_TERM_VALUES = ImmutableSet.of("matched normal");

  public static ReferenceSampleTypeCategory fromSampleType(String term) {
    val matched = MATCHED_TERM_CODES.contains(term) || NON_MATCHED_TERM_VALUES.contains(term);

    return matched ? MATCHED : NON_MATCHED;
  }

}
