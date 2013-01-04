/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Base class for creating an integration test.
 * 
 * This class is responsible for setting up the reusable objects for establishing the integration test environment,
 * encapsulating the minimal set of required Guice module.
 */
public abstract class BaseIntegrationTest {

  static {
    setProperties();
  }

  /**
   * Sets key system properties before test initialization.
   */
  private static void setProperties() {
    // See http://stackoverflow.com/questions/7134723/hadoop-on-osx-unable-to-load-realm-info-from-scdynamicstore
    System.setProperty("java.security.krb5.realm", "OX.AC.UK");
    System.setProperty("java.security.krb5.kdc", "kdc0.ox.ac.uk:kdc1.ox.ac.uk");
  }

  protected Config config;

  protected Configuration conf;

  protected FileSystem fs;

  protected final Client client = ClientFactory.newClient();

  protected void setup(String fileName) throws IOException, URISyntaxException, InterruptedException {
    this.config = ConfigFactory.load(fileName);
    this.conf = new Configuration();
    this.fs = FileSystem.get(new URI(getFsUrl()), conf, "hdfs");
  }

  protected String getFsUrl() {
    return config.getString("fs.url");
  }

  protected String getRootDir() {
    return config.getString("fs.root");
  }

  protected String getMongoUri() {
    return config.getString("mongo.uri");
  }

}
