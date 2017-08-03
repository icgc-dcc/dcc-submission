package org.icgc.dcc.submission.validation.accession.ega;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.*;

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
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ShellScriptDownloaderTest.class,
    EGAFileAccessionValidatorTest.class
})
public class EGAFileAccessionValidatorTestSuite {
  private static FtpServer ftpServer = null;
  private static String adminHomePath = "/tmp/submission/ega/test";
  private static File adminHomeDir = new File(adminHomePath);
  private static String dataPath = adminHomePath + "/ICGC_metadata";
  public static int defaultFtpPort = 2222;
  @BeforeClass
  public static void initialize() {

    try {
      if(adminHomeDir.exists())
        FileUtils.deleteDirectory(adminHomeDir);

      adminHomeDir.mkdirs();
      (new File(dataPath)).mkdir();

      copyFileFromClasspathToTmpDir("/fixtures/validation/accession/ftp", "users.properties", adminHomePath);
      copyFileFromClasspathToTmpDir("/fixtures/validation/accession/ftp", "EGAD00001000045.tar.gz", dataPath);
      copyFileFromClasspathToTmpDir("/fixtures/validation/accession/ftp", "EGAD00001000083.tar.gz", dataPath);

      FtpServerFactory serverFactory = new FtpServerFactory();

      PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
      userManagerFactory.setFile(new File(adminHomeDir, "users.properties"));

      serverFactory.setUserManager(userManagerFactory.createUserManager());

      ListenerFactory listenerFactory = new ListenerFactory();
      listenerFactory.setPort(defaultFtpPort);

      serverFactory.addListener("default", listenerFactory.createListener());

      ftpServer = serverFactory.createServer();
      ftpServer.start();

    } catch (IOException e) {
      e.printStackTrace();
    } catch (FtpException e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void destroy(){
    ftpServer.stop();

    try {
      FileUtils.deleteDirectory(adminHomeDir);
      FileUtils.deleteDirectory(new File("/tmp/submission"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void copyFileFromClasspathToTmpDir(String classpath, String fileName, String targetPath){
    try {

      File target = new File(targetPath + "/" + fileName);
      target.createNewFile();

      FileOutputStream fos = new FileOutputStream(target);
      InputStream is = ShellScriptDownloaderTest.class.getResourceAsStream(classpath + "/" + fileName);
      byte[] buffer = new byte[512];
      int len = 0;
      while((len = is.read(buffer)) > 0){
        fos.write(buffer, 0, len);
      }
      fos.flush();
      fos.close();
      is.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
