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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.ComparisonChain.start;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.mongodb.morphia.annotations.Embedded;

import com.mongodb.BasicDBObject;

/**
 * Reports on a file field.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "name": "f1",
 *    "type": "minmax",
 *    
 *    "nulls": 0,            
 *    "missing": 1,             
 *    "populated": 1,           
 *    "completeness": 0.5,    
 *    
 *    "summary": {
 *      "MIN": 1,
 *      "MAX": 10
 *    }
 * }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "name", "type" })
public class FieldReport implements Comparable<FieldReport> {

  /**
   * Key.
   */
  private String name;
  private SummaryType type;

  /**
   * Values.
   */
  private long nulls;
  private long missing;
  private long populated;
  private double completeness;

  private BasicDBObject summary;

  public FieldReport(@NonNull FieldReport fieldReport) {
    this.name = fieldReport.name;
    this.type = fieldReport.type;

    this.nulls = fieldReport.nulls;
    this.missing = fieldReport.missing;
    this.populated = fieldReport.populated;
    this.completeness = fieldReport.completeness;

    this.summary = (BasicDBObject) fieldReport.summary.copy();
  }

  @Override
  public int compareTo(@NonNull FieldReport other) {
    return start()
        .compare(this.type, other.type)
        .compare(this.name, other.name)
        .result();
  }

}
