(function(d3) {
	y = y/2;
	var hue = d3.scale.category10();
	var this_radius;
	var marg;
	if(x/2 - 2*margin.left > y-2*margin.top){
		this_radius = y - 2*margin.top;
		marg = margin.top;
	}
	else{
		this_radius = x/2 - 2*margin.left;
		marg = margin.left;
	}
	function httpGetAsync(theUrl, callback) {
		var xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange = function() { 
			if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
				callback(xmlHttp.responseText);
		}
		xmlHttp.open("GET", theUrl, true); // true for asynchronous which we want
		xmlHttp.send(null);
	}
	
//	
	function dostuff(json) {
		var obj = JSON.parse(json);
		var dataset = [{ label: 'Used space', count: obj.summary[0].used}, 
		               { label: 'Free space', count: obj.summary[0].unused}];
		//4 times smaller than sunburst
		//$("#replication").html("Replication factor : "+obj.replication+"</br>"+"Number of datanodes : "+(obj.summary.length-1));
		var vis = d3.select("#chart")
		.append("svg:svg") //create the SVG element inside the <body>
		.data([dataset]) //associate our data with the document
		.attr("width", this_radius + "px") //set the width of the canvas
		.attr("height", this_radius + "px")//set the height of the canvas
		.style("position", "absolute")
		.style("top",  (margin.top) + "px")
		.style("left", (x + marg) + "px")
		.append("svg:g") //make a group to hold our pie chart
		.attr('transform', 'translate(' + (this_radius/2)+ ',' + (this_radius/2) + ')');

		var arc = d3.svg.arc()
		.outerRadius(this_radius/2);

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
		
		$("#infoSize").append("</br><div style='color:white;'>Replication factor : "+obj.replication+"</br>"+"Number of datanodes : "+(obj.summary.length-1)+"</div>")
		console.log($("#infoSize").height());
		$("#infoSize")
		.css("position", "absolute")
		.css("left", (3*x/2 + margin.left) + "px")
		.css("top", ((y - $("#infoSize").height())/2) + "px")
		.css("z-index", 10);
//		for(var i = 0; i < 16; i++){
//			obj.summary[i+2] = obj.summary[1];
//		}
//		
//		for(var i = 1; i < obj.summary.length; i++) {
//			createWrapper();
//		}
//		for(var i = 1; i < obj.summary.length; i++) {
//			displayDatanode(obj.summary[i], i);
//		}
	}

	httpGetAsync("/HadoopAnalyser/DiskUsage", dostuff)
})(window.d3);