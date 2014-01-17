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

mediator = require 'mediator'
DataTableView = require 'views/base/data_table_view'
View = require 'views/base/view'
utils = require 'lib/utils'

module.exports = class ReportDatatypeView extends View
  template: template
  template = null

  autoRender: false
  #containerMethod: "append"

  initialize: ->
    #console.debug "ReportTableView#initialize", @model, @el
    @report = @model.get "report"
    @currentDatatypes = []

    @files = []
    console.log @files

    super

    @modelBind 'change', @update

  update: ->
    #console.debug "ReportTableView#update", @model
    console.log "ReportDatatypeView -> update"
    @report = @model.get "report"

    # Figure out all the table placements

    # Extract unique datatypes for current update
    datatypes = []
    @schemaReports = @report.get "schemaReports"
    @schemaReports.each (report)->
      console.log report
      console.log report.get "dataType"
      datatype = report.get "dataType"
      if datatypes.indexOf( datatype ) == -1
        if datatype == null
          datatype = "Others"
        datatypes.push report.get "dataType"

    console.log datatypes

    # Create data type tables if they do not exist
    # TODO: Sort based on data type
    # TODO: table headers
    # TODO: section by clinical and experimental
    # TODO: should clinical always be visible?
    container = @$el
    datatypes.forEach (datatype)=>
      elem = container.find("#"+datatype)
      if elem.length == 0
        container.append("<table id='#{datatype}'></table>")
        container.append("<br>")
        container.append("<br>")

        elem = container.find("##{datatype}")
        elem.addClass("report table table-striped table-bordered table-hover")
        @createDataTable(datatype)

    # Clean up (deletions)
    @currentDatatypes.forEach (datatype)->
      if datatypes.indexOf(datatype) == -1
        container.find("[id^="+datatype+"]").remove()
    @currentDatatypes = datatypes
        
    @updateDataTable()

  # Since we chop and dice the collection, we need to use a different update
  updateDataTable: ->
    console.log @model.get("dataState")
    dataState = @model.get("dataState")
    dataStateMap = {}
    dataState.forEach (ds)->
      dataStateMap[ds.dataType] = ds.state

    @currentDatatypes.forEach (datatype)=>
      @files = _.filter @report.get("schemaReports").toJSON(), (d)->
        return d.dataType == datatype
      dt = @$el.find("#"+datatype).dataTable()

      state = dataStateMap[datatype]
      if state and state in ["INVALID", "VALID", "SIGNED_OFF"]
        dt.fnSetColumnVis( 3, true )
        dt.fnSetColumnVis( 4, true )
      else
        state = ""

      dt.fnClearTable()
      dt.fnAddData @files

      title = utils.translateDataType(datatype)
      $(".#{datatype}_title").html("<strong>#{title} - #{state}</strong>")
 

  createDataTable: (datatype)->
    #console.debug "ReportTableView#createDataTable", @$el, @model.get "name"
    aoColumns = [
        {
          sTitle: "File"
          mData: "name"
        }
        {
          sTitle: "Last Updated"
          mData: (source) -> utils.date source.lastUpdate
          sType: "date"
        }
        {
          sTitle: "Size"
          bUseRendered: false
          mData: (source, type) ->
            if type is "display"
              return utils.fileSize source.size
            source.size
        }
        {
          sTitle: "Status"
          bVisible: false
          mData: (source, type) ->
            state = if source.schemaName
              if source.errors.length
                "INVALID"
              else if source.fieldReports.length or source.summaryReports.length
                "VALID"
              else
                "NOT VALIDATED"
            else
              "SKIPPED"

            if type == "display"
              return switch state
                when "INVALID"
                  "<span class='error'>" +
                  "<i class='icon-remove-sign'></i> " +
                  state + "</span>"
                when "VALID"
                    "<span class='valid'>" +
                    "<i class='icon-ok-sign'></i> " +
                    state + "</span>"

            state
        }
        {
          sTitle: "Report"
          bSortable: false
          bVisible: false
          mData: (source) =>
            if source.errors.length or source.fieldReports.length \
                or source.summaryReports.length
              "<a href='/releases/#{@model.get('release')}" +
              "/submissions/#{@model.get('projectKey')}" +
              "/report/#{source.name}'>view</span>"
            else
              ""
        }
      ]

    @$el.find("##{datatype}").dataTable
      sDom:
        """
        <'row-fluid'<'span6 #{datatype}_title'l>
        <'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>
        >
        """
       oLanguage:
        "sLengthMenu": "_MENU_ files per page"
        "sEmptyTable": "You need to upload files for this submission."
      bPaginate: false
      aaSorting: [[ 1, "asc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        switch aData.schemaName
          when null
            $(nRow).css {'color': '#999', 'font-style': 'italic'}
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @files

