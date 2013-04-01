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

import org.icgc.dcc.portal.DataPortalConfiguration;

public final class Project {
  public static final String NAME = "project";

  public static final String[] FIELDS = {"_release_id", "_project_id", "project_name", "project_key", "primary_site",
      "countries", "_summary._total_donor_count", "_summary._ssm_tested_donor_count",
      "_summary._cnsm_tested_donor_count", "_summary._exp_tested_donor_count", "_summary._meth_tested_donor_count",
      "pubmed_ids"};

  public static final String INDEX = DataPortalConfiguration.INDEX_NAME;

  public static final String TYPE = "projects";

  public static final ImmutableMap<String, ImmutableList<String>> FACETS = ImmutableMap.of("terms",
      ImmutableList.of("project_name", "primary_site", "countries", "_summary._available_data_type"));

  public static final ImmutableMap<String, ImmutableList<String>> FILTERS = ImmutableMap.of("terms", ImmutableList.of(
      "_project_id", "project_name", "project_key", "primary_site", "countries", "_summary._available_data_type"),
      "ranges", ImmutableList.of(""), "locations", ImmutableList.of(""));
}
