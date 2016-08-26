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
package org.icgc.dcc.submission.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.icgc.dcc.submission.core.model.Status;
import org.icgc.dcc.submission.service.SystemService;
import org.icgc.dcc.submission.web.controller.SystemController;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.google.common.util.concurrent.Service.State;

@WebMvcTest(SystemController.class)
public class SystemControllerTest extends ControllerTest {

  /**
   * Test collaborators.
   */
  @MockBean
  private SystemService systemService;

  @Test
  public void testGetStatus() throws Exception {
    when(systemService.isEnabled()).thenReturn(true);
    when(systemService.getStatus()).thenReturn(new Status(true, State.RUNNING));

    mvc
        .perform(
            get("/ws/systems")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content()
            .string("{\"sftpEnabled\":true,\"sftpState\":\"RUNNING\",\"activeSftpSessions\":0,\"userSessions\":[]}"));
  }

  @Test
  public void testDisableSftp() throws Exception {
    when(systemService.getStatus()).thenReturn(new Status(false, State.RUNNING));

    mvc
        .perform(
            patch("/ws/systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content()
            .string("{\"sftpEnabled\":false,\"sftpState\":\"RUNNING\",\"activeSftpSessions\":0,\"userSessions\":[]}"));
  }

  @Test
  public void testEnableSftp() throws Exception {
    when(systemService.getStatus()).thenReturn(new Status(true, State.RUNNING));

    mvc
        .perform(
            patch("/ws/systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content()
            .string("{\"sftpEnabled\":true,\"sftpState\":\"RUNNING\",\"activeSftpSessions\":0,\"userSessions\":[]}"));

  }

  @Test
  public void testInvalidSftpPatch() throws Exception {
    mvc
        .perform(
            patch("/ws/systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("{\"code\":\"MissingRequiredData\",\"parameters\":[]}"));
  }

}
