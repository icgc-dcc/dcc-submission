function TableViewer(config, d) {
   this.config = config;
   this.dictUtil = d;
   this.isTable = true;
   this.toggleNodeFunc = null;
   this.toggleDataTypeFunc = null;

   this.selectedDataType = 'all';


   // Configuraitons
   this.barHeight = 25;
   this.colourDefault   = d3.rgb(240, 240, 240);

   //this.colourNew       = d3.rgb(0,188,58);
   //this.colourChanged   = d3.rgb(177,12,12);
   this.colourHighlight = d3.rgb(242, 155, 4);

   this.colourNew = d3.rgb(77,175,74);
   this.colourChanged = d3.rgb(228,26,28);

   this.colourMinimapDefault = d3.rgb(230,230,230);
   this.colourMinimapSelect  = d3.rgb(166, 206, 227);
}

////////////////////////////////////////////////////////////////////////////////
// Main dictionary function
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.showDictionaryTable = function(versionFrom, versionTo) {

   var _self = this;
   var data = _self.dictUtil.getDictionary(versionTo);


   // Reset
   d3.select("#datatypeGraph").transition().duration(300).style("opacity", 0.1).each("end", function() {
      d3.select("#datatypeGraph").style("display", "none");
      d3.select("#datatypeTable").style("display", "block");
      d3.select("#datatypeSelector").style("visibility", "visible");
      d3.select("#datatypeTable").transition().duration(400).style("opacity", 1.0);
   });

   // Re-order according to importance and data type
   data.files  = _.sortBy(data.files, function(d) {
     return _self.dictUtil.sortingOrder().indexOf(d.name);
   });

   // Clean up DOM
   d3.select("#minimap").selectAll("*").remove();
   d3.select("#datatypeTable").selectAll("*").remove();

   // Ensure basic visiblity interactions are in place
   d3.select("body").on("click", function() {
      d3.select("#minimapWrapper").style("display", "none");
   });

   d3.select("#minimapLabel")
     .classed("data-type-list", true)
     .on("click", function() {
        if (d3.select("#minimapWrapper").style("display") === "none") {
           d3.select("#minimapWrapper").style("display", "block");
        } else {
           d3.select("#minimapWrapper").style("display", "none");
        }
        d3.event.stopPropagation();
     });


   // Hardwire "all data type" minimap option, note minimap height is dynamically
   // computed.
   var currentY = 1;
   var grp = d3.select("#minimap").append("g").attr("id", "minimap-all").attr("transform", "translate(" + 0 + "," + (currentY) + ")");
   _self.buildFilterRow(grp, "all", "All Data Types", _self.barHeight);
   currentY += _self.barHeight;


   // Calculate data type removals
   /*
   var vTo = getSelect("#version_a");
   var dTo = dictUtil.dictionaryMap[vTo];
   var vFrom = getSelect("#version_b");
   var dFrom = dictUtil.dictionaryMap[vFrom];
   var deleteTableList = [];
   dFrom.files.forEach(function(f) {
      if (!_.find(dTo.files, function(obj) { return obj.name === f.name; })) {
         deleteTableList.push(f.name);
      }
   });

   if (deleteTableList.length > 0) {
      d3.select("#datatypeRemoved").text("Data types removed in " + vTo + ": " + deleteTableList.join(", "));
      changeSummary["datatype_removed"] = deleteTableList;
   } else {
      d3.select("#datatypeRemoved").text("");
   }
   */

   // Main building block
   d3.select("#datatypeTable")
     .selectAll("div")
     .data( data.files )
     .enter()
     .append("div")
     .classed("selection_wrapper", true)
     .append("div")
     .classed("filter_wrapper", true)
     .each(function(table, i) {

        // Section title
        d3.select(this).append("br");
        d3.select(this).append("strong").text(table.label);
        d3.select(this).append("br");

        // Generate a deleted columns list, or show table wiped
        /*
        var tableName = table.name;
        var comparedVer  = getSelect("#version_b");
        var compareDict  = dictUtil.dictionaryMap[comparedVer];
        var compareTable = _.find(compareDict.files, function(obj) { return obj.name === tableName; });
        var deleteFieldList = [];
        if (compareTable) {
           compareTable.fields.forEach(function(f) {
              if ( ! _.find(table.fields, function(o) { return o.name == f.name; }) ) {
                 deleteFieldList.push(f.name);
              }
           });
        }
        if (deleteFieldList.length > 0) {
           var ver = getSelect("#version_a");
           d3.select(this).append("small").classed("removed", true).text("Fields removed in " + ver + ": " + deleteFieldList.join(", "));
        }
        */


        // Create the minimap things
        var realH = Math.max(_self.barHeight, 5+table.fields.length);
        var grp = d3.select("#minimap").append("g").attr("id", "minimap-"+table.name).attr("transform", "translate(" + 0 + "," + (currentY) + ")");
        _self.buildFilterRow(grp, table.name, table.label, realH);
        currentY += realH;


        // Table - definition
        var dtable = d3.select(this)
          .append("table")
          .classed("table", true)
          .classed("table-bordered", true)
          .classed("table-hover", true)
          .classed("table-condensed", true)
          .classed("dictionary_table", true);

        // Table - columns
        var thead = dtable.append("thead").append("tr");
        thead.append("th").style("width", "5px").text("");
        thead.append("th").text("Field");
        thead.append("th").text("Attribute");
        thead.append("th").text("Type");
        thead.append("th").text("Description");
        thead.append("th").text("CodeList");
        thead.append("th").text("RegExp");
        thead.append("th").text("Script");

        // Table - build
        var tbody = dtable.append("tbody");
        tbody.selectAll("tr")
          .data(table.fields)
          .enter()
          .append("tr")
          .style("font-size", "1em")
          .each(function(row, idx) {
             var rrr = _self.dictUtil.getField(versionFrom, table.name, row.name);
             _self.buildRow(d3.select(this), row, rrr, idx);
          });
     });

     // Auto adjust the svg height
     d3.select("#minimap").attr("height", (currentY+5) + "px");

     // Turn on regexp highlighter
     RegexColorizer.colorizeAll();
}


