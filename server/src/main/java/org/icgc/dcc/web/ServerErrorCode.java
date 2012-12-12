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
package org.icgc.dcc.web;

/**
 * Represents server error types.
 */
public enum ServerErrorCode { // TODO: migrate all (DCC-660)
  UNAUTHORIZED("Unauthorized"), //
  NO_SUCH_ENTITY("NoSuchEntity"), //
  NO_DATA("NoData"), //
  NAME_MISMATCH("NameMismatch"), //
  ALREADY_EXISTS("AlreadyExists"), //
  RESOURCE_CLOSED("ResourceClosed"), //
  ALREADY_INITIALIZED("AlreadyInitialized"), //
  MISSING_REQUIRED_DATA("MissingRequiredData"), //
  EMPTY_REQUEST("EmptyRequest"), //
  INVALID_NAME("InvalidName"), //
  RELEASE_EXCEPTION("ReleaseException"), //
  ;

  private String code;

  private ServerErrorCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
