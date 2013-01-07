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
package org.icgc.dcc.data.model;

import java.util.Date;
import java.util.List;

import org.icgc.dcc.core.model.Timestamped;
import org.icgc.dcc.data.web.HasKey;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.common.base.Objects;

/**
 * 
 */
@Entity
public class Donor extends Timestamped implements HasKey {

  @Id
  public String id;

  public String cancerType;

  public int gender;

  public int age;

  public String regionOfResidence;

  public int donorVitalStatus;

  public int donorSurvivalTime;

  public List<Specimen> specimens;

  public Donor() {
    this.created = new Date();
    this.lastUpdate = new Date();
  }

  @Override
  public Object getKey() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof Donor == false) return false;
    Donor rhs = (Donor) obj;
    return id.equals(rhs.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
