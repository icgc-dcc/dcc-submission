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
package org.icgc.dcc.submission.validation.pcawg.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.validation.ValidationTests.getTestProjectPath;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.hadoop.fs.FileSystems;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.ValidationTests;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ClinicalParserTest {

  private static final Path TEST_PROJECT_PATH = getTestProjectPath("project.3");

  @Mock
  ValidationContext context;

  @Test
  public void testParse() throws Exception {
    val dictionary = ValidationTests.getTestDictionary();

    when(context.getFileSystem()).thenReturn(FileSystems.getDefaultLocalFileSystem());

    val donorFile = mockProjectFile(dictionary, FileType.DONOR_TYPE, "donor.txt");
    val specimenFile = mockProjectFile(dictionary, FileType.SPECIMEN_TYPE, "specimen.txt");
    val sampleFile = mockProjectFile(dictionary, FileType.SAMPLE_TYPE, "sample.txt");

    mockProjectFile(dictionary, FileType.BIOMARKER_TYPE, null);
    mockProjectFile(dictionary, FileType.FAMILY_TYPE, null);
    mockProjectFile(dictionary, FileType.EXPOSURE_TYPE, null);
    mockProjectFile(dictionary, FileType.SURGERY_TYPE, null);
    mockProjectFile(dictionary, FileType.THERAPY_TYPE, null);

    val clinical = ClinicalParser.parse(context);

    assertThat(clinical.getCore().getDonors()).hasSize(countRows(donorFile));
    assertThat(clinical.getCore().getSpecimens()).hasSize(countRows(specimenFile));
    assertThat(clinical.getCore().getSamples()).hasSize(countRows(sampleFile));

    log.info("Clinical: {}", clinical);
  }

  private Path mockProjectFile(Dictionary dictionary, FileType fileType, String fileName) {
    val file = fileName != null ? new Path(TEST_PROJECT_PATH, fileName) : null;
    val files = fileName != null ? ImmutableList.of(file) : Collections.<Path> emptyList();
    val fileSchema = dictionary.getFileSchema(fileType);

    when(context.getFiles(fileType)).thenReturn(files);
    when(context.getFileSchema(fileType)).thenReturn(fileSchema);

    return file;
  }

  private static int countRows(Path path) {
    val headerLineCount = 1;

    return countLines(new File(path.toString())) - headerLineCount;
  }

  @SneakyThrows
  private static int countLines(File file) {
    return Files.readLines(file, StandardCharsets.UTF_8).size();
  }

}
