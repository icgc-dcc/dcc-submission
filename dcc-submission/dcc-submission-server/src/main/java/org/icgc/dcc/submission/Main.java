/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.icgc.dcc.submission.config.ConfigModule;
import org.icgc.dcc.submission.core.CoreModule;
import org.icgc.dcc.submission.core.DccRuntime;
import org.icgc.dcc.submission.core.morphia.MorphiaModule;
import org.icgc.dcc.submission.dictionary.DictionaryModule;
import org.icgc.dcc.submission.fs.FileSystemModule;
import org.icgc.dcc.submission.http.HttpModule;
import org.icgc.dcc.submission.http.jersey.JerseyModule;
import org.icgc.dcc.submission.release.ReleaseModule;
import org.icgc.dcc.submission.sftp.SftpModule;
import org.icgc.dcc.submission.shiro.ShiroModule;
import org.icgc.dcc.submission.validation.ValidationModule;
import org.icgc.dcc.submission.web.WebModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static final String HADOOP_USER_NAME_PARAM = "HADOOP_USER_NAME";

  private static final String HADOOP_USER_NAME = "hdfs";

  private static enum CONFIG {
    qa("application_qa"), dev("application_dev"), local("application"), external(null);

    String filename;

    private CONFIG(String filename) {
      this.filename = filename;
    }

    public static String listValues() {
      return Arrays.asList(CONFIG.values()).toString();
    }
  };

  private static Injector injector;

  public static void main(String[] args) throws IOException {

    Config parsedConfig = loadConfig(args);

    System.setProperty(HADOOP_USER_NAME_PARAM, HADOOP_USER_NAME); // see DCC-572
    Main.injector = Guice.createInjector(new ConfigModule(parsedConfig) //
        // Infrastructure modules
        , new CoreModule()//
        , new HttpModule()//
        , new JerseyModule()//
        , new WebModule()//
        , new MorphiaModule()//
        , new ShiroModule()//
        , new FileSystemModule()//
        , new SftpModule()//

        // Business modules
        , new DictionaryModule()//
        , new ReleaseModule()//
        , new ValidationModule());

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        // No one call shutdown?
        boolean running = injector != null;
        if(running) {
          injector.getInstance(DccRuntime.class).stop();
        }
      }

    }, "Shutdown-thread"));

    injector.getInstance(DccRuntime.class).start();
  }

  private static Config loadConfig(String[] args) throws FileNotFoundException {
    CONFIG configType;
    try {
      configType = (args != null && args.length > 0) ? CONFIG.valueOf(args[0]) : CONFIG.local;
    } catch(IllegalArgumentException e) {
      throw new IllegalArgumentException(args[0] + " is not a valid argument. Valid arguments are "
          + CONFIG.listValues());
    }
    log.info("Using config type {}", configType);
    Config parsedConfig;
    if(configType == CONFIG.external) {
      if(args.length < 2) {
        throw new IllegalArgumentException("The argument 'external' requires a filename as an additional parameter");
      }
      File configFile = new File(args[1]);
      log.info("Using config file {}", configFile.getAbsoluteFile());
      if(configFile.exists() == false) {
        throw new FileNotFoundException(args[1]);
      }
      parsedConfig = ConfigFactory.parseFile(configFile).resolve();
    } else {
      parsedConfig = ConfigFactory.load(configType.filename);
    }
    return parsedConfig;
  }

  /**
   * This method is required for testing since shutdown hooks are not invoked between tests.
   */
  @VisibleForTesting
  public static void shutdown() {
    injector.getInstance(DccRuntime.class).stop();
    injector = null;
  }

}
