/**
 * 
 */


//Getting window's height and width
var w = window,
d = document,
e = d.documentElement,
g = d.getElementsByTagName('body')[0],
x = w.innerWidth || e.clientWidth || g.clientWidth,
y = w.innerHeight|| e.clientHeight|| g.clientHeight;
var color = d3.scale.category10();
var margin = 20;
var topButtonsMargin = $("#button").height();
var gifSize = 100
var width = 2*x/3-2*margin;
var height = y-(topButtonsMargin  + 2*margin);
var radius = Math.min(width, height) / 2;
$("#waitChartTables")
.css("position", "absolute")
.css("left", ((2*x/3-gifSize)/2) + "px")
.css("top", (y-gifSize - topButtonsMargin)/2 + "px");
$("#tables")
.css("position", "absolute")
.css("left", (2*x/3 + margin) + "px")
.css("top", topButtonsMargin + "px")
.css("width", (x/3 - 2*margin)+ "px")
.css("height", (y - topButtonsMargin) + "px");
$("#chartTables")
.css("position", "absolute")
.css("left", (2*x/3 - 2*radius)/2 + "px")
.css("top", ((y - topButtonsMargin - 2*radius)/2 + topButtonsMargin) + "px")
.css("width", 2*radius+ "px")
.css("height", 2*radius + "px");

(function(d3) {
	'use strict';

	var dataset = [
	               { label: 'Abulia', count: 10 }, 
	               { label: 'Betelgeuse', count: 20 },
	               { label: 'Cantaloupe', count: 30 },
	               { label: 'Dijkstra', count: 40 }
	               ];

	function formatBytes(bytes,decimals) {
		if(bytes == 0) return '0 o';
		var k = 1000;
		var dm = decimals + 1 || 3;
		var sizes = ['o', 'Ko', 'Mo', 'Go', 'To', 'Po', 'Eo', 'Zo', 'Yo'];
		var i = Math.floor(Math.log(bytes) / Math.log(k));
		return (bytes / Math.pow(k, i)).toPrecision(dm) + ' ' + sizes[i];
	}



	function httpGetAsync(theUrl, callback1, callback2) {
		$("#chartTables").empty();
		$("#waitChartTables").show();
		var xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange = function() { 
			if (xmlHttp.readyState == 4 && xmlHttp.status == 200){
				$("#waitChartTables").hide();
				callback1(xmlHttp.responseText);
				callback2(xmlHttp.responseText);
			} else if(xmlHttp.readyState == 4 && xmlHttp.status != 200){
				$("#waitChartTables").hide();
				alert("Status "+xmlHttp.status+" : Can't get Hbase data, possible solution : \n - Check that Hbase is Running \n - Set the HBASE_CONF environment variable to the absolute path of your hadoop hbase-site.xml in your ~/.bashrc and then source ~/.bashrc");
				return;
			}
		}
		xmlHttp.open("GET", theUrl, true); // true for asynchronous which we want
		xmlHttp.send(null);
	}



	function drawPie(data){
		var json = JSON.parse(data);
		var dataset = json.tbls;
		var svg = d3.select('#chartTables')
		.append('svg')
		.attr('width', 2*radius)
		.attr('height', 2*radius)
		.append('g')
		.attr('transform', 'translate(' + radius + 
				',' + radius + ')');

		var arc = d3.svg.arc()
		.outerRadius(radius);

		var pie = d3.layout.pie()
		.value(function(d) { return d.size; })
		.sort(null);

		var tip = d3.tip()
		.attr('class', 'd3-tip')
		.offset([y/6, 0])
		.html(function(d) {
			return d.data.name + "<br>" + "<span style='color:orangered'>" + formatBytes(d.data.size, 2) + "</span>";
		});

		svg.call(tip);

		var path = svg.selectAll('path')
		.data(pie(dataset))
		.enter()
		.append('path')
		.attr('d', arc)
		.attr('fill', function(d, i) { 
			return color(d.data.name);
		})
		.on("mouseover", tip.show)
		.on("mouseout", tip.hide);;
	}

	function fillTable(data){
		var json = JSON.parse(data);
		console.log(json.tbls);
		json.tbls.sort(function(a, b) {
		    return parseInt(b.size) - parseInt(a.size);
		});
		for(var i = 0; i<json.tbls.length; i++){
			var col = color(json.tbls[i].name);
			$("#databases_tbody").append("<tr class='db-info db-onclick' id='"+json.tbls[i].name+"'><td class='lalign' style='color: " + col + ";'>" + json.tbls[i].name + "</td><td style='color: " + col + ";'>" + json.tbls[i].location + "</td> <td style='color: " + col + ";'> " + formatBytes(json.tbls[i].size,2) + "</td>");
		}
	}

	httpGetAsync("/HadoopAnalyser/HbaseTables", drawPie, fillTable);

  })(window.d3);