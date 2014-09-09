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

import static com.google.common.collect.ImmutableSet.of;
import static org.icgc.dcc.core.util.FormatUtils._;
import lombok.NonNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Common separators.
 */
public class Separators {

  public static final char TAB_CHARACTER = '\t';

  public static final String EMPTY_STRING = Strings2.EMPTY_STRING;
  public static final String NEWLINE = Strings2.UNIX_NEW_LINE;
  public static final String TAB = Strings2.TAB;
  public static final String DOT = ".";
  public static final String COMMA = ",";
  public static final String DASH = "-";
  public static final String UNDERSCORE = "_";
  public static final String SLASH = "/";
  public static final String WHITESPACE = " ";
  public static final String COLON = ":";
  public static final String SEMICOLON = ";";
  public static final String HASHTAG = "#";
  public static final String DOLLAR = "$";

  // Combinations
  public static final String DOUBLE_DASH = DASH + DASH;

  // Aliases
  public static final String EXTENSION = Strings2.DOT;
  public static final String PATH = SLASH;
  public static final String CREDENTIALS = COLON;
  public static final String HOST_AND_PORT = COLON;
  public static final String NAMESPACING = DOT;

  // Formatting
  public static final String INDENT = Separators.NEWLINE + Separators.TAB;

  public static final Splitter getCorrespondingSplitter(@NonNull final String separator) {
    return getCorrespondingObject(separator, true);
  }

  public static final Joiner getCorrespondingJoiner(@NonNull final String separator) {
    return getCorrespondingObject(separator, false);
  }

  /**
   * TODO: write cleaner version
   */
  @SuppressWarnings("unchecked")
  private final static <T> T getCorrespondingObject(
      @NonNull final String separator,
      final boolean splitter) {

    // Basic ones
    if (separator.equals(WHITESPACE)) {
      return (T) (splitter ? Splitters.WHITESPACE : Joiners.WHITESPACE);
    } else if (separator.equals(TAB)) {
      return (T) (splitter ? Splitters.TAB : Joiners.TAB);
    } else if (separator.equals(NEWLINE)) {
      return (T) (splitter ? Splitters.NEWLINE : Joiners.NEWLINE);
    } else if (separator.equals(DASH)) {
      return (T) (splitter ? Splitters.DASH : Joiners.DASH);
    } else if (separator.equals(UNDERSCORE)) {
      return (T) (splitter ? Splitters.UNDERSCORE : Joiners.UNDERSCORE);
    } else if (separator.equals(COMMA)) {
      return (T) (splitter ? Splitters.COMMA : Joiners.COMMA);
    } else if (separator.equals(SEMICOLON)) {
      return (T) (splitter ? Splitters.SEMICOLON : Joiners.SEMICOLON);
    } else if (separator.equals(HASHTAG)) {
      return (T) (splitter ? Splitters.HASHTAG : Joiners.HASHTAG);
    }

    // Combinations
    else if (separator.equals(DOUBLE_DASH)) {
      return (T) (splitter ? Splitters.DOUBLE_DASH : Joiners.DOUBLE_DASH);
    }

    // Aliased ones
    else if (of(DOT, EXTENSION, NAMESPACING).contains(separator)) {
      return (T) (splitter ? Splitters.DOT : Joiners.DOT);
    } else if (of(SLASH, PATH).contains(separator)) {
      return (T) (splitter ? Splitters.SLASH : Joiners.SLASH);
    } else if (of(COLON, CREDENTIALS, HOST_AND_PORT).contains(separator)) {
      return (T) (splitter ? Splitters.COLON : Joiners.COLON);
    }

    // Special ones
    else if (separator.equals(EMPTY_STRING)) {
      throw new IllegalStateException(_("Cannot split/join using '{}'", EMPTY_STRING)); // TODO: confirm for join
    }

    // Error
    else {
      throw new UnsupportedOperationException(_("Unsupported yet: '%s'", separator));
    }

  }
}
