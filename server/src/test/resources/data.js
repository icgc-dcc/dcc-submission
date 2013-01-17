db.dropDatabase();

// Project
db.Project.insert({
  "key": "project1",
  "users": ["admin"],
  "groups": ["admin"]
});
db.Project.insert({
  "key": "project2",
  "users": ["admin"],
  "groups": ["admin"]
});

// Dictionary
db.Dictionary.insert({
  "version": "1.0",
  "state": "OPENED",
  "files": [
    {
      "name": "biomarker",
      "label": "",
      "pattern": "^\\w+__\\d+__\\d+__biomarker__\\d+\\.txt$",
      "role": "SUBMISSION",
      "uniqueFields": [
        "", 
        ""
      ],
      "relations": {
        "fields": [],
        "otherFields": [],
        "other": null,
        "allowOrphan": null,
        "joinType": null
      },
      "fields": [
        {
          "name": "donor_id",
          "label": "Unique identifier for the donor; assigned by data provider. It must be coded, and correspond to a donor ID listed in the donor data file.",
          "valueType": "TEXT",
          "restrictions": [
            {
              "type": "required",
              "config": null
            }
          ]
        }, {
          "name": "specimen_id",
          "label": "ID of the specimen on which biomarker ascertainment was performed, if applicable",
          "valueType": "TEXT",
          "restrictions": [
            {
              "type": "required",
              "config": null
            }
          ]
        }
      ]
    }
  ] 
});

// Code List
db.CodeList.insert({
  "name": "appendix_B10",
  "label": "",
  "terms": [{
    "code": "1",
    "value": "GRCh37",
    "uri": ""
  }, {
    "code": "2",
    "value": "NCBI36",
    "uri": ""
  }]
});
db.CodeList.insert({
  "name": "appendix_B12",
  "label": "",
  "terms": [{
    "code": "1",
    "value": "EGA",
    "uri": ""
  }, {
    "code": "2",
    "value": "dbSNP",
    "uri": ""
  }]
});


// Release
db.Release.insert({
  "name": "release1",
  "state": "COMPLETED",
  "submissions": [
    {
      "projectKey": "project1",
      "state": "SIGNED_OFF"
    },{
      "projectKey": "project2",
      "state": "SIGNED_OFF"
   }
  ],
  "projectKeys": ["project1", "project2"],
  "dictionary": "1.0"
});
db.Release.insert({
  "name": "release2",
  "state": "OPENED",
  "submissions": [
    {
      "projectKey": "project1",
      "state": "QUEUED"
    },{
      "projectKey": "project2",
      "state": "NOT_VALIDATED"
   }
  ],
  "projectKeys": ["project1", "project2"],
  "dictionary": "1.0"
});
