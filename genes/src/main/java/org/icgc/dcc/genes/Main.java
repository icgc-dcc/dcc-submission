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
package org.icgc.dcc.genes;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.genes.cli.Options;
import org.icgc.dcc.genes.service.GenesService;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Command line utility used to import Heliotrope genes.bson {@code mongodump} file into DCC's MongoDB gene database.
 */
@Slf4j
public class Main {

  private final Options options = new Options();

  public static void main(String... args) {
    new Main().run(args);
  }

  private void run(String... args) {
    JCommander cli = new JCommander(options);
    cli.setProgramName(getProgramName());

    try {
      cli.parse(args);

      if(options.help) {
        cli.usage();

        return;
      } else if(options.version) {
        out.printf("ICGC DCC Gene Loader%nVersion %s%n", getVersion());

        return;
      }

      load();
    } catch(ParameterException pe) {
      err.printf("dcc-genes: %s%n", pe.getMessage());
      err.printf("Try '%s --help' for more information.%n", getProgramName());
    }
  }

  private void load() {
    GenesService loader = new GenesService(options.mongoUri);

    log.info("Loading gene model using: {}", options);
    loader.load(options.file);
    log.info("Finished loading!");
  }

  private String getProgramName() {
    return "java -jar " + getJarName();
  }

  private String getVersion() {
    String version = getClass().getPackage().getImplementationVersion();

    return version == null ? "" : version;
  }

  private String getJarName() {
    String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    File jarFile = new File(jarPath);

    return jarFile.getName();
  }

}
