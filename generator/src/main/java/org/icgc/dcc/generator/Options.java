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
package org.icgc.dcc.generator;

import lombok.ToString;

import com.beust.jcommander.Parameter;

/**
 * Command line options.
 * 
 * @author btiernay
 */
@ToString
public class Options {

  @Parameter(names = { "-j", "--jurisdiction" }, help = true, description = "Jurisdiction leading the project")
  public String leadJurisdiction;

  @Parameter(names = { "-t", "--tumour" }, help = true, description = "Tumour type")
  public String tumourType;

  @Parameter(names = { "-i", "--institution" }, help = true, description = "Institution submitting the data")
  public String Institution;

  @Parameter(names = { "-p", "--platform" }, help = true, description = "Platform or technology used in the analysis")
  public String platform;

  @Parameter(names = { "-r", "--rows" }, help = true, description = "Number of donors/rows")
  public int rows;

  @Parameter(names = { "-n", "--tempNumber" }, help = true, description = "Number of template files")
  public int numberOfTemplateFiles;

  @Parameter(names = { "-t", "--template" }, help = true, description = "Name of template file")
  public int templateName;

  @Parameter(names = { "-e", "--expFiles" }, help = true, description = "Experimental Files")
  public String experimentalFile;

  @Parameter(names = { "-v", "--version" }, help = true, description = "Show version")
  public boolean version;

  @Parameter(names = { "-h", "--help" }, help = true, description = "Show help information")
  public boolean help;

}
