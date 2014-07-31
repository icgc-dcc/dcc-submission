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

import static com.google.common.base.Splitter.on;
import static org.icgc.dcc.core.util.FormatUtils._;
import lombok.NonNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Common splitters.
 */
public class Splitters {

  public static final Splitter WHITESPACE = on(Separators.WHITESPACE);
  public static final Splitter EMPTY_STRING = on(Separators.EMPTY_STRING);
  public static final Splitter TAB = on(Separators.TAB);
  public static final Splitter NEWLINE = on(Separators.NEWLINE);
  public static final Splitter SLASH = on(Separators.SLASH);
  public static final Splitter DOT = on(Separators.DOT);
  public static final Splitter DASH = on(Separators.DASH);
  public static final Splitter UNDERSCORE = on(Separators.UNDERSCORE);
  public static final Splitter COLON = on(Separators.COLON);
  public static final Splitter COMMA = on(Separators.COMMA);
  public static final Splitter SEMICOLON = on(Separators.SEMICOLON);

  // Aliases
  public static final Splitter PATH = SLASH;
  public static final Splitter EXTENSION = DOT;
  public static final Splitter CREDENTIALS = COLON;

  /**
   * TODO: consider enum rather?
   */
  public static final Joiner getCorrespondingJoiner(@NonNull final Splitter splitter) {
    if (splitter.equals(WHITESPACE)) {
      return Joiners.WHITESPACE;
    } else if (splitter.equals(EMPTY_STRING)) {
      return Joiners.EMPTY_STRING;
    } else if (splitter.equals(SLASH) || splitter.equals(PATH)) {
      return Joiners.SLASH;
    } else if (splitter.equals(TAB)) {
      return Joiners.TAB;
    } else if (splitter.equals(NEWLINE)) {
      return Joiners.NEWLINE;
    } else if (splitter.equals(DOT) || splitter.equals(EXTENSION)) {
      return Joiners.DOT;
    } else if (splitter.equals(DASH)) {
      return Joiners.DASH;
    } else if (splitter.equals(UNDERSCORE)) {
      return Joiners.UNDERSCORE;
    } else if (splitter.equals(COMMA)) {
      return Joiners.COMMA;
    } else if (splitter.equals(COLON) || splitter.equals(CREDENTIALS)) {
      return Joiners.COLON;
    } else if (splitter.equals(SEMICOLON)) {
      return Joiners.SEMICOLON;
    } else if (splitter.equals(PATH)) {
      return Joiners.PATH;
    } else {
      throw new UnsupportedOperationException(_("Unsupported yet: '%s'", splitter));
    }
  }

}
