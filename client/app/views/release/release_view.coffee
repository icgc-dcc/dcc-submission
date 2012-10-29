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
PageView = require 'views/base/view'
ReleaseHeaderView = require 'views/release/release_header_view'
CompleteReleaseView = require 'views/release/complete_release_view'
SubmissionTableView = require 'views/submission/submission_table_view'
utils = require 'lib/utils'
template = require 'views/templates/release/release'

module.exports = class ReleaseView extends PageView
  template: template
  template = null

  container: '#page-container'
  containerMethod: 'html'
  tagName: 'div'
  id: 'release-view'

  initialize: ->
    #console.debug 'ReleaseView#initialize', @model
    super

    @subscribeEvent "completeRelease", ->@model.fetch()
    @subscribeEvent "validateSubmission", -> @model.fetch()
    @subscribeEvent "signOffSubmission", -> @model.fetch()

    @delegate 'click', '#complete-release-popup-button', @completeReleasePopup

    i = setInterval( =>

      if @model
        popup = @subviewsByName
          ?.SubmissionsTable
          ?.subviewsByName
          ?.validateSubmissionView?.$el.hasClass('in')
        if not popup
          @model.fetch()
      else
        clearInterval i
    , 10000)

  completeReleasePopup: (e) ->
    #console.debug "ReleaseView#completeRelease", e

    @subview('CompleteReleases'
      new CompleteReleaseView
        'name': @model.get 'name'
        'show': true
    )

  render: ->
    #console.debug "ReleaseView#render", @model
    super

    @subview('ReleaseHeader'
      new ReleaseHeaderView {
        @model
        el: @.$("#release-header-container")
      }
    )

    @subview('SubmissionsTable'
      new SubmissionTableView {
        @model
        el: @.$("#submissions-table")
      }
    )
