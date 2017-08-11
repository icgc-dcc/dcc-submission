package org.icgc.dcc.submission.ega.test.metadata;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.EGAMetadataImporter;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.download.EGAMetadataDownloader;
import org.icgc.dcc.submission.ega.metadata.download.impl.ShellScriptDownloader;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGASampleFileExtractor;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.icgc.dcc.submission.ega.metadata.repo.impl.EGAMetadataRepoPostgres;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import rx.Observable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

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

public class EGAMetadataImporterTest extends EGAMetadataResourcesProvider {

  private static EGAMetadataDownloader downloader;

  private static DataExtractor<Pair<String, String>> extractor;

  private static EGAMetadataRepo repo;

  private static EGAMetadataConfig.EGAMetadataPostgresqlConfig config;

  @BeforeClass
  public static void download(){
    downloader = new ShellScriptDownloader(
        "ftp://admin:admin@localhost:"+ defaultFtpPort + "/ICGC_metadata",
        "/tmp/submission/ega/test/data",
        "/ega/metadata/script/download_ega_metadata.sh"
    );
    downloader.download();

    extractor = new EGASampleFileExtractor();

    config = new EGAMetadataConfig.EGAMetadataPostgresqlConfig();
    config.setHost("localhost:5435");
    config.setDatabase("ICGC_metadata");
    config.setUser("sa");
    config.setPassword("");
    config.setViewName("view_ega_sample_mapping");

    repo = new EGAMetadataRepoPostgres(config);

  }

