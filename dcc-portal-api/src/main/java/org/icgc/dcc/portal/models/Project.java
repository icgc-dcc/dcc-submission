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

public final class Project {
  public static final String NAME = "project";

  public static final String[] FIELDS = {"project_name", "project_key", "primary_site", "country", "total_donor_count",
      "ssm_tested_donor_count", "cnsm_tested_donor_count", "exp_tested_donor_count", "meth_tested_donor_count",
      "pubmed_id"};

  public static final String INDEX = "icgc_demo";

  public static final String TYPE = "projects";

  public static final ImmutableMap<String, ImmutableList<String>> FACETS = ImmutableMap.of("terms",
      ImmutableList.of("project_name", "primary_site", "country", "available_profiling_data"));

  public static final ImmutableMap<String, ImmutableList<String>> FILTERS = ImmutableMap.of("terms",
      ImmutableList.of("project_name", "project_key", "primary_site", "country", "available_profiling_data"), "ranges",
      ImmutableList.of(""), "locations", ImmutableList.of(""));
}
