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
CompleteReleaseView = require 'views/release/complete_release_view'
utils = require 'lib/utils'

module.exports = class ReleaseTableView extends DataTableView
  template: template
  template = null

  autoRender: true

  initialize: ->
    console.debug "ReleasesTableView#initialize", @collection, @el
    super

    @modelBind 'reset', @updateDataTable

    @delegate 'click', '#complete-release-popup-button', @completeReleasePopup

  completeReleasePopup: (e) ->
    console.debug "ReleaseTableView#completeRelease", e
    @subview('CompleteReleases'
      new CompleteReleaseView
        'name': $(e.currentTarget).data('release-name')
    )

  updateDataTable: ->
    if not utils.is_admin()
      dt = @$el.dataTable()
      dt.fnSetColumnVis( 4, false )
    super

  createDataTable: ->
    console.debug "ReleasesTableView#createDataTable"
    aoColumns = [
        {
          sTitle: "Name"
          bUseRendered: false
          mData: (source) ->
            "<a href='/releases/#{source.name}'>#{source.name}</a>"
        }
        { sTitle: "State", mDataProp: "state" }
        {
          sTitle: "Release Date"
          mData: (source) ->
            if source.releaseDate
              utils.date(source.releaseDate)
            else
              "<em>Unreleased</em>"
        }
        {
          sTitle: "Projects"
          mData: (source) ->
            source.submissions.length
        }
        {
          sTitle: "Action"
          bSortable: false
          mData: (source) ->
            if not source.releaseDate
              """
              <button
                class="m-btn blue-stripe mini"
                id="complete-release-popup-button"
                data-toggle="modal"
                data-release-name="#{source.name}"
                href="#complete-release-popup">
                Release Now
              </button>
              """
            else ""
        }
      ]

    @$el.dataTable
      sDom:
        "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
      bPaginate: false
      oLanguage:
        "sLengthMenu": "_MENU_ releases per page"
      aaSorting: [[ 2, "desc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        cell = $('td:nth-child(2)', nRow)
        switch aData.state
          when "OPENED"
            cell.css 'color', '#468847'

      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @collection.toJSON()
