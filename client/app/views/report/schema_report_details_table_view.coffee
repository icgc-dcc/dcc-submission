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


DataTableView = require 'views/base/data_table_view'

module.exports = class SchemaReportDetailsTableView extends DataTableView
  template: template
  template = null

  container: "#schema-report-container"
  containerMethod: 'html'

  autoRender: true

  initialize: ->
    console.debug "SchemaReportDetailsTableView#initialize", @collection
    super

    @modelBind 'change', @update

  update: ->
    console.debug "SchemaReportDetailsTableView#update", @collection
    @updateDataTable()

  summaryDetails: (data) ->
    console.debug "ReportTableView#summaryDetails", data
    type = switch data.type
      when "AVERAGE" then "Statistics"
      when "FREQUENCY" then "Value Frequencies (value:count)"

    sOut = "<dt>#{type}</dt>"
    for key, value of data.summary
      value =
        if key in ['stddev','avg']
          Number(value).toFixed(2)
        else
          value
      sOut += "<dd><strong>#{key}&nbsp;:&nbsp;</strong>#{value}<br></dd>"

    sOut

  createDataTable: ->
    console.debug "SchemaReportDetailsTableView#createDataTable",
      @$el, @collection
    aoColumns = [
        { sTitle: "Column Name", mData: "name"}
        { sTitle: "Percentage of populated rows", mData: "completeness"}
        { sTitle: "Number of populated rows", mData: "populated"}
        {
          sTitle: "Number of rows with missing values"
          mData: "missing"
        }
        { sTitle: "Number of rows with nulls", mData: "nulls"}
        {
          sTitle: "Summary"
          mData: (source) =>
            @summaryDetails source
        }
      ]

    @$el.dataTable
      sDom:
        "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
      bPaginate: false
      oLanguage:
        "sLengthMenu": "_MENU_ submissions per page"
      aaSorting: [[ 1, "desc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""

      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @collection.toJSON()
