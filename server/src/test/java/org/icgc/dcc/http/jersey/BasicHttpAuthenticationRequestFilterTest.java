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
package org.icgc.dcc.http.jersey;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.net.HttpHeaders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BasicHttpAuthenticationRequestFilterTest {

  private Request mockRequest;

  private MultivaluedMap<String, String> mockHeaders;

  private ContainerRequestContext mockContext;

  @Mock
  private UriInfo mockUriInfo;

  private BasicHttpAuthenticationRequestFilter basicHttpAuthenticationRequestFilter;

  private UsernamePasswordAuthenticator usernamePasswordAuthenticator;

  private static final String HTTP_AUTH_PREFIX = "X-DCC-Auth";

  private static final String WWW_AUTHENTICATE_REALM = "DCC";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {

    // Create a few mock instances
    this.mockRequest = mock(Request.class);
    this.mockHeaders = mock(MultivaluedMap.class);
    this.mockContext = mock(ContainerRequestContext.class);
    this.usernamePasswordAuthenticator = mock(UsernamePasswordAuthenticator.class);
    Subject mockSubject = mock(Subject.class);
    SecurityContext mockSecurityContext = mock(SecurityContext.class);

    this.basicHttpAuthenticationRequestFilter =
        new BasicHttpAuthenticationRequestFilter(this.usernamePasswordAuthenticator);

    // Create some behaviour
    when(this.mockContext.getRequest()).thenReturn(this.mockRequest);
    when(this.mockContext.getHeaders()).thenReturn(this.mockHeaders);
    when(this.mockContext.getSecurityContext()).thenReturn(mockSecurityContext);
    when(this.mockContext.getUriInfo()).thenReturn(mockUriInfo);
    when(mockUriInfo.getPath()).thenReturn("/fake/path");

    when(this.usernamePasswordAuthenticator.authenticate("brett", "brettspasswd".toCharArray(), "")).thenReturn(
        mockSubject);
  }

  @Test
  public void test_filter_handlesCorrectAuthorizationHeader() throws IOException {

    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION))//
        .thenReturn(String.format("%s YnJldHQ6YnJldHRzcGFzc3dk", HTTP_AUTH_PREFIX)); // encodes "brett:brettspasswd" in
                                                                                     // base64
    // (generate using: $ echo -n "brett:brettspasswd" | base64)
    this.runFilter();
    // Make sure there was a login attempt and capture the token
    // Assert username and password match
    verify(this.usernamePasswordAuthenticator).authenticate("brett", "brettspasswd".toCharArray(), "");
    verify(this.mockContext, Mockito.never()).abortWith(any(Response.class));
  }

  @Test
  public void test_filter_handlesIncorrectAuthorizationHeader() throws IOException {

    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION))//
        .thenReturn(String.format("%s YnJldHQ6Tk9UYnJldHRzcGFzc3dk", HTTP_AUTH_PREFIX)); // encodes "brett:brettspasswd"
                                                                                         // in base64
    // (generate using: $ echo -n "brett:NOTbrettspasswd" | base64)
    this.runFilter();

    verify(this.usernamePasswordAuthenticator).authenticate("brett", "NOTbrettspasswd".toCharArray(), "");

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(this.mockContext).abortWith(response.capture());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getValue().getStatus());
    assertEquals(String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM), response.getValue()
        .getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void test_filter_handlesMissingHeader() throws IOException {

    // This test is testing that the header is absent
    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    this.runFilter();

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(this.mockContext).abortWith(response.capture());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getValue().getStatus());
    assertEquals(String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM), response.getValue()
        .getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void test_filter_handlesMalformedAuthorizationHeader1() throws IOException {
    testMalformedHeader(String.format("%sX YnJldHQ6YnJldHRzcGFzc3dkCg==", HTTP_AUTH_PREFIX));
  }

  @Test
  public void test_filter_handlesMalformedAuthorizationHeader2() throws IOException {
    testMalformedHeader(String.format("%s YnJldHQvYnJldHRzcGFzc3dkCg==", HTTP_AUTH_PREFIX));
  }

  @Test
  public void test_filter_handlesMalformedAuthorizationHeader3() throws IOException {
    testMalformedHeader(HTTP_AUTH_PREFIX);
  }

  /**
   * Common test fixture for a malformed header
   */
  private void testMalformedHeader(String malformed) throws IOException {
    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(malformed);
    this.runFilter();

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(this.mockContext).abortWith(response.capture());
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getValue().getStatus());
  }

  // Exercise the code we're testing
  private void runFilter() throws IOException {
    this.basicHttpAuthenticationRequestFilter.filter(this.mockContext);
  }
}
