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
package org.icgc.dcc.genes;

import java.io.File;

import lombok.ToString;

import org.icgc.dcc.genes.cli.FileValidator;
import org.icgc.dcc.genes.cli.MongoURIConverter;
import org.icgc.dcc.genes.cli.MongoURIValidator;
import org.icgc.dcc.genes.cli.MongoValidator;

import com.beust.jcommander.Parameter;
import com.mongodb.MongoURI;

/**
 * Command line options.
 * 
 * @author btiernay
 */
@ToString
public class Options {

  @Parameter(names = { "-f", "--file" }, required = true, validateValueWith = FileValidator.class, description = "Heliotrope genes.bson mongodump file (e.g. genes.bson)")
  public File file;

  @Parameter(names = { "-d", "--database" }, required = true, converter = MongoURIConverter.class, validateWith = MongoURIValidator.class, validateValueWith = MongoValidator.class, description = "DCC mongo database uri (e.g. mongodb://localhost/dcc-genes.Genes)")
  public MongoURI mongoUri;

  @Parameter(names = { "-v", "--version" }, help = true, description = "Show version information")
  public boolean version;

  @Parameter(names = { "-h", "--help" }, help = true, description = "Show help information")
  public boolean help;

}