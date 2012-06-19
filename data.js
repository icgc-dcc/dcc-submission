use icgc;
db.dropDatabase();


// Project
db.Project.insert([{
	"key": "",
	"users": "",
	"groups": ""
}, {
	"key": "",
	"users": "",
	"groups": ""
}]);


// Dictionary
db.Dictionary.insert([{
	"version": "",
	"state": "",
	"files": [{
		"name": "",
		"label": "",
		"pattern": "",
		"role": "",
		"uniqueFields": [
			"", 
			""
		],
		"fields": [{
			"name": "",
			"label": "",
			"valueType": "",
			"restrictions": [{
					"type": "",
					"config": {}
				}, {
					"type": "",
					"config": {}
			}]
		}, {
			"name": "",
			"label": "",
			"valueType": "",
			"restrictions": [{
					"type": "",
					"config": {}
				}, {
					"type": "",
					"config": {}
			}]
		}]
	}] 
}, {
	"version": "",
	"state": "",
	"files": []	
}]);


// Code List
db.CodeList.insert([{
		"name": "",
		"label": "",
		"terms": [{
				"code": "",
				"value": "",
				"uri": ""
			}, {
				"code": "",
				"value": "",
				"uri": ""
		}]
	}, {
		"name": "",
		"label": "",
		"terms": [{
				"code": "",
				"value": "",
				"uri": ""
			}, {
				"code": "",
				"value": "",
				"uri": ""
		}]
}]);


// Release
db.Release.insert([{
		"name": "",
		"state": "",
		"submissions": [{
				"projectKey": "",
				"state": ""
			},{
				"projectKey": "",
				"state": ""
		}],
		"projectKeys": [],
		"dictionary": ""
	}, {
		"name": "",
		"state": "",
		"submissions": [{
				"projectKey": "",
				"state": ""
			},{
				"projectKey": "",
				"state": ""
		}],
		"projectKeys": [],
		"dictionary": ""
}]);