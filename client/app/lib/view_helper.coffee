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
utils = require 'chaplin/lib/utils'

# View helpers (Handlebars in this case)
# --------------------------------------

# Add application-specific Handlebars helpers
# -------------------------------------------

# Choose block by user login status
Handlebars.registerHelper 'if_logged_in', (options) ->
  console.log mediator.user
  if mediator.user
    options.fn(this)
  else
    options.inverse(this)

Handlebars.registerHelper 'is_admin', (options) ->
  "admin" in mediator.user.get "roles"

Handlebars.registerHelper 'if_admin', (options) ->
  if is_admin
    options.fn(this)
  else
    options.inverse(this)

Handlebars.registerHelper 'if_opened', (state, options) ->
  if state is 'OPENED'
    options.fn(this)
  else
    options.inverse(this)

Handlebars.registerHelper 'if_completed', (state, options) ->
  if state is 'COMPLETED'
    options.fn(this)
  else
    options.inverse(this)


# Return a Unreleased if no release date
Handlebars.registerHelper 'submission_action', (state) ->
  switch state
    when "VALID"
      new Handlebars.SafeString """
      <button
        class="btn btn-success"
        id="signoff-submission-popup-button"
        data-toggle="modal"
        href="#signoff-submission-popup">
        Sign Off
      </button>
      """
    when "INVALID", "NOT_VALIDATED"
      new Handlebars.SafeString """
      <button
        class="btn btn-success"
        id="validate-submission-popup-button"
        data-toggle="modal"
        href="#validate-submission-popup">
        Launch Validation
      </button>
      """

# Return a Unreleased if no release date
Handlebars.registerHelper 'release_date', (date) ->
  return new Handlebars.SafeString '<em>Unreleased</em>' unless date
  Handlebars.helpers.date.call(this, date)

# Make a date out of epoc
Handlebars.registerHelper 'date', (date) ->
  return false unless date
  new Handlebars.SafeString moment(date).format("YYYY-MM-DD")

Handlebars.registerHelper 'underscore2space', (string) ->
  return false unless string
  new Handlebars.SafeString string.replace "_", " "

Handlebars.registerHelper 'lowercase', (string) ->
  return false unless string
  new Handlebars.SafeString string.toLowerCase()


Handlebars.registerHelper 'release_summary', (submissions) ->
  console.log submissions
  signed_off = 0
  valid = 0
  queued = 0
  invalid = 0
  not_validated = 0

  for submission in submissions.models
    console.log submission
    switch submission.get "state"
      when 'SIGNED_OFF' then signed_off++
      when 'VALID' then valid++
      when 'QUEUED' then queued++
      when 'INVALID' then invalid++
      when 'NOT_VALIDATED' then not_validated++

  new Handlebars.SafeString """
    <li>Signed Off: #{signed_off}</li>
    <li>Valid: #{valid}</li>
    <li>Queued: #{queued}</li>
    <li>Invalid: #{invalid}</li>
    <li>Not Validated: #{not_validated}</li>
  """