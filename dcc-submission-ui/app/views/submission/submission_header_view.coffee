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


View = require 'views/base/view'
template = require 'views/templates/submission/submission_header'
utils = require 'lib/utils'

module.exports = class SubmissionHeaderView extends View
  template: template
  template = null

  autoRender: true

  initialize: ->
    @dataTypeReports = []
    @dt = null
    super

    @modelBind 'change', @render

  render: ->
    super
    report = @model.get "report"
    @dataTypeReports = report.dataTypeReports

    if not @dataTypeReports
      @dataTypeReports = []

    @dataTypeReports = _.sortBy @dataTypeReports, (dataType)->
      switch dataType.dataType
        when "CLINICAL_CORE_TYPE"
          return 0
        when "CLINICAL_SUPPLEMENTAL_TYPE"
          return 1
        else
          return 10



    aoColumns = [
       {
          sTitle: "Data Type"
          mData: (source)->
            utils.translateDataType(source.dataType)
       }
       {
          sTitle: "State"
          mData: (source)->
            return utils.getStateDisplay source.dataTypeState
       }
    ]

    @$el.find("#datatype-summary").dataTable
      sDom:
        "t"
        #"<'row-fluid'<'span6'><'span6'>r>t<'row-fluid'<'span6'><'span6'>>"
      bPaginate: false
      bSort: false
      #aaSorting: [[ 1, "asc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @dataTypeReports
      #fnDrawCallback:
      #  $(document).on "click", "#datatype-summary tbody tr", (e)->
      #    data = $("#datatype-summary").dataTable().fnGetData @
      #    scroll = $("##{data.dataType}_wrapper").offset().top
      #    console.log "scroll", scroll
      #    $("body").animate({ "scrollTop": scroll}, 1)

