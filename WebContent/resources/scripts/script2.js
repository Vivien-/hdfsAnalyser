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



$("#datanodes")
.css("position", "absolute")
.css("top",  (margin.top/2) + "px")
.css("left", (3*x/2 - margin.right) + "px")
.css("width", (numberOfLayers*radius + 36) + "px")
.css("height", (numberOfLayers*radius) + "px");

$("#replication")
.css("position", "absolute")
.css("top",  (margin.top/2 + (numberOfLayers*radius -10)) + "px")
.css("left", (3*x/2 - margin.right) + "px");

$("#path")
.css("left", (x + margin.left) + "px") 
.css("top", (y/2 + margin.top/2) + "px")
.css("height", "auto !important")
.css("z-index", 10)
.css("max-width", (x - 2*margin.left) + "px");


$("#switch")
.css("top",0)
.css("left",0);


$("#details")
.css("top",0)
.css("left",x/6);


$("#time")
.css("top",y-20)
.css("left",0);

