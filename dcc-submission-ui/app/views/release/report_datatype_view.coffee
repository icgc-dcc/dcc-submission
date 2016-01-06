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
    @fileMap = {}

    super

    @modelBind 'change', @update

  update: ->
    #console.log "ReportDatatypeView -> update"

    # Update file meta data
    submissionFiles = @model.get "submissionFiles"
    @fileMap = {}
    submissionFiles.forEach (f)=>
      @fileMap[f.name] = f



    # Start contstructing
    @report = @model.get "report"
    @reportDataType = @report.dataTypeReports


    # Extract the datatypes for logical sort
    # Clinical is alway present, and should be shown first
    datatypes = []
    datatypes.push("CLINICAL_CORE_TYPE")
    @reportDataType.forEach (d) ->
      if datatypes.indexOf(d.dataType) == -1
        datatypes.push(d.dataType)

    datatypes = _.sortBy datatypes, (datatype)->
      switch datatype
        when "CLINICAL_CORE_TYPE"
          return 0
        when "CLINICAL_SUPPLEMENTAL_TYPE"
          return 1
        else
          return 10



    # Dynamically manage tables
    datatypes.forEach (datatype)=>
      container = null
      if datatype in ["CLINICAL_SUPPLEMENTAL_TYPE", "CLINICAL_CORE_TYPE"]
        container = @$el.find("#clinical-report-container")
      else
        container = @$el.find("#experimental-report-container")

      elem = container.find("#"+datatype)
      if elem.length == 0
        container.append("<table id='#{datatype}'></table><br>")

        elem = container.find("##{datatype}")
        elem.addClass("report table table-striped table-bordered table-hover")
        @createDataTable(datatype)

    # Clean up (deletions)
    @currentDatatypes.forEach (datatype)=>
      if datatypes.indexOf(datatype) == -1
        @$el.find("[id^="+datatype+"]").remove()
        #container.find("[id^="+datatype+"]").remove()
    @currentDatatypes = datatypes

    @updateDataTable()



    # Handle special "null" types for misc/unrecognized
    unrecognized = _.filter submissionFiles, (f)->
      return f.fileType == null

    container = @$el.find("#miscellaneous-report-container")
    if unrecognized and unrecognized.length > 0
      container.css("visibility", "visible")
      elem = container.find("#MISCELLANEOUS")

      # Create if not exist
      if elem.length == 0
        container.append("<table id='MISCELLANEOUS'></table>")
        elem = container.find("#MISCELLANEOUS")
        elem.addClass("report table table-striped table-bordered table-hover")
        @createDataTable("MISCELLANEOUS")

      dt = @$el.find("#MISCELLANEOUS").dataTable()
      temp = []
      miscCache = {}
      unrecognized.forEach (f)=>
        miscCache[f.name] = @fileMap[f.name].lastUpdate
        temp.push({fileName:f.name, fileState:"SKIPPED"})

      if not _.isEqual miscCache, @datatypeCache["MISCELLANEOUS"]
        dt.fnClearTable()
        dt.fnAddData temp
      @datatypeCache["MISCELLANEOUS"] = _.clone miscCache
    else
      @$el.find("#miscellaneous-report-container").css("visibility", "hidden")


  getAction: (dataType)->
    state = dataType.dataTypeState
    title = utils.translateDataType(dataType.dataType)
    lc_state = state.toLowerCase()
    ui_state = state.replace("_", " ")

    globalState = @model.get "state"

    if state == "" or globalState in ["QUEUED", "VALIDATING", "ERROR"]
      ""
    else if state in ["VALID", "INVALID", "NOT_VALIDATED"]
      """
      <a data-toggle="modal"
         class="m-btn mini blue"
         style="height:auto; margin-top:0"
         href="#validate-submission-popup"
         id="validate-submission-popup-button">
      Validate #{title}
      </a>
      """

  getTitleBar: (dataType)->
    title = utils.translateDataType(dataType.dataType)
    lc_state = dataType.dataTypeState.toLowerCase()
    ui_state = dataType.dataTypeState.replace("_", " ")

    """
    <span>#{title}&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;</span> #{utils.getStateDisplay dataType.dataTypeState}
    """

  # Since we chop and dice the collection, we need to use a different update
  updateDataTable: ->
    #console.log @model.get("dataState")


    @datatypes = @report.dataTypeReports
    @datatypes.forEach (dataType) =>
      files = []
      filesCache = {}

      dt = @$el.find("#"+dataType.dataType).dataTable()
      fileTypeReports = dataType.fileTypeReports
      fileTypeReports.forEach (fileType) =>
        fileReports = fileType.fileReports
        fileReports.forEach (file) =>
          # Datatable
          files.push file

          # To tell if files changed
          filesCache[file.fileName] = @fileMap[file.fileName].lastUpdate + file.fileState

      # Only reset pagination when delete/update/new files,  status change
      if not _.isEqual filesCache, @datatypeCache[dataType.dataType]
        dt.fnClearTable()
        dt.fnAddData files
      @datatypeCache[dataType.dataType] = _.clone(filesCache)


      # Update the table header and actions
      target = "." + dataType.dataType + "_title"
      $(target).children().remove()
      $(target).append( @getTitleBar(dataType) )

      target = "." + dataType.dataType + "_action"
      $(target).children().remove()
      $(target).data("datatype", dataType.dataType)
      $(target).append( @getAction(dataType) )

  createDataTable: (datatype)->
    #console.debug "ReportTableView#createDataTable", @$el, @model.get "name"
    aoColumns = [
        {
          sTitle: "File"
          mData: "fileName"
        }
        {
          sTitle: "Last Updated"
          mData: (source) -> utils.date source.lastUpdate
          sType: "date"
        }
        {
          sTitle: "Size"
          bUseRendered: false
          mData: (source, type) =>
            if type is "display"
              return utils.fileSize @fileMap[source.fileName].size
            @fileMap[source.fileName].size
        }
        {
          sTitle: "Status"
          bVisible: true
          mData: (source, type) ->
            #state = @dataStateMap[source.dataType]
            return utils.getStateDisplay source.fileState
        }
        {
          sTitle: "Report"
          bSortable: false
          bVisible: true
          mData: (source) =>
            fileState = source.fileState
            if fileState in ["QUEUED", "VALIDATING", "SKIPPED"]
              ""
            else if source.errorReports.length or source.fieldReports.length \
                or source.summaryReports.length
              "<a href='/releases/#{@model.get('release')}" +
              "/submissions/#{@model.get('projectKey')}" +
              "/report/#{source.fileName}'>view</span>"
            else
              ""
        }
      ]


    sDOMStr = switch datatype
      when "MISCELLANEOUS"
        """
          <'row-fluid'<'span12'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        """
      else
        """
          <'row-fluid' <'span12 #{datatype}_title'>>
          <'row-fluid' <'span6 #{datatype}_action'><'span6'f>r>
          t<'row-fluid'<'span6'i><'span6'p>>"
        """


    @$el.find("##{datatype}").dataTable
      sDom:
        sDOMStr
       oLanguage:
        "sLengthMenu": "_MENU_ files per page"
        "sEmptyTable": "You need to upload files for this submission."
        #"oPaginate":
          #"sPrevious": "< "
          #"sNext": " >"
      iDisplayLength: 10
      sPaginationType: "full_numbers"
      bPaginate: true
      aaSorting: [[ 0, "asc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        switch aData.fileState
          when "SKIPPED"
            $(nRow).css {'color': '#999', 'font-style': 'italic'}
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @files

