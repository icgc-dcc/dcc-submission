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

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.ImmutableSet.of;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.FormatUtils._;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Common joiners.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Joiners {

  public static final Joiner WHITESPACE = on(Separators.WHITESPACE);
  public static final Joiner EMPTY_STRING = on(Separators.EMPTY_STRING);
  public static final Joiner SLASH = on(Separators.SLASH);
  public static final Joiner TAB = on(Separators.TAB);
  public static final Joiner NEWLINE = on(Separators.NEWLINE);
  public static final Joiner DOT = on(Separators.DOT);
  public static final Joiner DASH = on(Separators.DASH);
  public static final Joiner UNDERSCORE = on(Separators.UNDERSCORE);
  public static final Joiner COMMA = on(Separators.COMMA);
  public static final Joiner COLON = on(Separators.COLON);
  public static final Joiner SEMICOLON = on(Separators.SEMICOLON);
  public static final Joiner HASHTAG = on(Separators.HASHTAG);

  // Combinations
  public static final Joiner DOUBLE_DASH = on(Separators.DOUBLE_DASH);

  // Aliases
  public static final Joiner PATH = SLASH;
  public static final Joiner EXTENSION = DOT;
  public static final Joiner NAMESPACING = DOT;
  public static final Joiner HOST_AND_PORT = COLON;
  public static final Joiner CREDENTIALS = COLON;

  // Formatting
  public static final Joiner INDENT = on(Separators.INDENT);

  /**
   * TODO: consider enum rather?
   */
  public static final Splitter getCorrespondingSplitter(@NonNull final Joiner joiner) {

    // Basic ones
    if (joiner.equals(WHITESPACE)) {
      return Splitters.WHITESPACE;
    } else if (joiner.equals(TAB)) {
      return Splitters.TAB;
    } else if (joiner.equals(NEWLINE)) {
      return Splitters.NEWLINE;
    } else if (joiner.equals(DASH)) {
      return Splitters.DASH;
    } else if (joiner.equals(UNDERSCORE)) {
      return Splitters.UNDERSCORE;
    } else if (joiner.equals(COMMA)) {
      return Splitters.COMMA;
    } else if (joiner.equals(SEMICOLON)) {
      return Splitters.SEMICOLON;
    } else if (joiner.equals(HASHTAG)) {
      return Splitters.HASHTAG;
    }

    // Combinations
    else if (joiner.equals(DOUBLE_DASH)) {
      return Splitters.DOUBLE_DASH;
    }

    // Aliases
    else if (of(DOT, EXTENSION, NAMESPACING).contains(joiner)) {
      return Splitters.DOT;
    } else if (of(SLASH, PATH).contains(joiner)) {
      return Splitters.SLASH;
    } else if (of(COLON, CREDENTIALS, HOST_AND_PORT).contains(joiner)) {
      return Splitters.COLON;
    }

    // Special ones
    else if (joiner.equals(EMPTY_STRING)) {
      throw new IllegalStateException(_("Cannot split using '{}'", EMPTY_STRING));
    }

    // Error
    else {
      throw new UnsupportedOperationException(_("Unsupported yet: '%s'", joiner));
    }
  }

}
