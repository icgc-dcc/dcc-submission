/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.models;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class Donor {
  public static final String NAME = "donor";

  public static final String[] FIELDS = {"donor_id", "primary_site", "project_name", "project_key", "donor_sex",
      "donor_age_at_diagnosis", "donor_tumour_stage_at_diagnosis", "has_ssm", "has_cnsm", "has_exp", "has_meth"};

  public static final String INDEX = "icgc_demo";

  public static final String TYPE = "donors";

  public static final ImmutableMap<String, ImmutableList<String>> FACETS = ImmutableMap.of("terms",
      ImmutableList.of("project_name", "donor_sex", "primary_site", "donor_tumour_stage_at_diagnosis"));

  public static final ImmutableMap<String, ImmutableList<String>> FILTERS = ImmutableMap.of("terms",
      ImmutableList.of("project_name", "donor_sex", "donor_tumour_stage_at_diagnosis"), "ranges",
      ImmutableList.of("age_at_diagnosis", "survival_time", "donor_release_interval"), "locations",
      ImmutableList.of(""));
}