////////////////////////////////////////////////////////////////////////////////
// Build a data type table row
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.buildRow = function(elem, row, rowFrom, idx ) {
   var _self = this;
   var restrictionList = row.restrictions;
   var codelist = _.find(restrictionList, function(obj) { return obj.type == 'codelist'; });
   var regex    = _.find(restrictionList, function(obj) { return obj.type == 'regex'; });
   var script   = _.find(restrictionList, function(obj) { return obj.type == 'script'; });
   var required = _.find(restrictionList, function(obj) { return obj.type == 'required'; });

   var tableName = d3.select(elem.node().parentNode).datum().name;
   var compareRow = rowFrom;

   var differenceList = compareRow? _self.dictUtil.isDifferent2(row, compareRow) : [];

   elem.append("td").style("background-color", function() {
      if (!compareRow) return _self.colourNew;
      if (differenceList.length > 0) return _self.colourChanged;
      return null;
   });


   // Updates the datatype minimap
   var minimap = d3.select("#minimap").select("#minimap-"+tableName).append("rect").attr("x", 2).attr("y", 2+idx).attr("height", 1).attr("width", 20);
   if (!compareRow)  {
      minimap.style("fill", _self.colourNew);
      // entryNew ++;
   } else if (differenceList.length > 0) {
      minimap.style("fill", _self.colourChanged);
   } else {
      minimap.style("fill", _self.colourDefault);
   }


   elem.append("td").text(row.name);
   var attrBox = elem.append("td");
   if (row.controlled === true) {
      attrBox.append("div").text("Controlled");
   }
   if (required) {
      attrBox.append("div").text("Required");
      if (required.config.acceptMissingCode == true) {
         attrBox.append("div").text("NA-Code");
      }
   }

   elem.append("td").text(row.valueType);
   elem.append("td").style("max-width", "260px").text(row.label);

   elem.append("td").each(function(d) {
      d.expanded = false;
      var cell = d3.select(this);
      if (codelist) {
         d3.select(this).append("a")
           .text( codelist.config.name + " ")
           .on("click", function(d) {
              d.expanded = !d.expanded;
              if (d.expanded) {
                 d3.select(this).select("i").classed("glyphicon-chevron-down", false);
                 d3.select(this).select("i").classed("glyphicon-chevron-up", true);
                 cell.select("ul").style("display", "block");
              } else {
                 d3.select(this).select("i").classed("glyphicon-chevron-down", true);
                 d3.select(this).select("i").classed("glyphicon-chevron-up", false);
                 cell.select("ul").style("display", "none");
              }
           })
           .append("i")
           .classed("glyphicon", true)
           .classed("glyphicon-chevron-down", true)
           .style("position", "inherit"); // Not sure why this works, otherwise it overlap with svg

         var list = d3.select(this).append("ul").style("display", "none").classed("list-unstyled", true);
         var c = codelistMap[ codelist.config.name ];
         c.terms.forEach(function(term) {
            list.append("li").classed("data-type-list", true).text(term.code + "  " + term.value);
         });
      }
   });

   elem.append("td").style("max-width", "250px").each(function() {
      if (regex) {
         d3.select(this).append("p").classed("regex", true).text(regex.config.pattern);

         if (regex.config.examples) {
            var example = d3.select(this).append("pre")
              .style("width", "100%")
              .style("font-size", "0.8em")
              .style("cursor", "pointer")
              .on("click", function() {
                 var examples = [];
                 var baseURL = "http://www.regexplanet.com/advanced/java/index.html?";
                 baseURL = baseURL + "regex=" + encodeURIComponent(regex.config.pattern);

                 if (Array.isArray( regex.config.examples)) {
                    examples = regex.config.examples;
                 } else {
                    examples = regex.config.examples.split(", ");
                 }
                 examples.forEach(function(ex) {
                    baseURL += "&input=" + encodeURIComponent(ex);
                 });

                 window.open(baseURL, "_blank");
              })
              .append("code");

            example.append("p").text("Examples");
            example.append("a")
              .text(regex.config.examples);

              //.text(regex.config.examples);
         }
      }
   });

   elem.append("td").style("max-width",  "250px").each(function() {
      if (script) {
         var beautifiedScript = hljs.highlight("java", js_beautify( script.config.script )).value;
         d3.select(this).append("p").text(script.config.description);
         d3.select(this).append("pre").style("width", "100%").style("font-size", "0.8em").append("code").html(beautifiedScript);
      }
   });

   /*
   .style("box-shadow", function() {
      if (differenceList.indexOf("script") >= 0) {
         //return _self.colourChanged;
         return "inset 0 0 10px 0 " + _self.colourChanged;
      } else {
         return null;
      }
   });
   */
}


