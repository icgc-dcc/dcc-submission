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
package org.icgc.dcc.core.model;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

/**
 * Values with a special meaning.
 */
public class SpecialValue {

  /**
   * Former reserved values that must not appear in required data anymore.
   */
  public static final List<String> DEPRECATED_VALUES = newArrayList("-999");

  public static final String MISSING_CODE1 = "-777";
  public static final String MISSING_CODE2 = "-888";

  /**
   * Code used in legacy submissions to fill in a value that is strictly required but wasn't before.
   */
  public static final String LEGACY_CODE = "-9999";

  /**
   * Values representing absent values.
   * <p>
   * "-999" has been deprecated {@link ForbiddenValuesFunction}
   */
  public static final List<String> MISSING_CODES =
      newArrayList(MISSING_CODE1, MISSING_CODE2, LEGACY_CODE);

  /**
   * Value used to represent "nothing" in cascading {@link Tuple}s.
   */
  public static final Object NO_VALUE = null;

}
