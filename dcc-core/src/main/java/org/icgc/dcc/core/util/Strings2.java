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
package org.icgc.dcc.core.util;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utils methods for {@link String}.
 */
@NoArgsConstructor(access = PRIVATE)
public class Strings2 {

  public static final String DOT = ".";
  public static final String EMPTY_STRING = "";
  public static final String TAB = "\t";
  public static final String UNIX_NEW_LINE = "\n";
  public static final String DOUBLE_QUOTE = "\"";
  public static final String SINGLE_QUOTE = "'";
  public static final String NOT_APPLICABLE = "N/A";

  public static String removeTrailingS(@NonNull final String s) {
    return s.replaceAll("s$", "");
  }

  /**
   * Not appropriate for very big {@link String}s.
   */
  public static boolean isLowerCase(@NonNull final String s) {
    return s.equals(s.toLowerCase());
  }

  /**
   * Not appropriate for very big {@link String}s.
   */
  public static boolean isUpperCase(@NonNull final String s) {
    return s.equals(s.toUpperCase());
  }

  public static String removeTarget(@NonNull final String s, @NonNull final String target) {
    return s.replace(target, EMPTY_STRING);
  }

  public static String unquote(@NonNull final String s) {
    return s
        .replaceAll(DOUBLE_QUOTE, EMPTY_STRING)
        .replaceAll(SINGLE_QUOTE, EMPTY_STRING);
  }

  public static String getFirstCharacter(@NonNull final String s) {
    return s.substring(0, 1);
  }

}
