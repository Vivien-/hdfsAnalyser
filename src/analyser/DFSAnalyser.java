package analyser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class DFSAnalyser {

	private String url;

	public DFSAnalyser(String url){
		this.url = url;
	}

	private int indexInArray(JsonArray array, String id){
		for(int i = 0; i < array.size(); i++){
			JsonObject tmp = (JsonObject)array.get(i);
			if(tmp.get("name").getAsString().equals(id))
				return i;
		}
		return -1;
	}

	public String diskUsage() throws IOException, URISyntaxException{
		JsonObject json = new JsonObject();
		JsonObject global = new JsonObject();
		Configuration configuration = new Configuration();
		DistributedFileSystem hdfs;
		hdfs = (DistributedFileSystem) FileSystem.get(new URI(url), configuration);
		DatanodeInfo[] dataNodes = hdfs.getDataNodeStats();
		json.add("summary", new JsonArray());
		global.addProperty("used", hdfs.getContentSummary(new Path("/")).getSpaceConsumed());
		global.addProperty("unused", hdfs.getStatus().getRemaining());
		json.get("summary").getAsJsonArray().add(global);
		for(int i = 0; i < dataNodes.length; i++){
			JsonObject current = new JsonObject();
			current.addProperty("name", dataNodes[i].getName());
			current.addProperty("used", dataNodes[i].getDfsUsed());
			current.addProperty("unused", dataNodes[i].getCapacity());
			current.addProperty("percentage", dataNodes[i].getDfsUsedPercent());
			current.addProperty("nondfs", dataNodes[i].getNonDfsUsed());
			json.get("summary").getAsJsonArray().add(current);
		}
		json.addProperty("replication", hdfs.getFileStatus(new Path("/")).getReplication() + 1);
		return json.toString();
	}

	public String jsonify(TreeMap<String,Map<String, Long>> treemap) {
		JsonObject json = new JsonObject();
		for(Map.Entry<String,Map<String,Long>> entry : treemap.entrySet()) {
			String key = entry.getKey();
			String[] tokens = key.split("/");

			//create the root node
			if(json.get("name") == null || !json.get("name").getAsString().equals(tokens[0])) {
				json.addProperty("name", tokens[0]);
				json.add("children", new JsonArray());
			}
			//the rest of the tree hierarchy without leaves (files)
			JsonObject next = json;
			for(int i = 1; i < tokens.length; i++ ){
				if(next.get("children") == null)
					next.add("children",new JsonArray());
				int index = indexInArray(next.get("children").getAsJsonArray(), tokens[i]);
				if(index == -1) {
					JsonObject tmp = new JsonObject();
					tmp.addProperty("name", tokens[i]);
					tmp.add("children", new JsonArray());
					next.get("children").getAsJsonArray().add(tmp);
					index = indexInArray(next.get("children").getAsJsonArray(), tokens[i]);
				}
				next = (JsonObject) next.get("children").getAsJsonArray().get(index);
			}

			//now adding the files
			Map<String, Long> files = entry.getValue();
			for(Map.Entry<String,Long> file : files.entrySet()) {
				JsonObject tmp = new JsonObject();
				tmp.addProperty("name", file.getKey());
				tmp.addProperty("size", file.getValue());
				next.get("children").getAsJsonArray().add(tmp);
			}
		}
		return json.toString();
	}

	public TreeMap<String,Map<String, Long>> getHDFSContent(String directory) throws IllegalArgumentException, IOException, URISyntaxException{
		TreeMap<String,Map<String, Long>> structure = new TreeMap<String, Map<String, Long>>();
		Configuration configuration = new Configuration();
		FileSystem hdfs;
		hdfs = FileSystem.get(new URI(url), configuration);
		RemoteIterator<LocatedFileStatus> it = hdfs.listFiles(new Path(url+directory), true);
		while(it.hasNext())
		{
			LocatedFileStatus next = it.next();
			String path = next.getPath().toString();
			String name = next.getPath().getName();
			Long size = next.getLen();
			String parentPath = path.substring(0, path.lastIndexOf("/"));
			parentPath = parentPath.replace(url, "");
			if(structure.get(parentPath) == null)
				structure.put(parentPath, new HashMap<String,Long>());
			structure.get(parentPath).put(name, size);
		}
		return structure;
	}
}
