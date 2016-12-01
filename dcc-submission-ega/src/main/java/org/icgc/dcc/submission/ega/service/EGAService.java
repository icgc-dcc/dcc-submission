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
package org.icgc.dcc.submission.ega.service;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.common.core.json.JsonNodeBuilders.object;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.common.ega.client.EGAAPIClient;
import org.icgc.dcc.common.ega.client.EGAFTPClient;
import org.icgc.dcc.common.ega.client.EGAFTPClient.Item;
import org.icgc.dcc.common.ega.dataset.EGADatasetMetaArchive;
import org.icgc.dcc.common.ega.dataset.EGADatasetMetaArchiveResolver;
import org.icgc.dcc.common.ega.dump.EGAMetadataDumpReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.NonNull;
import lombok.val;

@Service
public class EGAService {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;

  /**
   * Dependencies.
   */
  @Autowired
  EGAAPIClient api;
  @Autowired
  EGAFTPClient ftp;
  @Autowired
  EGADatasetMetaArchiveResolver archiveResolver;

  public Set<String> getDatasetIds() {
    return archiveResolver.resolveDatasetIds();
  }

  public List<Item> getFtpListing() {
    return ftp.getListing();
  }

  public List<ObjectNode> getReport() {
    val reportFile = new File(workspaceDir, "icgc-ega-report.jsonl");
    if (!reportFile.exists()) {
      return emptyList();
    }

    return new EGAMetadataDumpReader()
        .read(reportFile)
        .map(file -> object()
            .with("projectId", file.path("projectId").path(0))
            .with("fileId", file.get("fileId"))
            .with("submitterSampleId", file.path("samples").path(0).path("submitterSampleId")).end())
        .collect(toList());
  }

  public List<ObjectNode> getDatasetFiles(@NonNull String datasetId) {
    return api.getDatasetFiles(datasetId);
  }

  public EGADatasetMetaArchive getMetadata(@NonNull String datasetId) {
    return archiveResolver.resolveArchive(datasetId);
  }

  public InputStream getArchive(@NonNull String datasetId) throws IOException {
    val archiveUrl = archiveResolver.resolveArchiveUrl(datasetId);
    return archiveUrl.openStream();
  }

}
