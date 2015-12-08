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

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Classification of specimen type code list terms.
 * 
 * @see http://docs.icgc.org/controlled-vocabulary#specimen.0.specimen_type.v3
 */
@RequiredArgsConstructor(access = PRIVATE)
public enum SpecimenTypeCategory {

  /**
   * Healthy.
   */
  NORMAL("normal"),

  /**
   * Tumor related.
   */
  NON_NORMAL("non-normal");

  /**
   * Metadata.
   */
  @Getter
  private final String description;

  /**
   * Constants.
   */
  private static final Set<String> NORMAL_TERM_CODES = ImmutableSet.<String> builder()
      .add("101")
      .add("102")
      .add("103")
      .add("104")
      .add("105")
      .add("106")
      .add("107")
      .add("108")
      .build();
  private static final Set<String> NORMAL_TERM_VALUES = ImmutableSet.<String> builder()
      .add("Normal - solid tissue")
      .add("Normal - blood derived")
      .add("Normal - bone marrow")
      .add("Normal - tissue adjacent to primary")
      .add("Normal - buccal cell")
      .add("Normal - EBV immortalized")
      .add("Normal - lymph node")
      .add("Normal - other")
      .build();

  public static SpecimenTypeCategory fromSpecimenType(String term) {
    val normal = NORMAL_TERM_CODES.contains(term) || NORMAL_TERM_VALUES.contains(term);

    return normal ? NORMAL : NON_NORMAL;
  }

  /**
   * @see https://jira.oicr.on.ca/browse/DCC-3629
   */
  public static boolean isBothCategories(String specimenType) {
    // Code or value
    return "Normal - tissue adjacent to primary".equals(specimenType) || "104".equals(specimenType);
  }

}