////////////////////////////////////////////////////////////////////////////////
// Data type filter, hide unmatched datatype tables
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.selectDataType = function(label) {

   d3.select("#minimapLabel").select("span").text("File Type: " + label+" ");
   d3.select("#minimapWrapper").style("display", "none");

   window.scrollTo(0, 0);

   d3.selectAll(".selection_wrapper").style("display", "block");

   // if (val === "all") {
   if (label === "all") {
      d3.selectAll(".selection_wrapper").style("display", "block");
   } else {
      d3.selectAll(".selection_wrapper").filter(function(section) {
         if (section.name === label) return 0;
         return 1;
      }).style("display", "none");
   }
}

////////////////////////////////////////////////////////////////////////////////
// Builds a mini-map row (one row per table)
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.buildFilterRow = function(grp, name, label, height) {
   var _self = this;
   grp.selectAll("rect")
      .data([{ cCurrent: _self.colourMinimapDefault}])
      .enter()
      .append("rect")
      .attr("width", 300).attr("height", (height-1)).style("fill", _self.colourMinimapDefault)
      .on("mouseover", function(d) { d3.select(this).style("fill", _self.colourMinimapSelect); })
      .on("mouseout", function(d) { d3.select(this).style("fill", d.cCurrent); })
      .on("click", function(d) {
         //_self.selectDataType(name);
         // d3.event.stopPropagation();

         _self.selectedDataType = name;
         _self.toggleDataTypeFunc();
      });

   grp.append("text")
      .attr("x", 30)
      .attr("y", 15)
      .attr("font-size", "0.9em")
      .style("pointer-events", "none")
      .text(name);
}


