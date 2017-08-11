package org.icgc.dcc.submission.ega.metadata.download.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.icgc.dcc.submission.ega.metadata.download.EGAMetadataDownloader;

import java.io.*;
import java.util.Optional;

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
@RequiredArgsConstructor
@Slf4j
public class ShellScriptDownloader implements EGAMetadataDownloader {

  @NonNull
  private String ftp_connection;
  @NonNull
  private String tmp_data_directory;
  @NonNull
  private String script_file_path;

  @Override
  public Optional<File> download() {

    File dataDir = new File(tmp_data_directory);
    if (dataDir.exists()){
      try {
        FileUtils.deleteDirectory(dataDir);
      } catch (IOException e) {
        return Optional.empty();
      }
    }

    if(dataDir.mkdirs() == false)
      return Optional.empty();

    try {
      FileOutputStream fos = new FileOutputStream(new File(tmp_data_directory + "/download_ega_metadata.sh"));
      InputStream is = ShellScriptDownloader.class.getResourceAsStream(script_file_path);
      byte[] buffer = new byte[512];
      int len = 0;
      while((len = is.read(buffer)) > 0){
        fos.write(buffer, 0, len);
      }
      fos.flush();
      fos.close();
      is.close();
    } catch (FileNotFoundException e) {
      return Optional.empty();
    } catch (IOException e) {
      return Optional.empty();
    }

    try {
      Process proc = (new ProcessBuilder("sh", tmp_data_directory + "/download_ega_metadata.sh", tmp_data_directory, ftp_connection)).start();

      Thread download_monitor_thread = new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line = br.readLine()) != null){
              log.info(line);
            }
          } catch (IOException e) {

          }
        }
      });
      download_monitor_thread.start();

      int exitVal = proc.waitFor();
      if(exitVal != 0)
        return Optional.empty();

    } catch (IOException e) {
      return Optional.empty();
    } catch (InterruptedException e) {
      return Optional.empty();
    }

    return Optional.of(dataDir);

  }

}
