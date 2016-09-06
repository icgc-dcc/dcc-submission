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
package org.icgc.dcc.submission.validation.pcawg.util;

import static com.google.common.io.Resources.readLines;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icgc.dcc.common.core.json.Jackson.formatPrettyJson;
import static org.icgc.dcc.common.core.model.Programs.isTCGA;
import static org.icgc.dcc.common.core.util.Splitters.TAB;
import static org.icgc.dcc.common.core.util.URLs.getUrl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import org.icgc.dcc.common.tcga.core.TCGAMappings;
import org.icgc.dcc.common.tcga.reader.TCGAMappingsReader;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSample;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility used to download, convert and store sample PCAWG sample sheet information.
 * <p>
 * For use with PCAWG validation.
 */
@Slf4j
public class PCAWGSampleSheets {

  /**
   * Constants.
   */
  public static final URL SAMPLE_SHEET_URL =
      getUrl("http://pancancer.info/data_releases/latest/pcawg_sample_sheet.tsv");

  private static final File SAMPLE_SHEET_FILE = new File("src/main/resources/pcawg-sample-sheet.json");

  /**
   * Utility to read, map, convert and write sample sheet.
   */
  @SneakyThrows
  public static void main(String[] args) {
    val parser = new PCAWGSampleSheetParser(SAMPLE_SHEET_URL);
    val mapper = new PCAWGSampleSheetMapper();
    val writer = new PCAWGSampleSheetWriter(SAMPLE_SHEET_FILE);

    val sheet = Lists.<PCAWGSample> newArrayList();
    parser.parse((values) -> {
      PCAWGSample sample = mapper.map(values);
      log.info("{}", sample);
      sheet.add(sample);
    });

    writer.write(sheet);
  }

  @RequiredArgsConstructor
  private static class PCAWGSampleSheetParser {

    /**
     * Configuration.
     */
    @NonNull
    private final URL url;

    @SneakyThrows
    public void parse(@NonNull Consumer<List<String>> consumer) {
      val lines = readLines(url, UTF_8);
      for (int i = 1 /* Skip header */; i < lines.size(); i++) {
        val line = lines.get(i);
        val values = TAB.splitToList(line);

        consumer.accept(values);
      }
    }

  }

  private static class PCAWGSampleSheetMapper {

    /**
     * State.
     */
    private final TCGAMappings mappings = new TCGAMappingsReader().readMappings();

    public PCAWGSample map(List<String> values) {
      val sample = new PCAWGSample();

      val projectKey = values.get(3).trim();

      String donorId = values.get(1).trim();
      String specimenId = values.get(5).trim();
      String sampleId = values.get(7).trim();

      // ICGC DCC uses barcodes
      if (isTCGA(projectKey)) {
        donorId = getBarcode(donorId);
        specimenId = getBarcode(specimenId);
        sampleId = getBarcode(sampleId);
      }

      // Temporary until additional QC is done upstream. Need to align with our CV terms
      String specimenType = values.get(9).trim();
      if (specimenType.equals("peripheral blood|Primary tumour - blood derived (peripheral blood)")) {
        specimenType = "Primary tumour - blood derived (peripheral blood)";
      }
      if (specimenType.equals("Primary tumour")) {
        specimenType = "Primary tumour - solid tissue";
      }

      return sample
          .setProjectKey(projectKey)
          .setDonorId(donorId)
          .setSpecimenId(specimenId)
          .setSpecimenType(specimenType)
          .setSampleId(sampleId);
    }

    private String getBarcode(String uuid) {
      return mappings.getBarcode(uuid);
    }

  }

  @RequiredArgsConstructor
  private static class PCAWGSampleSheetWriter {

    /**
     * Configuration.
     */
    @NonNull
    private final File file;

    public void write(List<PCAWGSample> sheet) throws IOException {
      Files.write(formatPrettyJson(sheet), file, UTF_8);
    }

  }

}
