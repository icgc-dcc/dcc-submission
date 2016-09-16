# from submission.coffee L49-54

module.exports = (report) =>
  validFileCount = 0
  report.dataTypeReports.forEach (dataType)->
    dataType.fileTypeReports.forEach (fileType)->
      fileType.fileReports.forEach (file)->
        if file.fileState == "VALID"
          validFileCount += 1
  validFileCount