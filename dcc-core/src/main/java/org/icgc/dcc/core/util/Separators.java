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

/**
 * Common separators.
 */
public class Separators {

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

  // Aliases
  public static final String EXTENSION = Strings2.DOT;
  public static final String PATH = SLASH;
  public static final String CREDENTIALS = COLON;

  // Formatting
  public static final String INDENT = Separators.NEWLINE + Separators.TAB;

}
