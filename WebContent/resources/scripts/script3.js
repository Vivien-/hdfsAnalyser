var w = window,
    d = document,
    e = d.documentElement,
    g = d.getElementsByTagName('body')[0],
    x = w.innerWidth || e.clientWidth || g.clientWidth,
    y = w.innerHeight|| e.clientHeight|| g.clientHeight;
//put svg in the left side of the screen
x=x/2;
//number of layers including the center
var numberOfLayers = 7;
var margin = {top: y/30, right: x/30, bottom: y/30, left: x/30 },
	radius = Math.min(y - 2*margin.top, x - 2*margin.right, y - 2*margin.bottom, x - 2*margin.left)/(2*numberOfLayers);

var min_degree_arc_filter = 1;
var hue = d3.scale.category10();

function formatBytes(bytes,decimals) {
	if(bytes == 0) return '0 o';
	var k = 1000;
	var dm = decimals + 1 || 3;
	var sizes = ['o', 'Ko', 'Mo', 'Go', 'To', 'Po', 'Eo', 'Zo', 'Yo'];
	var i = Math.floor(Math.log(bytes) / Math.log(k));
	return (bytes / Math.pow(k, i)).toPrecision(dm) + ' ' + sizes[i];
}

var luminance = d3.scale.sqrt()
.domain([0, 1e6])
.clamp(true)
.range([90, 20]);

var tooltip = d3.select("body")
.append("div")
.attr("id", "tooltip")
.style("position", "absolute")
.style("z-index", "10")
.style("opacity", 0);

function mouseOverArc(d) {
	tooltip.html(d.name + "<br>" + formatBytes(d.value, 2));
	return tooltip.transition()
	.duration(50)
	.style("opacity", 0.9);
}

function mouseMoveArc (d) {
	return tooltip
	.style("top", (d3.event.pageY-10)+"px")
	.style("left", (d3.event.pageX+50)+"px");
}

function mouseOutArc(){
	return tooltip.style("opacity", 0);
}

var svg = d3.select("body").append("svg")
.attr("width", (numberOfLayers*2*radius) + "px")
.attr("height", (numberOfLayers*2*radius) + "px")
.style("position", "absolute")
.style("top",margin.top + "px")
.style("left",margin.left + "px")
.append("g")
.attr("transform", "translate(" + (numberOfLayers*radius) + "," + (numberOfLayers*radius) + ")");

var partition = d3.layout.partition()
.sort(function(a, b) { return d3.ascending(a.name, b.name); })
.size([2 * Math.PI, radius]);

var arc = d3.svg.arc()
.startAngle(function(d) { return d.x; })
.endAngle(function(d) { return d.x + d.dx ; })
.padAngle(.05) 
.padRadius(radius/3)
.innerRadius(function(d) { return radius* d.depth; })
.outerRadius(function(d) { return radius * (d.depth + 1) - 1; });

var explore = $('#infos').css("height");

