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
package org.icgc.submission.summary;

import lombok.Data;
import lombok.ToString;

import org.icgc.dcc.submission.core.model.Views.Digest;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonView;

@Entity(noClassnameStored = true)
@ToString
@Indexes(@Index(name = "release_project_type", value = "releaseName, projectCode, type"))
@Data
public class ProjectDataTypeReport {

  public ProjectDataTypeReport(String releaseName, String projectCode, String featureType, String sampleType) {
    this.releaseName = releaseName;
    this.projectCode = projectCode;
    this.featureType = featureType;
    this.sampleType = sampleType;
    donorCount = specimenCount = sampleCount = observationCount = 0;
  }

  public ProjectDataTypeReport() {
  }

  @Id
  private String id;

  @JsonView(Digest.class)
  protected String releaseName;

  @JsonView(Digest.class)
  protected String projectCode;

  @JsonView(Digest.class)
  protected String featureType;

  @JsonView(Digest.class)
  protected String sampleType;

  @JsonView(Digest.class)
  protected long donorCount;

  @JsonView(Digest.class)
  protected long specimenCount;

  @JsonView(Digest.class)
  protected long sampleCount;

  @JsonView(Digest.class)
  protected long observationCount;

}
