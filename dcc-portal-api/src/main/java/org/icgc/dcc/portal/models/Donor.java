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

public final class Donor {

  public static final String NAME = "donor";

  public static final String[] FIELDS = //
      { //
      "_donor_id", //
          "_project_id", //
          "donor_sex", //
          "donor_survival_time", //
          "donor_vital_status", //
          "donor_age_at_diagnosis", //
          "donor_age_at_enrollment", //
          "donor_age_at_last_followup", //
          "donor_diagnosis_icd10", //
          "donor_interval_of_last_followup", //
          "disease_status_last_followup", //
          "donor_tumour_stage_at_diagnosis", //
          "donor_tumour_staging_system_at_diagnosis", //
          "donor_tumour_stage_at_diagnosis_supplemental", //
          "donor_relapse_type", //
          "donor_relapse_interval", //
          "_summary._ssm_count", //
          "_summary._cnsm_count", //
          "_summary._sgv_count", //
          "_summary._stsm_count", //
          "_summary._exp_exists", //
          "_summary._jcn_exists", //
          "_summary._meth_exists", //
          "_summary._mirna_exists" //
      };

  public static final String INDEX = DataPortalConfiguration.INDEX_NAME;

  public static final String TYPE = "donor-centric";

  public static final ImmutableMap<String, ImmutableList<String>> FACETS = //
      ImmutableMap.of(//
          "terms", ImmutableList.of( //
              "donor_sex", //
              "donor_tumour_stage_at_diagnosis", //
              "donor_vital_status", //
              "disease_status_last_followup", //
              "donor_relapse_type") //
          );

  public static final ImmutableMap<String, ImmutableList<String>> FILTERS = //
      ImmutableMap.of( //
          "terms", ImmutableList.of( //
              "donor_sex", //
              "donor_tumour_stage_at_diagnosis", //
              "donor_vital_status", //
              "disease_status_last_followup", //
              "donor_relapse_type" //
          ), //
          "ranges", ImmutableList.of( //
              "donor_age_at_diagnosis", //
              "donor_survival_time", //
              "donor_relapse_interval" //
          ), //
          "locations", ImmutableList.of( //
              "" //
              ) //
          );
}