var start = new Date().getTime();
d3.json("/HadoopAnalyser/FileContent", function(error, root) {
	$("#wait").hide();
	if(error) {
		$("#error").show();
	} else {
		var end = new Date().getTime();
		$("#time").text("hdfs fetched in " + (end-start)/1000 + "s");		
	}
	// Compute the initial layout on the entire tree to sum sizes.
	// Also compute the full name and fill color for each node,
	// and stash the children so they can be restored as we descend.
	partition
	.value(function(d) { return d.size; })
	.nodes(root)
	.forEach(function(d) {
		d._children = d.children;
		d.sum = d.value;
		d.key = key(d);
		d.fill = fill(d);
	});
	// Now redefine the value function to use the previously-computed sum.
	partition
	.children(function(d, depth) { return depth < numberOfLayers-1 ? d._children : null; })
	.value(function(d) { return d.sum; });

	var center = svg.append("circle")
	.attr("r", radius)
	.on("click", zoomOut);

	center.append("title").text("zoom out");
	
	$("#center").text(formatBytes(root.value, 2));
	
	var children_sorted = root.children.sort(function(a,b){
		if(a.value > b.value)
			return -1;
		else if (a.value < b.value)
			return 1;
		else 
			return 0;
	})

	for(var i = 0; i < children_sorted.length; i++){
		var child = children_sorted[i];
		var col = d3.select(child)[0][0].fill.toString();
		$("#infos").append("<div><figure class='circle' style='background: " + col + "'></figure><span class='info' style='color: " + col + "'> " + child.name + "</span><span class='right' style='color: white'> " + formatBytes(child.value,2) + "</span></div><div style='clear:both;'></div>");
	}
	
	var path = svg.selectAll("path")
	.data(partition.nodes(root).slice(1))
	.enter().append("path")
	.filter(function(d) { return (Math.abs(d.x - (d.x + d.dx)) > min_degree_arc_filter *(Math.PI)/180); })
	.attr("d", arc)
	.style("fill", function(d) { return d.fill; })
	.on("mouseover", mouseOverArc)
	.on("mousemove", mouseMoveArc)
	.on("mouseout", mouseOutArc)
	.each(function(d) { this._current = updateArc(d); })
	.on("click", zoomIn);

	function zoomIn(p) {
		if (p.depth > 1) p = p.parent;
		if (!p.children) return;
		zoom(p, p);
	}

	function zoomOut(p) {
		if(typeof p === "undefined") return;
		if (!p.parent) return;
		zoom(p.parent, p);
	}
	
	var end2 = new Date().getTime();
	$("#time").append("<br>visualization in " + ((end2-end)/1000) + "s");
	// Zoom to the specified new root.
	function zoom(root, p) {
		if (document.documentElement.__transition__) return;
		// Rescale outside angles to match the new layout.
		var enterArc,
			exitArc,
			outsideAngle = d3.scale.linear().domain([0, 2 * Math.PI]);

		function insideArc(d) {
			return p.key > d.key
			? {depth: d.depth - 1, x: 0, dx: 0} : p.key < d.key
					? {depth: d.depth - 1, x: 2 * Math.PI, dx: 0}
			: {depth: 0, x: 0, dx: 2 * Math.PI};
		}

		$("#path").empty();
		var current_dir = root;
		var path_dir = '';

		while(current_dir.parent != null){
			var col = current_dir.fill.toString();
			path_dir += '<span class="path_element" style="background-color: ' + col + '">' + current_dir.name+'</span>/$#';
			current_dir = current_dir.parent;
		}
		path_dir += '<span class="path_element" style="background-color: #cccccc">/</span>/$#';
		$("#path").html(path_dir.split("/$#").reverse().join(""));
		
		function outsideArc(d) {
			return {depth: d.depth + 1, x: outsideAngle(d.x), dx: outsideAngle(d.x + d.dx) - outsideAngle(d.x)};
		}

	
		
		center.datum(root);
		var val = 0;
		
		var children_sorted = root.children.sort(function(a,b){
			if(a.value > b.value)
				return -1;
			else if (a.value < b.value)
				return 1;
			else 
				return 0;
		})
		
		$("#infos").empty();
		
		for(var i = 0; i < children_sorted.length; i++){
			val += root.children[i].value;
			var child = children_sorted[i];
			var col = d3.select(child)[0][0].fill.toString();
			$("#infos").append("<div><figure class='circle' style='background: " + col + "'></figure><span class='info' style='color: "+ col +"'>" + child.name + "</span><span class='right' style='color: white'> " + formatBytes(child.value,2) + "</span></div><div style='clear:both;'></div>");
		}
		$("#center").text(formatBytes(val,2));

		$("#infos").css("top", parseInt($("#path").css("top"), 10) + parseInt($("#path").height(),10) + 5);
		
		// When zooming in, arcs enter from the outside and exit to the inside.
		// Entering outside arcs start from the old layout.
		if (root === p) enterArc = outsideArc, exitArc = insideArc, outsideAngle.range([p.x, p.x + p.dx]);

		path = path.data(partition.nodes(root).slice(1), function(d) { return d.key; });

		// When zooming out, arcs enter from the inside and exit to the outside.
		// Exiting outside arcs transition to the new layout.
		if (root !== p) enterArc = insideArc, exitArc = outsideArc, outsideAngle.range([p.x, p.x + p.dx]);

		d3.transition().duration(750).each(function() {
			path.exit().transition()
			.style("fill-opacity", function(d) { return d.depth === 1 + (root === p) ? 1 : 0; })
			.attrTween("d", function(d) { return arcTween.call(this, exitArc(d)); })
			.remove();

			path.enter()
			.append("path")
			.style("fill-opacity", function(d) { return d.depth === 2 - (root === p) ? 1 : 0; })
			.style("fill", function(d) { return d.fill; })
			.on("click", zoomIn)
			.on("mouseover", mouseOverArc)
			.on("mousemove", mouseMoveArc)
			.on("mouseout", mouseOutArc)
			.each(function(d) { this._current = enterArc(d); });

			path.transition()
			.style("fill-opacity", 1)
			.attrTween("d", function(d) { return arcTween.call(this, updateArc(d)); });
		});

		$(".path_element").click(function(){
			var current_elem = root;
			for(var i = 0; i < $(this).nextAll().length; i++)
				current_elem = current_elem.parent;
			zoomIn(current_elem);	
		});
		
	}
});

function key(d) {
	var k = [], p = d;
	while (p.depth) k.push(p.name), p = p.parent;
	return k.reverse().join(".");
}

function nextColor(color) {
	return colors[(colors.indexOf(color) + 1) % (colors.length)];
}

function fill(d) {
	var p = d;
	while (p.depth > 1) p = p.parent;
	var c = d3.lab(hue(p.name));
	c.l = luminance(50000 * d.depth);
	return c;
}

function arcTween(b) {
	var i = d3.interpolate(this._current, b);
	this._current = i(0);
	return function(t) {
		return arc(i(t));
	};
}

function updateArc(d) {
	return {depth: d.depth, x: d.x, dx: d.dx};
}

d3.select(self.frameElement).style("height", margin.top + margin.bottom + "px");
$("svg").hover(function(){
	tooltip.style("opacity", 0);
});
