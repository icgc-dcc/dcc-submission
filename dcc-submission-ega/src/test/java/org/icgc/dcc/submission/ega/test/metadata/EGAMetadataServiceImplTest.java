package org.icgc.dcc.submission.ega.test.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.EGAMetadataImporter;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.download.EGAMetadataDownloader;
import org.icgc.dcc.submission.ega.metadata.download.impl.ShellScriptDownloader;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGASampleFileExtractor;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.icgc.dcc.submission.ega.metadata.repo.impl.EGAMetadataRepoPostgres;
import org.icgc.dcc.submission.ega.metadata.service.impl.EGAMetadataServiceImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

/**
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
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

public class EGAMetadataServiceImplTest extends EGAMetadataResourcesProvider {

  private static EGAMetadataDownloader downloader;

  private static DataExtractor<Pair<String, String>> extractor;

  private static EGAMetadataRepo repo;

  private static EGAMetadataConfig.EGAMetadataPostgresqlConfig config;

  @BeforeClass
  public static void prepare() {
    downloader = new ShellScriptDownloader(
        "ftp://admin:admin@localhost:"+ defaultFtpPort + "/ICGC_metadata",
        "/tmp/submission/ega/test/data",
        "/ega/metadata/script/download_ega_metadata.sh"
    );

    extractor = new EGASampleFileExtractor();

    config = new EGAMetadataConfig.EGAMetadataPostgresqlConfig();
    config.setHost("localhost:5435");
    config.setDatabase("ICGC_metadata");
    config.setUser("sa");
    config.setPassword("");
    config.setViewName("view_ega_sample_mapping");

    repo = new EGAMetadataRepoPostgres(config);
  }

  @AfterClass
  public static void tearDown() {

  }

  @Test
  public void test_getData() {

    EGAMetadataImporter importer = new EGAMetadataImporter(downloader, extractor, repo);
    importer.executePeriodically();

    EGAMetadataServiceImpl service = new EGAMetadataServiceImpl(config);

    List<ObjectNode> data = service.getData();

    ImmutableMultimap.Builder builder =  ImmutableMultimap.builder();
    data.stream().forEach(node -> {
      builder.put(node.get("submitterSampleId").textValue(), node.get("fileId").textValue());
    });

    Multimap map = builder.build();

    Collection c = map.get("168-02-8TR");
    Assert.assertFalse(c.isEmpty());
    Assert.assertTrue(c.contains("EGAF00000143419"));
    Assert.assertTrue(c.contains("EGAF00000143420"));

    c = map.get("PD7436c-sc-2013-08-02T02:01:54Z-1674523");
    Assert.assertFalse(c.isEmpty());
    Assert.assertTrue(c.contains("EGAF00000406990"));

  }
}
