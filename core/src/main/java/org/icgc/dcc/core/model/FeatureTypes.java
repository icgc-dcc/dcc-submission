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

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Utilities for working with ICGC feature types.
 */
public final class FeatureTypes {

  /** From the ICGC Submission Manual */
  private static final List<String> FEATURE_TYPES = ImmutableList.of("ssm", "sgv", "cnsm", "cngv", "stsm", "stgv",
      "exp", "mirna", "jcn", "meth");

  /** Subset of {@link #FEATURE_TYPES} that relates to somatic mutations */
  private static final List<String> SOMATIC_FEATURE_TYPES = ImmutableList.of("ssm", "cnsm", "stsm");

  private static final Set<String> SOMATIC_FEATURE_TYPES_SET = ImmutableSet.copyOf(SOMATIC_FEATURE_TYPES);

  public static List<String> getTypes() {
    return FEATURE_TYPES;
  }

  public static List<String> getSomaticTypes() {
    return SOMATIC_FEATURE_TYPES;
  }

  public static boolean isSomaticType(String type) {
    return SOMATIC_FEATURE_TYPES_SET.contains(type);
  }

}
