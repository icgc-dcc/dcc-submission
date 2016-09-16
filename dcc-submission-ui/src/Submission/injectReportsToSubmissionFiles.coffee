# from submission.coffee L57-65

module.exports = (submissionFiles, report) ->
  return if not report
  for file in submissionFiles
    for dataTypeReport in report.dataTypeReports
      for fileTypeReport in dataTypeReport.fileTypeReports
        for fileReport in fileTypeReport.fileReports
          if fileReport.fileName == file.name
            Object.assign(file, fileReport)
            file.dataType = dataTypeReport.dataType
            break