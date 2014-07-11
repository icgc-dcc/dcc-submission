////////////////////////////////////////////////////////////////////////////////
// Dictionary reader and utilities
////////////////////////////////////////////////////////////////////////////////
var DictionaryUtil = function(list, config) {

   list = _.filter(list, function(d) {
      var pattern = new RegExp("^draft");
      return pattern.test(d.version) === false;
   });

   this.dictList = list;
   this.config = config;
   this.sortedDictionaryList = _.sortBy(this.dictList, function(obj) {
      return obj.version;
   }).reverse();
   this.versionList = _.pluck(this.sortedDictionaryList, "version");
   this.dictionaryMap = {};

   for (var i=0; i < list.length; i++) {
      this.dictionaryMap[ list[i].version ] = list[i];
   }
};


////////////////////////////////////////////////////////////////////////////////
// Basic Getters
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.getDictionary = function(version) {
   return this.dictionaryMap[version];
};

DictionaryUtil.prototype.getFileType = function(version, filename) {
   var dict = this.getDictionary(version);
   if (!dict) return null;
   return _.find(dict.files, function(f) { return f.name == filename; });
};

DictionaryUtil.prototype.getField = function(version, filename, fieldname) {
   var file = this.getFileType(version, filename);
   if (!file) return null;
   return _.find(file.fields, function(f) { return f.name == fieldname; });
};



////////////////////////////////////////////////////////////////////////////////
// How the files should be sorted
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.sortingOrder = function() {
   return [
      "donor", "specimen", "sample",
      "biomarker", "surgery", "therapy", "family", "exposure",
      "ssm_m", "ssm_p", "ssm_s",
      "sgv_m", "sgv_p",
      "cnsm_m", "cnsm_p", "cnsm_s",
      "stsm_m", "stsm_p", "stsm_s",
      "exp_g", "exp_m",
      "pexp_m", "pexp_p",
      "mirna_m", "mirna_p", "mirna_s",
      "jcn_m", "jcn_p",
      "meth_m", "meth_p", "meth_s",
      "meth_seq_m", "meth_seq_p",
      "mirna_seq_m", "mirna_seq_p",
      "exp_seq_m", "exp_seq_p",
      "exp_array_m", "exp_array_p",
      "meth_array_m", "meth_array_p", 
      "meth_array_probes"
   ];
}


////////////////////////////////////////////////////////////////////////////////
// Determines whether if there are significant changes
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.isDifferent = function(row1, row2) {
   if (! _.isEqual(row1.restrictions, row2.restrictions)) return true;
   if (! _.isEqual(row1.label, row2.label)) return true;

   return false;
};


////////////////////////////////////////////////////////////////////////////////
// Verbose comparison, gives a detail list of what are the differences
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.isDifferent2 = function(row1, row2) {
   var diffList = [];
   function getRestriction(r, type) {
      if (! r.restrictions) return {};
      return _.find(r.restrictions, function(res) {
         return res.type === type;
      });
   }

   if (row1.controlled !== row2.controlled) {
      diffList.push("controlled");
   }
   if (! _.isEqual(getRestriction(row1, "regex"), getRestriction(row2, "regex"))) {
      diffList.push("regex");
   }
   if (! _.isEqual(getRestriction(row1, "required"), getRestriction(row2, "required"))) {
      diffList.push("required");
   }
   if (! _.isEqual(getRestriction(row1, "script"), getRestriction(row2, "script"))) {
      diffList.push("script");
   }
   if (! _.isEqual(getRestriction(row1, "codelist"), getRestriction(row2, "codelist"))) {
      diffList.push("codelist");
   }

   return diffList;
};



////////////////////////////////////////////////////////////////////////////////
// Determines whether if row matches
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.isMatch = function(row, regex) {
   var restrictionList = row.restrictions;
   var codelist = _.find(restrictionList, function(obj) { return obj.type == 'codelist'; });

   if (row.name.match(regex) || (codelist && codelist.config.name.match(regex))) {
      return true;
   }

   return false;
};