  @Test
  public void test_getSampleFiles() {
    EGAMetadataImporter importer = new EGAMetadataImporter(downloader, extractor, repo);

    try {
      Method method = EGAMetadataImporter.class.getDeclaredMethod("getSampleFiles", File.class);
      method.setAccessible(true);
      Object ret = method.invoke(importer, new File("/tmp/submission/ega/test/data"));

      Observable<File> observable = (Observable<File>)ret;

      List<File> listFiles = observable.toList().toBlocking().single();

      Assert.assertEquals(listFiles.size(), 2);

      Set<String> fileFullPaths = new HashSet<>();
      fileFullPaths.add("/tmp/submission/ega/test/data/EGAD00001000045/delimited_maps/Sample_File.map");
      fileFullPaths.add("/tmp/submission/ega/test/data/EGAD00001000083/delimited_maps/Sample_File.map");

      listFiles.forEach(file -> {
        Assert.assertTrue(fileFullPaths.contains(file.getAbsolutePath()));
      });
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test_parseSampleFiles() {
    EGAMetadataImporter importer = new EGAMetadataImporter(downloader, extractor, repo);
    try {
      Method method = EGAMetadataImporter.class.getDeclaredMethod("parseSampleFiles", Observable.class);
      method.setAccessible(true);

      Observable<List<Pair<String, String>>> pairs = (Observable<List<Pair<String, String>>>)method.invoke(
          importer,
          Observable.just(
              new File("/tmp/submission/ega/test/data/EGAD00001000045/delimited_maps/Sample_File.map"),
              new File("/tmp/submission/ega/test/data/EGAD00001000083/delimited_maps/Sample_File.map")
          )
      );


      List<Pair<String, String>> data = pairs.flatMap(list -> Observable.from(list)).toList().toBlocking().single();

      data.forEach(pair -> {
        System.out.println(pair.getKey() + " : " + pair.getValue());
      });

      Set<String> keySet = data.stream().map(Pair::getKey).collect(Collectors.toSet());
      Set<String> valueSet = data.stream().map(Pair::getValue).collect(Collectors.toSet());

      Assert.assertNotNull(keySet.contains("PD12852b-sc-2013-08-02T01:56:51Z-1674528"));
      Assert.assertNotNull(keySet.contains("PD9179a-sc-1927644"));

      Assert.assertNotNull(valueSet.contains("EGAF00000406986"));
      Assert.assertNotNull(valueSet.contains("EGAF00000604559"));

    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void test_persist() {
    Set<String> fileIds = new HashSet<>();
    fileIds.add("EGAF00000143419");
    fileIds.add("EGAF00000143420");
    fileIds.add("EGAF00000406990");

    EGAMetadataImporter importer = new EGAMetadataImporter(downloader, extractor, repo);

    try {
      Method method = EGAMetadataImporter.class.getDeclaredMethod("persist", Observable.class);
      method.setAccessible(true);
      method.invoke(importer, loadSampleData());

      JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://localhost:5435/ICGC_metadata?user=sa&password="));

      String sql = "select * from view_ega_sample_mapping where sample_id = '" + "168-02-8TR';";
      List<Map<String, Object>> ret = jdbcTemplate.queryForList(sql);
      Assert.assertTrue(2 == ret.size());
      ret.stream().forEach(map -> {
        Assert.assertTrue( fileIds.contains(map.get("file_id").toString()) );
      });

      sql = "select * from view_ega_sample_mapping where sample_id = '" + "PD7436c-sc-2013-08-02T02:01:54Z-1674523';";
      ret = jdbcTemplate.queryForList(sql);
      Assert.assertEquals(1, ret.size());
      ret.stream().forEach(map -> {
        Assert.assertTrue( fileIds.contains(map.get("file_id").toString()) );
      });

    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

  }

  private Observable<List<Pair<String, String>>> loadSampleData(){
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(EGAMetadataImporterTest.class.getResourceAsStream("/ega/metadata/sample/Sample_file_45.map")));
      List<Pair<String, String>> data1 = new ArrayList<>();

      String line;
      while((line = br.readLine()) != null){
        List<String> fields = Splitter.on(CharMatcher.BREAKING_WHITESPACE).omitEmptyStrings().trimResults().splitToList(line);
        data1.add(Pair.of(fields.get(0), fields.get(3)));
      }
      br.close();

      List<Pair<String, String>> data2 = new ArrayList<>();
      br = new BufferedReader(new InputStreamReader(EGAMetadataImporterTest.class.getResourceAsStream("/ega/metadata/sample/Sample_file_83.map")));
      while((line = br.readLine()) != null){
        List<String> fields = Splitter.on(CharMatcher.BREAKING_WHITESPACE).omitEmptyStrings().trimResults().splitToList(line);
        data2.add(Pair.of(fields.get(0), fields.get(3)));
      }
      br.close();

      return Observable.just(data1, data2);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Observable.empty();
  }

  @Test
  public void test_executePeriodically() {

    Set<String> fileIds = new HashSet<>();
    fileIds.add("EGAF00000143419");
    fileIds.add("EGAF00000143420");
    fileIds.add("EGAF00000406990");

    EGAMetadataImporter importer = new EGAMetadataImporter(downloader, extractor, repo);

    try {

      File tmpDataDir = new File("/tmp/submission/ega/test/data");
      if(tmpDataDir.exists())
        FileUtils.deleteDirectory(tmpDataDir);

      Method method = EGAMetadataImporter.class.getDeclaredMethod("executePeriodically");
      method.setAccessible(true);
      method.invoke(importer);

      JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://localhost:5435/ICGC_metadata?user=sa&password="));

      String sql = "select * from view_ega_sample_mapping where sample_id = '" + "168-02-8TR';";
      List<Map<String, Object>> ret = jdbcTemplate.queryForList(sql);
      Assert.assertTrue(2 == ret.size());
      ret.stream().forEach(map -> {
        Assert.assertTrue( fileIds.contains(map.get("file_id").toString()) );
      });

      sql = "select * from view_ega_sample_mapping where sample_id = '" + "PD7436c-sc-2013-08-02T02:01:54Z-1674523';";
      ret = jdbcTemplate.queryForList(sql);
      Assert.assertEquals(1, ret.size());
      ret.stream().forEach(map -> {
        Assert.assertTrue( fileIds.contains(map.get("file_id").toString()) );
      });

    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
