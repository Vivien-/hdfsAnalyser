
//Getting window's height and width
var w = window,
d = document,
e = d.documentElement,
g = d.getElementsByTagName('body')[0],
x = w.innerWidth || e.clientWidth || g.clientWidth,
y = w.innerHeight|| e.clientHeight|| g.clientHeight;




$("#waitChartDatabases")
.css("position", "absolute")
.css("left", (x/4 - 64 + 10) + "px")
.css("top", (y/4 - 64 + 10) + "px");

$("#waitChartTables")
.css("position", "absolute")
.css("left", (3*x/4 - 64 + 10) + "px")
.css("top", (y/4 - 64 + 10) + "px");

$("#databases")
.css("position", "absolute")
.css("left", (10) + "px")
.css("top", (y/2 + 10) + "px")
.css("width", (x/2 - 50)+ "px")
.css("height", (y/2 - 10) + "px");

$("#externals")
.css("position", "absolute")
.css("left", (x/2 + 10) + "px")
.css("top", (y/2 + 10) + "px")
.css("width", (x/2 - 50)+ "px")
.css("height", (y/4 - 10) + "px");


$("#internals")
.css("position", "absolute")
.css("left", (x/2 + 10) + "px")
.css("top", (y/2 + y/4 + 10) + "px")
.css("width", (x/2 - 50)+ "px")
.css("height", (y/4 - 10) + "px");


var color = d3.scale.category10();

(function(d3) {
	'use strict';

	//Function for asynchronous call
	function httpGetAsync(theUrl, callback1, callback2) {
		var xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange = function() { 
			if (xmlHttp.readyState == 4 && xmlHttp.status == 200){
				if(typeof callback2 != 'undefined') {
					json_g = JSON.parse(xmlHttp.responseText);
					callback1(xmlHttp.responseText, {"x": x/2-20, "y": y/2-20});
					callback2(xmlHttp.responseText);
				} else
					callback1(xmlHttp.responseText, {"x": x/2-20, "y": y/2-20});
			}

		}
		xmlHttp.open("GET", theUrl, true); // true for asynchronous which we want
		xmlHttp.send(null);
	}

	//global variable containing the tables json for each database (so we don't have to send a request twice for the same database)
	var dbInfo = {};
	var hasCreatedDB = false;
	var firstLoad = true;
	var json_g;
	
	function formatBytes(bytes,decimals) {
		if(bytes == 0) return '0 o';
		var k = 1000;
		var dm = decimals + 1 || 3;
		var sizes = ['o', 'Ko', 'Mo', 'Go', 'To', 'Po', 'Eo', 'Zo', 'Yo'];
		var i = Math.floor(Math.log(bytes) / Math.log(k));
		return (bytes / Math.pow(k, i)).toPrecision(dm) + ' ' + sizes[i];
	}

	//Function to draw databases pie (top left)
	function drawDatabasesPie(json, size){
		json = JSON.parse(json);
		var data;
		var targetWaiter = "#waitChartDatabases";
		var target = "#chartDatabases";
		if(hasCreatedDB) {
			targetWaiter = "#waitChartTables";
			target = "#chartTables";
			data = json.tbls;
			$(target).empty();
		} else {
			hasCreatedDB = true;
			data = json.dbs;
		}
				
		$(targetWaiter).hide();
		$(target)
		.css("width", width)
		.css("display", "inline-block");
		var width = size.x;
		var height = size.y;
		var radius = Math.min(width, height) / 2;

		var svg = d3.select(target)
		.append('svg')
		.attr('width', width + 10)
		.attr('height', height + 10)
		.append('g')
		.attr('transform', 'translate(' + (width / 2 + 10) + 
				',' + (height / 2 + 10) + ')');

		var arc = d3.svg.arc()
		.outerRadius(radius);

		var pie = d3.layout.pie()
		.value(function(d) { return d.count; })
		.sort(null);

		var tip = d3.tip()
		.attr('class', 'd3-tip')
		.offset([y/6, 0])
		.html(function(d) {
			return d.data.label + "<br>" + "<span style='color:orangered'>" + formatBytes(d.data.count, 2) + "</span>";
		});

		svg.call(tip);

		var path = svg.selectAll('path')
		.data(pie(data))
		.enter()
		.append('path')
		.attr('d', arc)
		.attr('fill', function(d, i) { 
			return color(d.data.label);
		})
		.on("mouseover", tip.show)
		.on("mouseout", tip.hide);
		
		if(firstLoad) {
			httpGetAsync("/HadoopAnalyser/Tables?database="+json_g.dbs[0].label, getDBInfoCallBack);
			firstLoad = false;
		}
	}

	function getDBInfoCallBack(json, size) {
		var tmp = JSON.parse(json);
		dbInfo[tmp.database] = json;
		drawDatabasesPie(dbInfo[tmp.database], size);
		appendTablesInfo(tmp);
	}
	
	function appendTablesInfo(json) {
		$("#externals_tbody").empty();
		$("#internals_tbody").empty();
		for(var i = 0; i<json.tbls.length; i++){
			if(json.tbls[i].type == "EXTERNAL_TABLE"){
				var col = color(json.tbls[i].label);
				$("#externals_tbody").append("<tr class='db-info' id='"+json.tbls[i].label+"'><td class='lalign' style='color: " + col + ";'>" + json.tbls[i].label + "</td><td style='color: " + col + ";'>" + json.tbls[i].location + "</td> <td style='color: " + col + ";'> " + formatBytes(json.tbls[i].count,2) + "</td>");
			}
		}
		for(var i = 0; i<json.tbls.length; i++){
			if(json.tbls[i].type == "MANAGED_TABLE"){
				var col = color(json.tbls[i].label);
				$("#internals_tbody").append("<tr class='db-info' id='"+json.tbls[i].label+"'><td class='lalign' style='color: " + col + ";'>" + json.tbls[i].label + "</td><td style='color: " + col + ";'>" + json.tbls[i].location + "</td> <td style='color: " + col + ";'> " + formatBytes(json.tbls[i].count,2) + "</td>");
			}
		}
		$(function() {
			$('.table-sort').tablesorter();
		});
	}
	
	function f2(json){
		json = JSON.parse(json);
		for(var i = 0; i<json.dbs.length; i++){
			var col = color(json.dbs[i].label);
			$("#databases_tbody").append("<tr class='db-info' id='"+json.dbs[i].label+"'><td class='lalign' style='color: " + col + ";'>" + json.dbs[i].label + "</td><td style='color: " + col + ";'>" + json.dbs[i].location + "</td> <td style='color: " + col + ";'> " + formatBytes(json.dbs[i].count,2) + "</td>");
		}
		$(".db-info").click(function(e) {
			var name = e.currentTarget.id;
			console.log(e.currentTarget);
			console.log(name);
			//No need to send a request
			if(typeof dbInfo[name] != 'undefined') {
				getDBInfoCallBack(dbInfo[name], {"x": x/2-20, "y": y/2-20});
			} // we need to send a request
			else {
				httpGetAsync("/HadoopAnalyser/Tables?database="+name, getDBInfoCallBack);				
			}
		})
	}
	httpGetAsync("/HadoopAnalyser/Databases", drawDatabasesPie, f2);
	$(function() {
		$('.table-sort').tablesorter();
	});
})(window.d3);