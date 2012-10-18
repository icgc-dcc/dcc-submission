"""
* Copyright 2012(c) The Ontario Institute for Cancer Research.
* All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the GNU Public License v3.0.
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
"""


View = require 'views/base/page_view'
SchemaReportErrorTableView =
  require 'views/report/schema_report_error_table_view'
SchemaReportDetailsTableView =
  require 'views/report/schema_report_details_table_view'
template = require 'views/templates/report/schema_report'

module.exports = class SchemaReportPageView extends View
  template: template
  template = null

  container: '#page-container'
  autoRender: no
  id: 'schema-report-view'

  initialize: ->
    console.log "SchemaReportPageView#initialize", @model
    super

  render: ->
    console.debug "SchemaReportPageView#render", @model
    super

    if @model.get("errors").length
      @subview('SchemaReportTable'
        new SchemaReportErrorTableView {
          collection: @model.get "errors"
          el: @.$("#schema-report-container")
        }
      )
    else
      @subview('SchemaReportTable'
        new SchemaReportDetailsTableView {
          collection: @model.get "fieldReports"
          el: @.$("#schema-report-container")
        }
      )