////////////////////////////////////////////////////////////////////////////////
// Search and filter dictionary
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.filter = function(txt) {
   var _self = this;
   var re = new RegExp(txt, "i");
   var datatypeMap = {};

   window.scrollTo(0, 0);

   d3.selectAll(".dictionary_table").selectAll("tr").style("display", "table-row");
   if (!txt || txt === '') {
      d3.selectAll(".dictionary_table").selectAll("tr").style("display", "table-row");
      d3.selectAll(".filter_wrapper").style("display", "block");
   } else {
      d3.selectAll(".dictionary_table").selectAll("tbody").selectAll("tr").filter(function(d) {

         if (_self.dictUtil.isMatch(d, re) === true) {
           var parentName = d3.select(d3.select(this).node().parentNode).datum().name;

           if (! datatypeMap[parentName]) {
              datatypeMap[parentName] = 1;
           } else {
              datatypeMap[parentName] ++;
           }
           return 0;
         }
         return 1;
      }).style("display", "none");

      d3.selectAll(".filter_wrapper").each(function(d) {
         d3.select(this).style("display", "block");
         //if (! datatypeMap[d.name] || datatypeMap[d.name] > d.fields.length) {
         if (! datatypeMap[d.name]) {
            d3.select(this).style("display", "none");
         }
      });
   }

   // Highlight corresponding entires in the minimap
   d3.select("#minimap")
     .selectAll("g")
     .each(function(d) {
        var datatype = d3.select(this).attr("id").split("-")[1];
        if (datatype in datatypeMap) {
           d3.select(this).select("rect").style("fill", function(d) {
              d.cCurrent = _self.colourHighlight;
              return d.cCurrent;
           });
        } else {
           d3.select(this).select("rect").style("fill", function(d) {
              d.cCurrent = _self.colourMinimapDefault;
              return d.cCurrent;
           });
        }
     });

   // Highlight the graph viewer
   d3.select("#graph").selectAll("circle").style("fill", null);
   d3.select("#graph").selectAll(".filter-indicator").style("opacity", 0);
   d3.select("#graph").selectAll("circle").filter(function(node) {
      if (!txt || txt === '') return false;
      if (!node) return false;

      var matches = _.filter(node.data.fields, function(field) {
         return _self.dictUtil.isMatch(field, re);
      });
      return matches.length > 0;
   }).style("fill", _self.colourHighlight);

   d3.select("#graph").selectAll(".filter-indicator").filter(function(field) {
      if (!txt || txt === '') return false;
      if (!field) return false;
      return _self.dictUtil.isMatch(field, re);
   }).style("opacity", 1.0);

}

