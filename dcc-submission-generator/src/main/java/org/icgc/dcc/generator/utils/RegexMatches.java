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
package org.icgc.dcc.generator.utils;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringEscapeUtils.escapeJava;
import static org.apache.commons.lang.StringUtils.isBlank;
import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.RestrictionType;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * See https://wiki.oicr.on.ca/display/DCCSOFT/0.6c+to+0.6d.
 * <p>
 * Consider Xeger library to reverse regexes (beware of limitations).
 * <p>
 * TODO: Must support obtaining multiple and unique matches for a regex (DCC-1202).
 */
@NoArgsConstructor(access = PRIVATE)
public final class RegexMatches {

  /**
   * Keep track of corresponding dictionary version since regexes are version-dependent.
   */
  public static final String LATEST_KNOWN_DICTIONARY_VERSION = "0.7e";

  // @formatter:off
  public static final ImmutableMap<String, String> MATCHING_VALUES = new ImmutableMap.Builder<String, String>()
      .put("(?iu)^([ATGC\\-]+$){1,200}","GA")
      .put("(?iu)^([ATGC\\-]+(([,{1}][ATGC\\-]+){1,3})?+)$","AC")
      .put("(?iu)^([ATGC\\-]+/[ATGC\\-]+(([/{1}][ATGC\\-]+){0,2})?+)$","GA/-")
      .put("(?iu)^([ATGC\\-]+/[ATGC\\-]+)$","GA/GA")
      .put("(A|C|G|T|U)+","AUGGUCGCCUUCCACC")
      .put("(^[\\w\\s_\\-\\.]+)((\\s)(http(s)?://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*)?((\\s)[-A-Za-z0-9_\\s|><.]+)?","Paired End http://www.illumina.com/technology/paired_end_sequencing_assay.ilmn")
      .put("^(1)$","1")
      .put("^([\\w+\\-\\_]+)$","LAML_PO_00445")
      .put("^(\\w+)\\:(\\w+)$","Ensembl:69")
      .put("^[\\w+\\-\\_]+$","hnc_12")
      .put("^\\d+$","243198475")
      .build();
  // @formatter:on

  /**
   * Utility to generate the code above.
   */
  public static void main(String... args) {
    // Set to remove duplicates, TreeSet to sort
    val regexes = Maps.<String, String> newTreeMap();
    val splitter = Splitter.on(",").trimResults();

    // Find all the unique regexes
    val fileSchemas = new FileSchemas();
    for (val fileSchema : fileSchemas.getSchemas()) {
      for (val field : fileSchema.getFields()) {
        for (val restriction : field.getRestrictions()) {
          if (restriction.getType() == RestrictionType.REGEX) {
            val config = restriction.getConfig();
            val regex = (String) config.get("pattern");
            val examples = (String) config.get("examples");

            if (regexes.containsKey(regex) && !isBlank(examples)) {
              continue;
            }

            val example = isBlank(examples) ? "" : splitter.splitToList(examples).get(0);
            regexes.put(regex, example);
          }
        }
      }
    }

    for (val entry : regexes.entrySet()) {
      val regex = entry.getKey();
      val example = entry.getValue();

      val line = "      .put(\"" + escapeJava(regex) + "\",\"" + example + "\")";
      System.out.println(line);
    }
  }
}
