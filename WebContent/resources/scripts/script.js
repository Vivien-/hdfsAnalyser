(function(d3) {
	y = y/2;
	var hue = d3.scale.category10();
	var this_radius = Math.min((x/2 - margin.left),(y/2 - margin.top))/2;

	function httpGetAsync(theUrl, callback) {
		var xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange = function() { 
			if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
				callback(xmlHttp.responseText);
		}
		xmlHttp.open("GET", theUrl, true); // true for asynchronous which we want
		xmlHttp.send(null);
	}
	
	var wrapper_width = numberOfLayers*radius/3;
	
	function displayDatanode(data, i) {
		var $wrapper = $('#datanodes').children().eq(i-1);
		$wrapper.children().eq(0).text(data.name);
		var dataset = [{ label: 'Used space', count: data.used, percentage: data.percentage}, 
		               { label: 'Free space', count: data.unused, percentage: (100-data.percentage)}
						];
		
		
		var radius = this_radius/3;
		var vis = d3.select($wrapper[0])
		.append("svg:svg") //create the SVG element inside the <body>
		.data([dataset]) //associate our data with the document
		.attr("width", wrapper_width + "px") //set the width of the canvas
		.attr("height", wrapper_width + "px") //set the height of the canvas
		.append("svg:g") //make a group to hold our pie chart
		.attr('transform', 'translate(' + (wrapper_width/2)+ ',' + (wrapper_width/2) + ')');

		var arc = d3.svg.arc()
		.outerRadius(2*radius)
	    .innerRadius(2*radius - 30);

		var pie = d3.layout.pie() //this will create arc data for us given a list of values
		.value(function(d) { return d.count; }); // Binding each value to the pie

		var arcs = vis.selectAll("g.slice")
		.data(pie)
		.enter()
		.append("svg:g")
		.attr("class", "slice");    //allow us to style things in the slices (like text)

		arcs.append("svg:path")
		.attr("fill", function(d, i) { return hue(i); } )
		.attr("d", arc);
	}
	
	function createWrapper() {
		$datanodes = $("#datanodes");
		$datanodes.append("<div>");
		
		$last = $("#datanodes div:last-child").last();
		
		$last
		.css("width", wrapper_width)
		.css("height", wrapper_width + 15)
 		.css("margin","1px")
		.css("display", "inline-block");
		
		$last.append("<span>");
		$last.children().eq(0).css("width", wrapper_width).css("height", 15);
	}
	
	function dostuff(json) {
		var obj = JSON.parse(json);
		var dataset = [{ label: 'Used space', count: obj.summary[0].used}, 
		               { label: 'Free space', count: obj.summary[0].unused}];
		//4 times smaller than sunburst
		$("#replication").html("Replication factor : "+obj.replication+"</br>"+"Number of datanodes : "+(obj.summary.length-1));
		var vis = d3.select("#chart")
		.append("svg:svg") //create the SVG element inside the <body>
		.data([dataset]) //associate our data with the document
		.attr("width", (numberOfLayers*radius - margin.right) + "px") //set the width of the canvas
		.attr("height", (numberOfLayers*radius - margin.right) + "px")//set the height of the canvas
		.style("position", "absolute")
		.style("top",  (margin.top/2) + "px")
		.style("left", x + "px")
		.append("svg:g") //make a group to hold our pie chart
		.attr('transform', 'translate(' + (numberOfLayers*radius/2)+ ',' + (numberOfLayers*radius/2) + ')');

		var arc = d3.svg.arc()
		.outerRadius(this_radius*2 - margin.right);

		var pie = d3.layout.pie() //this will create arc data for us given a list of values
		.value(function(d) { return d.count; }); // Binding each value to the pie

		var arcs = vis.selectAll("g.slice")
		.data(pie)
		.enter()
		.append("svg:g")
		.attr("class", "slice");    //allow us to style things in the slices (like text)

		arcs.append("svg:path")
		.attr("fill", function(d, i) { return hue(i); } )
		.attr("d", arc);

		for(var i = 0; i < dataset.length; i++) {
			$("#infoSize").append("<div><figure class='circle' style='background: " + hue(i) + "'></figure><span class='info' style='color: "+ hue(i) +"'>" + dataset[i].label + " &nbsp&nbsp</span><span class='right' style='color: white'> " + formatBytes(dataset[i].count,2) + "</span></div><div style='clear:both;'></div>");
		}
		
//		for(var i = 0; i < 8; i++){
//			obj.summary[i+2] = obj.summary[1];
//		}
		
		for(var i = 1; i < obj.summary.length; i++) {
			createWrapper();
		}
		for(var i = 1; i < obj.summary.length; i++) {
			displayDatanode(obj.summary[i], i);
		}
	}

	httpGetAsync("/HadoopAnalyser/DiskUsage", dostuff)
})(window.d3);