////////////////////////////////////////////////////////////////////////////////
// Render the dictioary in a tree/graph layout
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.showDictionaryGraph = function(versionFrom, versionTo) {
   var _self = this;
   var dict = _self.dictUtil.getDictionary(versionTo);
   // console.log(_self, versionFrom, versionTo);
   window.scrollTo(0, 0);

   // Reset
   d3.select("#datatypeTable").transition().duration(300).style("opacity", 0.1).each("end", function() {
     d3.select("#datatypeTable").style("display", "none");
     d3.select("#datatypeSelector").style("visibility", "hidden");
     d3.select("#datatypeGraph").style("display", "block");
     d3.select("#datatypeGraph").transition().duration(400).style("opacity", 1.0);
   });


   // Clear
   d3.select("#graph").selectAll("*").remove();



   var svg = d3.select("#graph")
      .append("svg")
      .attr("viewBox", "0 0 1400 700")
      //.attr("viewBox", "0 0 " + window.screen.width + " " + window.screen.height)
      .attr("preserveAspectRatio", "xMinYMin")
      //.attr("width", 1500)
      //.attr("height", 700)
      .append("g")
      .attr("transform", "translate(40, 0)");


   var i = 0, duration = 750, root;

   // This is pivoted
   var graphHeight = 700;
   var graphWidth = 1400;

   var tree = d3.layout.tree().size([graphHeight, graphWidth]);
   var diagonal = d3.svg.diagonal().projection(function(d) { return [d.y, d.x]; });



   var list = _self.dictUtil.getParentRelation(dict);
   var queue = [];
   var level = 0;

   var root = {name:"donor"};
   var visited = {};
   visited["donor"] = 1;
   _self.dictUtil.buildTree2(root, list, visited, dict);


   root.x0 = 200;
   root.y0 = 0;

   // Toggle children on click.
   function click(d) {
     d3.event.stopPropagation();
     // order matters

     _self.selectedDataType = d.name;
     _self.toggleNodeFunc();
     // _self.selectDataType(d.name);
   }


   function update(source) {
     // Compute the new tree layout.
     var nodes = tree.nodes(root).reverse();
     var links = tree.links(nodes);

     // Normalize for fixed-depth.
     nodes.forEach(function(d) {
        if (d.depth === 1) d.y = 150;
        else d.y = d.depth * 220;


        // hardwired, make a bit more room
        var len = _.filter(nodes, function(n) {
          return n.depth === d.depth;
        });
        if (d.depth < 3 && len.length > 4) {
           d.x = d.x + (d.x - graphHeight/2)*0.8;
        }

        // check same level "children"
        var name = d.name;
        var relations =  _.filter(list, function(l) {
           return name === l.parentNode;
        });

        for (var idx=0; idx < relations.length; idx++) {
           var reln = relations[idx];
           var node = _.find(nodes, function(n) {
              return n.name === reln.node;
           });

           if (node && node.depth === d.depth) {
              d.y += 95;
              break;
           }
        }

     });

     // Update the nodes…
     var node = svg.selectAll("g.node")
         .data(nodes, function(d) { return d.id || (d.id = ++i); });


     // Enter any new nodes at the parent's previous position.
     var nodeEnter = node.enter().append("g")
         .attr("id", function(d) { return d.name; })
         .attr("class", "node")
         .attr("transform", function(d) {
            //var depth = d.depth;
            /*
            if (d.name === 'specimen') {
               console.log(d);
               console.log(_.filter(list, function(l) { return l.parentNode==='specimen'}));
            }
            if (d.children) {
               d.y += 60;
            }
            */
            return "translate(" + d.y + "," + d.x + ")";
         })
         .on("click", click)
         .on("mouseover", function(d) {
            d3.select(this).selectAll("text").style("font-weight", "bold");
            d3.select(this).selectAll("circle").style("stroke-width", 2);
         })
         .on("mouseout", function(d) {
            d3.select(this).selectAll("text").style("font-weight", null);
            d3.select(this).selectAll("circle").style("stroke-width", 1);
         });

     nodeEnter.append("circle")
         .attr("r", 6.5)
         .style("fill", function(d) { return d._children ? "lightsteelblue" : "#fff"; });

     nodeEnter.append("text")
         .attr("x", "10")
         .attr("dy", ".10em")
         .style("font-size", "1.05em")
         .text(function(d) { return d.name + " (" + d.data.fields.length + " Fields)"; })
         .style("fill-opacity", 1);


     nodeEnter.each(function(node) {
        var ref = d3.select(this);

        // This is just to act as a backdrop to trigger interaction more smoothly
        ref.append("rect")
           .classed("dummy", true)
           .attr("x", 10)
           .attr("y", 6)
           .attr("width", 4.0*node.data.fields.length)
           .attr("height", 10)
           .style("opacity", 0);

        ref.selectAll(".field-indicator")
           .data(node.data.fields)
           .enter()
           .append("rect")
           .classed("field-indicator", true)
           .attr("x", function(d, i) { return 10+4.0*i;})
           .attr("y", "6")
           .attr("height", 11)
           .attr("width", 2.0)
           .style("fill", function(field) {
              var fieldFrom = _self.dictUtil.getField(versionFrom, node.name, field.name);
              if (!fieldFrom) {
                 return _self.colourNew;
              } else if (fieldFrom && _self.dictUtil.isDifferent2(field, fieldFrom).length > 0) {
                 return _self.colourChanged;
              } else {
                 return "#DDDDEE";
              }
           })
           .style("opacity", function(field) {
              var fieldFrom = _self.dictUtil.getField(versionFrom, node.name, field.name);
              if (!fieldFrom) {
                 return 1.0;
              } else if (fieldFrom && _self.dictUtil.isDifferent2(field, fieldFrom).length > 0) {
                 return 1.0;
              } else {
                 return 0.6;
              }

           });


        ref.selectAll(".filter-indicator")
           .data(node.data.fields)
           .enter()
           .append("rect")
           .classed("filter-indicator", true)
           .attr("x", function(d, i) { return 10+4.0*i;})
           .attr("y", "17")
           .attr("height", 5)
           .attr("width", 2)
           .style("opacity", 0)
           .style("fill", function() {
              return _self.colourHighlight;
           });

        //ref.append("rect").attr("x", 10).attr("y", 18).attr("height", 1).attr("width", n.data.fields.length*3).style("fill", "#888888");
     });


     var links = [];
     list.forEach(function(rel) {
        if (rel.parentNode === null) return;
        var a = svg.select("#"+rel.node).datum();
        var b = svg.select("#"+rel.parentNode).datum();
        links.push({
           source: { x: a.x, y: a.y },
           target: { x: b.x, y: b.y },
        });
     });


     svg.insert("g", ":first-child").selectAll("path.link")
        .data(links)
        .enter()
        .append("path")
        .attr("class", "link")
        .attr("d", diagonal);


     // Stash the old positions for transition.
     nodes.forEach(function(d) {
       d.x0 = d.x;
       d.y0 = d.y;
     });
   }
   update(root);

   this.renderLegend(svg, 20, 30);
}


