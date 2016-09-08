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
package org.icgc.dcc.submission.server.sftp;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.io.File;
import java.util.Collection;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.server.service.SystemService;

@NoArgsConstructor(access = PRIVATE)
public final class UserSessions {

  public static Collection<String> getTransferFiles(SystemService systemService, String projectKey) {
    return systemService.getFileTransfers().stream()
        .filter(transferFile -> isProjectTransfer(projectKey, transferFile))
        .map(transferFile -> getTransferFileName(transferFile))
        .collect(toImmutableList());
  }

  private static String getTransferFileName(String transferFile) {
    val fileName = new File(transferFile).getName();
    checkState(!isNullOrEmpty(fileName), "Failed to resolve transfer file name from path '{}'", transferFile);

    return fileName;
  }

  private static boolean isProjectTransfer(String projectKey, String transferFile) {
    val transferProject = new File(new File(transferFile).getParent()).getName();

    return projectKey.equals(transferProject);
  }

}
