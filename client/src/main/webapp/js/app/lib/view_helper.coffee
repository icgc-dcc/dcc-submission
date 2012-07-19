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
    if "admin" in mediator.user.get "roles"
      options.fn(this)
    else
      options.inverse(this)

  # Return a Unreleased if no release date
  Handlebars.registerHelper 'release_action', (state) ->
    return false unless state is 'OPENED'
    new Handlebars.SafeString """
    <button
      class="btn btn-primary btn-small"
      id="complete-release-popup-button"
      data-toggle="modal"
      href="#complete-release-popup">
      Complete
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

  # Make a date out of epoc
  Handlebars.registerHelper 'submission_summary', (submissions) ->
    signed_off = 0
    valid = 0
    queued = 0
    invalid = 0
    not_validated = 0
    
    for submission in submissions
      switch submission.state
        when 'SIGNED_OFF' then signed_off++
        when 'VALID' then valid++
        when 'QUEUED' then queued++
        when 'INVALID' then invalid++
        when 'NOT_VALIDATED' then invalid++
                
    new Handlebars.SafeString """
      <table class="table">
        <tbody>
        <tr><td>Signed Off</td><td>#{signed_off}</td></tr>
        <tr><td>Valid</td><td>#{valid}</td></tr>
        <tr><td>Queued</td><td>#{queued}</td></tr>
        <tr><td>Invalid</td><td>#{invalid}</td></tr>
        <tr><td>Not Validated</td><td>#{not_validated}</td></tr>
        </tbody>
      </table>
    """


  null