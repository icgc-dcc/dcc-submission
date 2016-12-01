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
package org.icgc.dcc.submission.ega.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.common.ega.client.EGAFTPClient.Item;
import org.icgc.dcc.common.ega.dataset.EGADatasetMetaArchive;
import org.icgc.dcc.submission.ega.service.EGAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.val;

@RestController
public class EGAController {

  /**
   * Constants.
   */
  private static final MediaType APPLICATION_TAR = new MediaType("application", "tar");

  /**
   * Dependencies.
   */
  @Autowired
  EGAService service;

  @GetMapping("/api/v1/ega/report")
  public ResponseEntity<List<ObjectNode>> getReport() {
    val report = service.getReport();
    return ResponseEntity
        .ok()
        .lastModified(report.getLastModified())
        .body(report.getFiles());
  }

  @Cacheable("/api/v1/ega/datasets")
  @GetMapping("/api/v1/ega/datasets")
  public Set<String> getDatasetIds() {
    return service.getDatasetIds();
  }

  @Cacheable("/api/v1/ega/ftp")
  @GetMapping("/api/v1/ega/ftp")
  public List<Item> getFtpListing() {
    return service.getFtpListing();
  }

  @Cacheable("/api/v1/ega/datasets/{datasetId}/files")
  @GetMapping("/api/v1/ega/datasets/{datasetId}/files")
  public List<ObjectNode> getFiles(@PathVariable("datasetId") String datasetId) {
    return service.getDatasetFiles(datasetId);
  }

  @Cacheable("/api/v1/ega/datasets/{datasetId}/metadata")
  @GetMapping("/api/v1/ega/datasets/{datasetId}/metadata")
  public EGADatasetMetaArchive getMetadata(@PathVariable("datasetId") String datasetId) {
    return service.getMetadata(datasetId);
  }

  @Cacheable("/api/v1/ega/datasets/{datasetId}/archive")
  @GetMapping("/api/v1/ega/datasets/{datasetId}/archive")
  public ResponseEntity<?> getArchive(@PathVariable("datasetId") String datasetId) throws IOException {
    val archive = service.getArchive(datasetId);

    return ResponseEntity
        .ok()
        .header("Content-Disposition", "attachment; filename=" + datasetId + "tar.gz")
        .contentType(APPLICATION_TAR)
        .body(new InputStreamResource(archive));
  }

}
