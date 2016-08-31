# from schema_report_error_table_view.coffee L314-378

module.exports = (source) ->

    # There are generally two types of errors: file level errors
    # with no line details, and row level errors
    if source.errorType in [
      "COMPRESSION_CODEC_ERROR"
      "TOO_MANY_FILES_ERROR"
      #"FILE_HEADER_ERROR"
      "RELATION_FILE_ERROR"
      "MISSING_ROWS_ERROR"
      "REVERSE_RELATION_FILE_ERROR"
      "TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR"
      ]
      return ""
    else if source.errorType in [
      "MISSING_VALUE_ERROR"
      "OUT_OF_RANGE_ERROR"
      "NOT_A_NUMBER_ERROR"
      "INVALID_CHARSET_ROW_ERROR"
      "LINE_TERMINATOR_MISSING_ERROR"
      #"STRUCTURALLY_INVALID_ROW_ERROR"
      ]
      return source.lineNumbers.join ', '
    else if source.errorType is 'FILE_HEADER_ERROR'
      console.log ">>>", source
      expected = source.parameters.EXPECTED
      actual = source.parameters.VALUE
      displayLength = Math.max(expected.length, actual.length)

      out = ""
      out += "<br><table class='table table-condensed'>
        <th style='border:none'>Expected</th>
        <th style='border:none'>Actual</th>"

      console.log expected
      console.log actual
      for i in [0..displayLength - 1] by 1
        expectedVal = expected[i] || '-'
        actualVal = actual[i] || '-'
        out += "<tr>
          <td style='background:none;border:none'>
          #{expectedVal}</td>
          <td style='background:none;border:none'>
          #{actualVal}</td></tr>"
      out += "</table>"
      return out

    else if source.fieldNames[0] is "FileLevelError"
      return ""

    out = ""

    out += "<br><table class='table table-condensed'>
      <th style='border:none'>Line</th>
      <th style='border:none'>Value</th>"
    for i in source.lineNumbers
      if i==-1
        display = "N/A"
      else
        display = i
      out += "<tr><td style='background:none;border:none'>#{display}</td>
      <td style='background:none;border:none'>
      #{source.lineValueMap[i]}</td></tr>"
    out += "</table>"
    out