////////////////////////////////////////////////////////////////////////////////
// Produces a difference list by comparing two different verions of a dictionary
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.createDiffListing = function(versionFrom, versionTo) {
   var report = {};
   var _self = this;
   report.fieldsAdded   = [];
   report.fieldsRemoved = [];
   report.fieldsChanged = [];


   // Scan for changed and remove file types
   var dictFrom = _self.getDictionary(versionFrom);
   if (dictFrom) {
      dictFrom.files.forEach(function(fileFrom) {
         var fileTo = _self.getFileType(versionTo, fileFrom.name);

         // If no file, then it as been removed.
         // Otherwise we need to check each field for changes.
         if (!fileTo) {
            fileFrom.fields.forEach(function(fieldFrom) {
               report.fieldsRemoved.push({
                  fileType: fileFrom.name,
                  fieldName: fieldFrom.name
               });
            });
         } else {
            // Check removed and changed
            fileFrom.fields.forEach(function(fieldFrom) {
               var fieldTo = _self.getField(versionTo, fileFrom.name, fieldFrom.name);

               if (!fieldTo) {
                  report.fieldsRemoved.push({
                     fileType: fileFrom.name,
                     fieldName: fieldFrom.name
                  });
               } else if (_self.isDifferent2(fieldTo, fieldFrom).length > 0) {
                  report.fieldsChanged.push({
                     fileType: fileFrom.name,
                     fieldName: fieldFrom.name,
                     changes: _self.isDifferent2(fieldTo, fieldFrom)
                  });
               }
            });

            // Check new
            fileTo.fields.forEach(function(fieldTo) {
               var fieldFrom = _self.getField(versionFrom, fileTo.name, fieldTo.name);

               if (!fieldFrom) {
                  report.fieldsAdded.push({
                     fileType: fileTo.name,
                     fieldName: fieldTo.name
                  });
               }
            });
         }
      });
   }

   // Now reverse logic to check for completely new file types
   var dictTo = _self.getDictionary(versionTo);
   if (dictTo) {
      dictTo.files.forEach(function(fileTo) {
         var fileFrom = _self.getFileType(versionFrom, fileTo.name);
         // fileTo is new
         if (!fileFrom) {
            fileTo.fields.forEach(function(fieldTo) {
               report.fieldsAdded.push({
                  fileType: fileTo.name,
                  fieldName: fieldTo.name
               });
            });
         }
      });
   }

   return report;
}


////////////////////////////////////////////////////////////////////////////////
// Build a tree using a edge list 
// It isn't a tree per se, but semantically a tree representation makes most sense
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.buildTree2 = function(node, relations, visited, dict) {
   var _self = this;
   node.data = _.find(dict.files, function(f) { return f.name === node.name });
   var children = _.filter(relations, function(r) {
      return r.parentNode === node.name;
   });

   node.children = [];

   children.forEach(function(child) {
      if (visited[child.node]) return;
      //if (visited[child.node]) {
      //}

      var c = {name:child.node, ancestor:node.name};
      node.children.push(c);
      visited[child.node] = 1;
   });

   node.children = _.sortBy(node.children, function(child) {
      return child.name;
   });

   node.children.forEach(function(child) {
      _self.buildTree2(child, relations, visited, dict);
   });

   node.children.sort(function(n) {
      return n.name.length;
   });
};


////////////////////////////////////////////////////////////////////////////////
// Return a list of describing filetype->filetype relations
////////////////////////////////////////////////////////////////////////////////
DictionaryUtil.prototype.getParentRelation = function(dict) {
   var list = [];
   var roleMap = {}
   var files = dict.files;

   files.forEach(function(f) {
      roleMap[f.name] = f.role;
   });

   files.forEach(function(f) {
      if (f.relations.length === 0) {
         list.push({
            node: f.name,
            parentNode: null
         });
      } else {
         f.relations.forEach(function(r) {
            if (roleMap[r.other] === "SYSTEM") {
               list.push({
                  node:r.other,
                  parentNode: f.name
               });
            } else {
               list.push({
                  node:f.name,
                  parentNode: r.other
               });
            }
         });
      }
   });
   return list;
};

