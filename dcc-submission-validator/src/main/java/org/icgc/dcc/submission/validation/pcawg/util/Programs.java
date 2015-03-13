/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS AS IS AND ANY                           
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

import lombok.experimental.UtilityClass;

// TODO: Move to dcc-common-core
@UtilityClass
public class Programs {

  public static boolean isTCGA(String projectName) {
    try {
      TCGA.valueOfProjectName(projectName);

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public enum TCGA {

    BLCA_US,
    BRCA_US,
    CESC_US,
    COAD_US,
    DBLC_US, // Non-DCC
    GBM_US,
    HNSC_US,
    KICH_US, // Non-DCC
    KIRC_US,
    KIRP_US,
    LAML_US,
    LGG_US,
    LIHC_US,
    LUAD_US,
    LUSC_US,
    OV_US,
    PAAD_US,
    PRAD_US,
    READ_US,
    SKCM_US,
    STAD_US,
    THCA_US,
    UCEC_US;

    public String getProjectName() {
      return name().replace("_", "-");
    }

    public static TCGA valueOfProjectName(String projectName) {
      return valueOf(projectName.replace("-_", "_"));
    }

  }

}
