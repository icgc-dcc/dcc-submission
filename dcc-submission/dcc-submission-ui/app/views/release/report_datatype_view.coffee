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

    # For determining table visibility if files were added/removed
    @currentDatatypes = []


    @files = []
    @datatypeCache = {}

    super

    @modelBind 'change', @update

  update: ->
    #console.log "ReportDatatypeView -> update"
    console.log "update", @model

    
    # Start contstructing
    @report = @model.get "report"
    @reportDataType = @report.dataTypeReports
    console.log "2 reportdatatype", @reportDataType


    # Extract the datatypes for logical sort
    datatypes = []
    datatypes.push("CLINICAL_CORE_TYPE")
    @reportDataType.forEach (d) ->
      if datatypes.indexOf(d.dataType) == -1
        datatypes.push(d.dataType)

    datatypes = _.sortBy datatypes, (datatype)->
      switch datatype
        when "CLINICAL_CORE_TYPE"
          return 0
        when "CLINICAL_OPTIONAL_TYPE"
          return 1
        when "MISCELLANEOUS"
          return 999
        else
          return 10

    console.log "3 sorted", datatypes


    datatypes.forEach (datatype)=>
      container = null
      if datatype in ["CLINICAL_OPTIONAL_TYPE", "CLINICAL_CORE_TYPE"]
        container = @$el.find("#clinical-report-container")
      else if datatype == "MISCELLANEOUS"
        container = @$el.find("#miscellaneous-report-container")
      else
        container = @$el.find("#experimental-report-container")

      elem = container.find("#"+datatype)
      if elem.length == 0
        container.append("<table id='#{datatype}'></table>")

        elem = container.find("##{datatype}")
        elem.addClass("report table table-striped table-bordered table-hover")
        @createDataTable(datatype)

    # Clean up (deletions)
    @currentDatatypes.forEach (datatype)=>
      if datatypes.indexOf(datatype) == -1
        @$el.find("[id^="+datatype+"]").remove()
        #container.find("[id^="+datatype+"]").remove()
    @currentDatatypes = datatypes

    # Clean up (section visiblity)
    if @$el.find("#miscellaneous-report-container table").length == 0
      @$el.find("#miscellaneous-report-container").css("visibility", "hidden")
    else
      @$el.find("#miscellaneous-report-container").css("visibility", "visible")

        
    @updateDataTable()


  getAction: (datatype, state, globalState)->
    title = utils.translateDataType(datatype)
    lc_state = state.toLowerCase()
    ui_state = state.replace("_", " ")

    if state == ""
      ""
    else if state in ["VALIDATED", "INVALID", "NOT_VALIDATED"]
      """
      <a data-toggle="modal"
         class="m-btn mini green"
         style="height:auto; margin-top:0"
         href="#validate-submission-popup"
         id="validate-submission-popup-button">
      Validate #{title}
      </a>
      """

  getTitleBar: (datatype, state, globalState)->
    title = utils.translateDataType(datatype)
    lc_state = state.toLowerCase()
    ui_state = state.replace("_", " ")

    if state == ""
      """
      <span>#{title}</span>
      """
    else
      """
      <span>#{title} - </span>
      <span class="#{lc_state}">#{ui_state}</span>
      """

  # Since we chop and dice the collection, we need to use a different update
  updateDataTable: ->
    #console.log @model.get("dataState")
    console.log "updateDataTable"

    globalState = @model.get("state")

    @currentDatatypes.forEach (datatype) =>
      console.log "4 ", @model.get "submissionFiles"
      @files = _.filter @model.get "submissionFiles", (f)->
        return f.type == datatype
      console.log "4 ", datatype, @files
      
    return

    @currentDatatypes.forEach (datatype)=>
      @files = _.filter @report.get("schemaReports").toJSON(), (d)->
        type = d.dataType
        if type == null
          type = "MISCELLANEOUS"
        return type == datatype
      dt = @$el.find("#"+datatype).dataTable()

      state = @dataStateMap[datatype]
      if not state
        state = ""


      # Check if the data has changed or not
      tempCache = {}
      @files.forEach (f)=>
        tempCache[f.name] = f.lastUpdate
      
      if not _.isEqual tempCache, @datatypeCache[datatype]
        console.log datatype + " has changed..."
        dt.fnClearTable()
        dt.fnAddData @files

      @datatypeCache[datatype] = _.clone(tempCache)


      target = "." + datatype + "_title"
      $(target).children().remove()
      $(target).append( @getTitleBar(datatype, state, globalState) )

      target = "." + datatype + "_action"
      $(target).children().remove()
      $(target).data("datatype", datatype)
      $(target).append( @getAction(datatype, state, globalState) )


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
          bVisible: true
          mData: (source, type) =>
            state = @dataStateMap[source.dataType]
            if source.schemaName
              if state
                state = state.replace("_", " ")
              else
                state = "NOT VALIDATED"
            else
              state = "SKIPPED"


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
          bVisible: true
          mData: (source) =>
            # Allow viewing reports if status is idle
            fileState = @dataStateMap[source.dataType]
            if fileState in ["QUEUED", "VALIDATING"]
              ""
            else if source.errors.length or source.fieldReports.length \
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
        <'row-fluid' <'span3 #{datatype}_title'l><'span3 #{datatype}_action'> <'span6'f>r>
        t<'row-fluid'<'span6'i><'span6'p>>"
        """
       oLanguage:
        "sLengthMenu": "_MENU_ files per page"
        "sEmptyTable": "You need to upload files for this submission."
      iDisplayLength: 2
      sPaginationType: "full_numbers"
      bPaginate: true
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

