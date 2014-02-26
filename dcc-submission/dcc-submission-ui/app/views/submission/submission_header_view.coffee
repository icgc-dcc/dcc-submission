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


View = require 'views/base/view'
template = require 'views/templates/submission/submission_header'

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
    console.log "initing dataTable"
    report = @model.get "report"
    @dataTypeReports = report.dataTypeReports

    if not @dataTypeReports
      @dataTypeReports = []

    console.log "datatype reports", @dataTypeReports

    aoColumns = [
       {
          sTitle: "Data Type"
          mData: (source)->
            source.dataType
       }
       {
          sTitle: "State"
          mData: (source)->
            #source.dataTypeState
            state = source.dataTypeState.replace("_", " ")
            return switch state
              when "INVALID"
                "<span class='error'><i class='icon-remove-sign'></i> " + state + "</span>"
              when "VALID"
                "<span class='valid'><i class='icon-ok-sign'></i> " + state + "</span>"
              when "VALIDATING"
                "<span class='validating'><i class='icon-cogs'></i> " + state + "</span>"
              when "QUEUED"
                "<span class='queued'><i class='icon-time'></i> " + state + "</span>"
              when "ERROR"
                "<span class='error'>" + "<i class='icon-exclamation-sign'></i> " + state + "</span>"
              when "NOT VALIDATED"
                "<span><i class='icon-question-sign'></i> " + state + "</span>"
              else
                state

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

    #@dt = @$el.find("#datatype-summary").dataTable()



      #for dataType in dataTypeReports
      #  console.log "d", dataType
      #  @dt.fnAddData( dataType )

