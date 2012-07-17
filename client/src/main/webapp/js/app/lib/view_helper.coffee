define [
  'handlebars'
  'chaplin'
  'lib/utils'
], (Handlebars, Chaplin, utils) ->
  'use strict'

  # View helpers (Handlebars in this case)
  # --------------------------------------

  # Shortcut to the mediator
  mediator = Chaplin.mediator

  # Add application-specific Handlebars helpers
  # -------------------------------------------

  # Choose block by user login status
  Handlebars.registerHelper 'if_logged_in', (options) ->
    console.log mediator.user
    if mediator.user
      options.fn(this)
    else
      options.inverse(this)
  
  # Choose block by user login status
  Handlebars.registerHelper 'unless_complete', (options) ->
    if mediator.user
      options.fn(this)
    else
      options.inverse(this)

  Handlebars.registerHelper 'if_admin', (options) ->
    if "admin" not in mediator.user.get "roles"
      options.fn(this)
    else
      options.inverse(this)

  # Return a Unreleased if no release date
  Handlebars.registerHelper 'release_action', (state) ->
    return false unless state is 'OPENED'
    new Handlebars.SafeString """
    <button class="btn btn-primary btn-small" data-toggle="modal" href="#completeRelease">
      Complete
    </button>
    <div class="modal hide fase" id="completeRelease">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">×</button>
        <h3>Complete release</h3>
      </div>
      <div class="modal-body">
        <fieldset class="well form-inline">
        <div class="control-group">
            <label class="control-label" for="nextRelease">Next Release Name</label>
            <div class="controls">
              <input type="text" id="nextRelease">
            </div>
          </div>
      </fieldset>
      </div>
      <div class="modal-footer">
        <a href="#" class="btn" data-dismiss="modal">Close</a>
        <a href="#" class="btn btn-success">Complete release</a>
      </div>
    </div>
    """

  # Return a Unreleased if no release date
  Handlebars.registerHelper 'release_date', (date) ->
    return new Handlebars.SafeString '<em>Unreleased</em>' unless date
    Handlebars.helpers.date.call(this, date)
    
  # Make a date out of epoc
  Handlebars.registerHelper 'date', (date) ->
    return false unless date
    new Handlebars.SafeString moment(date).format("YYYY-MM-DD")

  null