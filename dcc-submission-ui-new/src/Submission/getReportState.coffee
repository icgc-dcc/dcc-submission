module.exports = (source, type) ->
  return switch source.fileState
    when "NOT_VALIDATED"
      "<span><i class='icon-question-sign'></i> " +
        source.fileState.replace('_', ' ') + "</span>"
    when "ERROR"
      "<span class='error'>" +
        "<i class='icon-exclamation-sign'></i> " +
        source.fileState.replace('_', ' ') + "</span>"
    when "INVALID"
      "<span class='error'><i class='icon-remove-sign'></i> " +
        source.fileState.replace('_', ' ') + "</span>"
    when "QUEUED"
      "<span class='queued'><i class='icon-time'></i> " +
      source.fileState.replace('_', ' ') + "</span>"
    when "VALIDATING"
      "<span class='validating'><i class='icon-cogs'></i> " +
      source.fileState.replace('_', ' ') + "</span>"
    when "VALID"
      "<span class='valid'><i class='icon-ok-sign'></i> " +
        source.fileState.replace('_', ' ') + "</span>"
    when "SIGNED_OFF"
      "<span class='valid'><i class='icon-lock'></i> " +
        source.fileState.replace('_', ' ') + "</span>"
    else
      "SKIPPED"
