"""
* Copyright 2016(c) The Ontario Institute for Cancer Research.
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

module.exports = class SchemaReportSummaryTableView extends DataTableView
  template: template
  template = null

  container: "#schema-report-container"
  containerMethod: 'html'

  autoRender: true

  initialize: ->
    console.log @collection
    super

    @modelBind 'change', @update

  update: ->
    @updateDataTable()


  codeTable:
    TOTAL_START:
      """
      Total number of submitted SSMs
      """
    TOTAL_END:
      """
      Number of observations at the end of the
      normalization process
      """
    UNIQUE_START:
      """
      Number of unique analysis before filtering
      """
    DROPPED:
      """
      Number of observations dropped due to redundancy
      """
    UNIQUE_FILTERED:
      """
      Number of unique analysis remaining afer filtering
      """
    MARKED_AS_CONTROLLED:
      """
      Number of SSMs need to be masked for open access
      """
    MASKED:
      """
      Number of observations for which the sensitive data
      has been masked
      """
    RATIO:
      """
      Percentage
      """

  createDataTable: ->
    aoColumns = [
        {
          sTitle: "Description"
          mData: (source) =>
            if @codeTable[source.name]
              @codeTable[source.name]
            else
              source.name
        }
        {
          sTitle: "Value"
          mData: (source) ->
            if source.name in ["RATIO"]
              parseFloat(100 * source.value).toFixed(2) + "%"
            else
              source.value
        }
      ]

    @$el.dataTable
      sDom:
        "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
      bPaginate: false
      oLanguage:
        "sLengthMenu": "_MENU_ submissions per page"
      aaSorting: []
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""

      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @collection.toJSON()
