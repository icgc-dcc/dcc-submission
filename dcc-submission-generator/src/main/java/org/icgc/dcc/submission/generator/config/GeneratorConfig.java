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
package org.icgc.dcc.submission.generator.config;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import org.icgc.dcc.submission.generator.model.ExperimentalFile;
import org.icgc.dcc.submission.generator.model.OptionalFile;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Data
@Accessors(fluent = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class GeneratorConfig {

  /**
   * I/O specification.
   */
  String outputDirectory = "target/";
  Integer stringSize = 10;

  /**
   * Donor specification.
   */
  Integer numberOfDonors = 500;
  Integer numberOfSpecimensPerDonor = 2;
  Integer numberOfSamplesPerSpecimen = 2;

  /**
   * Project information.
   */
  String leadJurisdiction = "au";
  String tumourType = "01";
  String institution = "001";
  String platform = "1";

  /**
   * Random seed for stable generator output.
   */
  Long seed;

  /**
   * File output specifications.
   */
  List<OptionalFile> optionalFiles = newArrayList();
  List<ExperimentalFile> experimentalFiles = newArrayList();

}
