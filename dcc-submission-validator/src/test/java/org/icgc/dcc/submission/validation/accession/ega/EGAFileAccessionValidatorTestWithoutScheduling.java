package org.icgc.dcc.submission.validation.accession.ega;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.validation.accession.ega.download.impl.ShellScriptDownloader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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

public class EGAFileAccessionValidatorTestWithoutScheduling extends EGAFileAccessionValidatorFtpProvider{

  @BeforeClass
  public static void download(){

    (new ShellScriptDownloader(
        "ftp://admin:admin@localhost:"+ defaultFtpPort + "/ICGC_metadata",
        "/tmp/submission/ega/test/data",
        "/fixtures/validation/accession/ega/metadata.sh"
    )).download();

  }

  @Test
  public void test_getSampleFiles() {
    EGAFileAccessionValidator validator = new EGAFileAccessionValidator();
    try {
      Method method = EGAFileAccessionValidator.class.getDeclaredMethod("getSampleFiles", File.class);
      method.setAccessible(true);
      Object ret = method.invoke(validator, new File("/tmp/submission/ega/test/data"));

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
    EGAFileAccessionValidator validator = new EGAFileAccessionValidator();
    try {
      Method method = EGAFileAccessionValidator.class.getDeclaredMethod("parseSampleFiles", Observable.class);
      method.setAccessible(true);

      Observable<Pair<String, String>> pairs = (Observable<Pair<String, String>>)method.invoke(
        validator,
        Observable.just(
            new File("/tmp/submission/ega/test/data/EGAD00001000045/delimited_maps/Sample_File.map"),
            new File("/tmp/submission/ega/test/data/EGAD00001000083/delimited_maps/Sample_File.map")
        )
      );


      List<Pair<String, String>> data = pairs.toList().toBlocking().single();

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
  public void test_buildCache(){
    EGAFileAccessionValidator validator = new EGAFileAccessionValidator();
    try {

      File tmpDir = new File("/tmp/submission/ega");
      if (tmpDir.exists())
        FileUtils.deleteDirectory(tmpDir);

      Method method = EGAFileAccessionValidator.class.getDeclaredMethod("buildCache", Observable.class);
      method.setAccessible(true);

      Cache<String, Set<String>> cache = (Cache<String, Set<String>>)method.invoke(
          validator,
          loadSampleData()
        );

      Assert.assertNotNull(cache.getIfPresent("168-02-8TR"));
      Assert.assertTrue(cache.getIfPresent("168-02-8TR").contains("EGAF00000143419"));
      Assert.assertTrue(cache.getIfPresent("168-02-8TR").contains("EGAF00000143420"));

      Assert.assertNotNull(cache.getIfPresent("PD7436c-sc-2013-08-02T02:01:54Z-1674523"));
      Assert.assertTrue(cache.getIfPresent("PD7436c-sc-2013-08-02T02:01:54Z-1674523").contains("EGAF00000406990"));

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

  private Observable<Pair<String, String>> loadSampleData(){

    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(EGAFileAccessionValidatorTestWithoutScheduling.class.getResourceAsStream("/fixtures/validation/accession/ega/Sample_file_45.map")));
      List<Pair<String, String>> data = new ArrayList<>();

      String line;
      while((line = br.readLine()) != null){
        List<String> fields = Splitter.on(CharMatcher.BREAKING_WHITESPACE).omitEmptyStrings().trimResults().splitToList(line);
        data.add(Pair.of(fields.get(0), fields.get(3)));
      }
      br.close();

      br = new BufferedReader(new InputStreamReader(EGAFileAccessionValidatorTestWithoutScheduling.class.getResourceAsStream("/fixtures/validation/accession/ega/Sample_file_83.map")));
      while((line = br.readLine()) != null){
        List<String> fields = Splitter.on(CharMatcher.BREAKING_WHITESPACE).omitEmptyStrings().trimResults().splitToList(line);
        data.add(Pair.of(fields.get(0), fields.get(3)));
      }
      br.close();

      return Observable.from(data);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Observable.empty();
  }

  @Test
  public void test_initializeCache(){
    EGAFileAccessionValidator validator = new EGAFileAccessionValidator("ftp://admin:admin@localhost:"+ defaultFtpPort + "/ICGC_metadata", "0 0 9,21 * * ?");
    try {
      Method method = EGAFileAccessionValidator.class.getDeclaredMethod("initializeCache");
      method.setAccessible(true);

      Cache<String, Set<String>> cache = (Cache<String, Set<String>>)method.invoke(validator);

      Assert.assertNotNull(cache.getIfPresent("168-02-8TR"));
      Assert.assertTrue(cache.getIfPresent("168-02-8TR").contains("EGAF00000143419"));
      Assert.assertTrue(cache.getIfPresent("168-02-8TR").contains("EGAF00000143420"));

      Assert.assertNotNull(cache.getIfPresent("PD7436c-sc-2013-08-02T02:01:54Z-1674523"));
      Assert.assertTrue(cache.getIfPresent("PD7436c-sc-2013-08-02T02:01:54Z-1674523").contains("EGAF00000406990"));

    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}
