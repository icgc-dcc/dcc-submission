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
package org.icgc.dcc.test.es;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.fasterxml.jackson.databind.SerializationFeature.FLUSH_AFTER_WRITE_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.base.Charsets.UTF_8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

@Slf4j
public class ElasticSearchExporter {

  private static final ObjectMapper MAPPER = new ObjectMapper().enable(INDENT_OUTPUT).enable(FLUSH_AFTER_WRITE_VALUE)
      .configure(AUTO_CLOSE_TARGET, false);

  private static final String FILE_EXTENSION = "json";

  private final File directory;

  private final String indexName;

  private final TransportClient esClient;

  public ElasticSearchExporter(File targetDirectory, String indexName, TransportClient esClient) {
    this.directory = targetDirectory;
    this.indexName = indexName;
    this.esClient = esClient;
  }

  @SneakyThrows
  public void execute() {
    for(String typeName : getTypeNames()) {
      File exportFile = getExportFile(typeName);
      log.info("Exporting {}...", exportFile);

      @Cleanup
      Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile), UTF_8));

      SearchHits hits = getSearchHits(typeName);
      for(SearchHit hit : hits) {
        Map<String, Object> source = hit.getSource();

        MAPPER.writeValue(writer, source);
      }
    }
  }

  private Set<String> getTypeNames() {
    ClusterState cs = esClient.admin().cluster().prepareState() //
        .setFilterIndices(indexName).execute().actionGet().getState();
    IndexMetaData imd = cs.getMetaData().index(indexName);
    ImmutableSet<String> types = imd.getMappings().keySet();

    return types;
  }

  private File getExportFile(String typeName) throws IOException {
    File exportFile = new File(directory, typeName + "." + FILE_EXTENSION);
    Files.createParentDirs(exportFile);

    return exportFile;
  }

  private SearchHits getSearchHits(String typeName) throws InterruptedException, ExecutionException {
    SearchResponse searchResponse = esClient.prepareSearch(indexName) //
        .setTypes(typeName) //
        .setSize(100).execute().get();

    return searchResponse.hits();
  }

}
