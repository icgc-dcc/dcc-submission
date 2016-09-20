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
package org.icgc.dcc.submission.loader.file.export;

import static org.icgc.dcc.submission.loader.util.HdfsFiles.getCompressionAgnosticBufferedReader;

import java.io.File;

import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.submission.loader.core.DependencyFactory;
import org.icgc.dcc.submission.loader.file.AbstractFileLoaderFactory;
import org.icgc.dcc.submission.loader.file.FileLoader;
import org.icgc.dcc.submission.loader.model.FileTypePath;
import org.icgc.dcc.submission.loader.record.ExportRecordConverter;
import org.icgc.dcc.submission.loader.record.RecordReader;

import com.google.common.base.Optional;

@RequiredArgsConstructor
public class ExportFileLoaderFactory extends AbstractFileLoaderFactory {

  private final String outputDirectory;

  @Override
  public FileLoader createFileLoader(String project, String release, FileTypePath fileType) {
    val file = fileType.getPath();
    val outputFileName = resolveOutputFileName(file, project);
    val recordReader = new RecordReader(getCompressionAgnosticBufferedReader(file));
    val type = fileType.getType();
    val dictionary = DependencyFactory.getInstance().getDictionaryResolver().apply(Optional.absent());
    val converter = new ExportRecordConverter(dictionary, type);

    return new ExportFileLoader(project, type, recordReader, outputFileName, converter);
  }

  private String resolveOutputFileName(Path file, String project) {
    val fileName = file.getName();
    val parts = Splitters.DOT.splitToList(fileName);
    val extension = parts.get(parts.size() - 1);
    val name = extension.equals("gz") || extension.equals("bz2") ?
        Joiners.DOT.join(parts.subList(0, parts.size() - 1)) :
        fileName;
    val projectDir = outputDirectory + "/" + project;
    createProjectDir(projectDir);

    return projectDir + "/" + name + ".gz";
  }

  private synchronized void createProjectDir(String projectDirName) {
    val projectDir = new File(projectDirName);
    if (!projectDir.exists()) {
      projectDir.mkdir();
    }
  }

}
