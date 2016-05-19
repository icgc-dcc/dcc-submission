/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.dictionary.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.regex.Pattern;

import org.icgc.dcc.submission.dictionary.model.CodeList;

import com.google.common.collect.Maps;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class CodeLists {

  /**
   * Constants.
   */
  private static final Pattern SPECIMEN_NAME_PATTERN = Pattern.compile("specimen\\.0\\.specimen_type\\.v(\\d+)");

  public static CodeList getSpecimenTypes(@NonNull List<CodeList> codeLists) {
    return getLatest(codeLists, SPECIMEN_NAME_PATTERN);
  }

  private static CodeList getLatest(List<CodeList> codeLists, Pattern pattern) {
    val versions = Maps.<Integer, CodeList> newTreeMap();
    for (val codeList : codeLists) {
      val matcher = pattern.matcher(codeList.getName());
      if (matcher.matches()) {
        // If there is no version component then just use 0
        val version = matcher.groupCount() == 0 ? 0 : Integer.valueOf(matcher.group(1));
        versions.put(version, codeList);
      }
    }

    // Latest version
    return versions.lastEntry().getValue();
  }

}
