$("#wait")
.css("position", "absolute")
.css("left", (numberOfLayers*radius+margin.left - 64) + "px")
.css("top", (numberOfLayers*radius+margin.top - 64) + "px");

$("#center")
.css("position", "absolute")
.css("left", (numberOfLayers*radius+margin.left - 28) + "px")
.css("top", (numberOfLayers*radius+margin.top - 9) + "px")
.css("z-index", 10);

$("#error")
.css("position", "absolute")
.css("left", (numberOfLayers*radius+margin.left - 28) + "px")
.css("top", (numberOfLayers*radius+margin.top - 9) + "px")
.css("z-index", 10);

$("#border")
.css("left", x);

var this_radius = Math.min((x/2-2*margin.left),(y/2-2*margin.top))/2;

$("#infoSize")
.css("left", (x + radius*2 - 20) + "px")
.css("top", (margin.top + this_radius*2) + "px")
.css("z-index", 10);

$("#datanodes")
.css("position", "absolute")
.css("top",  (margin.top/2) + "px")
.css("left", (3*x/2 - margin.right) + "px")
.css("width", (numberOfLayers*radius + 36) + "px")
.css("height", (numberOfLayers*radius) + "px");

$("#path")
.css("left", (x + margin.left) + "px") 
.css("top", (y/2 + margin.top/2) + "px")
.css("height", "auto !important")
.css("z-index", 10)
.css("max-width", (x - 2*margin.left) + "px")
.html('<span class="path_element" style="background-color: #cccccc"">/</span>');

$("#infos")
.css("position", "absolute")
.css("left", (x + margin.left) + "px")
.css("top", (parseInt($("#path").css("top"), 10) + parseInt($("#path").height(),10) + 15 ) + "px")
.css("z-index", 10)
.css("width", (x - 2*margin.left) + "px")
.css("height", (y/2 - 2*margin.top) + "px");

