"""
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
"""

define (require) ->
  DataTableView = require 'views/base/data_table_view'
  template = require 'text!views/templates/release/submissions_table.handlebars'
  utils = require 'lib/utils'
  
  'use strict'

  class SubmissionFilesTableView extends DataTableView
    template: template
    template = null

    autoRender: true
    
    initialize: ->
      console.debug "SubmissionFilesTableView#initialize", @model, @el
      @collection = @model.get "files"
      
      super
      
    createDataTable: (collection) ->
      console.debug "SubmissionFilesTableView#createDataTable", @.$('table')
      aoColumns = [
          {
            sTitle: "File"
            mDataProp: "name"
            bUseRendered: true
            fnRender: (oObj, sVal) ->
              "<i class='icon-file'></i> #{sVal}"
          }
          {
            sTitle: "Last Updated"
            mDataProp: "lastUpdate"
            sType: "date"
            sWidth: "150px"
            fnRender: (oObj, sVal) ->
              utils.date sVal
          }
          {
            sTitle: "Size"
            mDataProp: "size"
            bUseRendered: false
            sWidth: "100px"
            fnRender: (oObj, Sval) ->
              utils.fileSize Sval
          }
        ]
      
      @.$('table').dataTable
        sDom:
          "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        bPaginate: false
        oLanguage:
          "sLengthMenu": "_MENU_ files per page"
        aaSorting: [[ 1, "desc" ]]
        aoColumns: aoColumns
        sAjaxSource: ""
        sAjaxDataProp: ""
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
