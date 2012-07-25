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
  CompactReleaseView = require 'views/release/compact_release_view'
  template = require 'text!views/templates/release/releases_table.handlebars'
  utils = require 'lib/utils'
  
  'use strict'

  class ReleaseTableView extends DataTableView
    template: template
    template = null
    autoRender: true
    
    container: '#releases-table'
    containerMethod: 'html'
    tagName: 'table'
    className: "releases table table-striped"
    id: "releases"
    
    initialize: ->
      console.debug "ReleasesTableView#initialize", @collection, @el
      super
      
      @subscribeEvent "completeRelease", @fetch
         
    createDataTable: (collection) ->
      console.debug "ReleasesTableView#createDataTable"
      aoColumns = [
          {
            sTitle: "Name"
            mDataProp: "name"
            fnRender: (oObj, sVal) ->
              "<a href='/releases/#{sVal}'>#{sVal}</a>"
          }
          { sTitle: "State", mDataProp: "state" }
          {
            sTitle: "Release Date"
            mDataProp: "releaseDate"
            fnRender: (oObj, sVal) ->
              if sVal
                utils.date(sVal)
              else
                if utils.is_admin
                  """
                    <a
                      id="complete-release-popup-button"
                      data-toggle="modal"
                      href="#complete-release-popup">
                      Release Now
                    </a>
                  """
                else
                  "<em>Unreleased</em>"
          }
          { sTitle: "Projects", mDataProp: "submissions.length" }
        ]
      
      @.$('table').dataTable
        sDom:
          "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        sPaginationType: "bootstrap"
        oLanguage:
          "sLengthMenu": "_MENU_ releases per page"
        aaSorting: [[ 2, "desc" ]]
        aoColumns: aoColumns
        sAjaxSource: ""
        sAjaxDataProp: ""
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