////////////////////////////////////////////////////////////////////////////////
// Legend
////////////////////////////////////////////////////////////////////////////////
TableViewer.prototype.renderLegend = function(svg, x, y) {
   var legend = svg.insert("g", ":first-child")
      .attr("font-size", "0.8em")
      .attr("transform", "translate(" + x + "," +  y + ")");

   legend.append("rect")
      .attr("x", -15)
      .attr("y", -15)
      .attr("width", 220)
      .attr("height", 40)
      .attr("stroke", "#666666")
      .attr("fill", "#FFFFFF");

   legend.append("circle")
      .attr("r", 6.5)
      .attr("fill", "#FFFFFF")
      .attr("stroke", "#666666");

   legend.append("text")
      .attr("x", 10)
      .attr("dy", ".10em")
      .text("meth_array_m");

   for(var i=0; i < 6; i++) {
      legend.append("rect")
         .attr("x", 10+4*i)
         .attr("y", "6")
         .attr("height", 11)
         .attr("width", 2)
         .style("fill", "#DDDDEE");
   }

   legend.append("text")
      .attr("x", 90)
      .attr("y", 0)
      .attr("fill", "#666666")
      .text("<--- FileType Name");

   legend.append("text")
      .attr("x", 90)
      .attr("y", 14)
      .attr("fill", "#666666")
      .text("<--- # Fields");
}

