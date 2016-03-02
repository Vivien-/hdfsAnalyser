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
