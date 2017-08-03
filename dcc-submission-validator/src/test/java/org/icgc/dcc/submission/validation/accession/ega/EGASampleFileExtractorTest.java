package org.icgc.dcc.submission.validation.accession.ega;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.validation.accession.ega.extractor.impl.EGASampleFileExtractor;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

public class EGASampleFileExtractorTest {

  private static String target_path = "/tmp/submission/ega/test";
  private static File targeDir = new File(target_path);
  private static String data_file_source_path = "/fixtures/validation/accession/ega";
  private static String data_file_name = "Sample_File_45.map";


  @BeforeClass
  public static void initialize() {

      try {

        if(targeDir.exists())
          FileUtils.deleteDirectory(targeDir);

        targeDir.mkdirs();

        FileOperationHelper.copyFileFromClasspathToTmpDir(data_file_source_path, data_file_name, target_path);

      } catch (IOException e) {
        e.printStackTrace();
      }


  }

  @AfterClass
  public static void tearDown() {

      try {
        if(targeDir.exists()) {
          FileUtils.deleteDirectory(targeDir);
          FileUtils.deleteDirectory(new File("/tmp/submission"));
        }


      } catch (IOException e) {
        e.printStackTrace();
      }
  }

  @Test
  public void test_extract(){
    EGASampleFileExtractor extractor = new EGASampleFileExtractor();

    Observable<Pair<String, String>> output = extractor.extract(new File(targeDir, data_file_name));

    Map<String, String> data = output.toMap(pair->pair.getKey(), pair->pair.getValue()).toBlocking().single();

    assertEquals( data.get("PD12852b-sc-2013-08-02T01:56:51Z-1674528"), "EGAF00000406986");
    assertEquals( data.get("PD9179a-sc-1927644"), "EGAF00000604559");
  }
}
