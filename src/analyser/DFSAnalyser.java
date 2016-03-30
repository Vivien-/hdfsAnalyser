package analyser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.thrift.TException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class DFSAnalyser {

	private String url;

	public DFSAnalyser(/*String url*/){
		//this.url = url;
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
		String cf = System.getenv("HADOOP_CONF");
		System.out.println(cf);
		Path p = new Path(cf);
		Configuration configuration = new Configuration(true);
		configuration.addResource(p);
		DistributedFileSystem hdfs = new DistributedFileSystem();
		FileSystem fs = FileSystem.get(/*new URI(url),*/ configuration);
		hdfs = (DistributedFileSystem) fs;
		hdfs.setConf(configuration);
		DatanodeInfo[] dataNodes = hdfs.getDataNodeStats(DatanodeReportType.ALL);
		json.add("summary", new JsonArray());
		global.addProperty("used", hdfs.getStatus().getUsed());
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
		boolean first = true;
		JsonObject json = new JsonObject();
		JsonObject json_f = new JsonObject();
		json_f.addProperty("name", "/");
		json_f.add("children", new JsonArray());
		for(Map.Entry<String,Map<String,Long>> entry : treemap.entrySet()) {
			String key = entry.getKey();
			String[] tokens = key.split("/");

			//create the root node
			if(json.get("name") == null || !json.get("name").getAsString().equals(tokens[0])) {
				if(!first)
					json_f.get("children").getAsJsonArray().add(json);
				else 
					first = false;
				json = new JsonObject();
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
		json_f.get("children").getAsJsonArray().add(json);
		return json_f.toString();
	}

	public TreeMap<String,Map<String, Long>> getHDFSContent(/*String directory*/) throws IllegalArgumentException, IOException, URISyntaxException{
		TreeMap<String,Map<String, Long>> structure = new TreeMap<String, Map<String, Long>>();
		String cf = System.getenv("HADOOP_CONF");
	
		Path p = new Path(cf);
		Configuration configuration = new Configuration(true);
		configuration.addResource(p);
		FileSystem hdfs;
		hdfs = FileSystem.get(/*new URI(url),*/ configuration);
		RemoteIterator<LocatedFileStatus> it = hdfs.listFiles(new Path("/")/*url+directory)*/, true);
		while(it.hasNext())
		{
			LocatedFileStatus next = it.next();
			String path = next.getPath().toString();
			String name = next.getPath().getName();
			Long size = next.getLen();
			String parentPath = path.substring(0, path.lastIndexOf("/"));
			parentPath = parentPath.replace(configuration.get("fs.defaultFS"), "");
			if(structure.get(parentPath) == null)
				structure.put(parentPath, new HashMap<String,Long>());
			structure.get(parentPath).put(name, size);
		}
		return structure;
	}
	
	public String getHiveContent() throws NoSuchObjectException, TException, IllegalArgumentException, IOException, ClassNotFoundException{
		//Getting Environnement variables locations
		String hiveCf = System.getenv("HIVE_CONF");
		String hdfsCf = System.getenv("HADOOP_CONF");

		//Setting paths
		Path hivep = new Path(hiveCf);
		Path hdfsp = new Path(hdfsCf);
		
		//Setting HiveConf
		HiveConf hiveConf = null;
		hiveConf =  new HiveConf();
		hiveConf.addResource(hivep);
		hiveConf.addResource(hdfsp);
		HiveMetaStoreClient client = new HiveMetaStoreClient(hiveConf);
		
		//Setting HDFS conf
		DistributedFileSystem hdfs = null;
		FileSystem fs = FileSystem.get(/*new URI(url),*/ hiveConf);
		hdfs = (DistributedFileSystem) fs;
		hdfs.setConf(hiveConf);
		
		
		List<String> dbs = client.getAllDatabases();
		JsonObject json = new JsonObject();
		json.add("dbs", new JsonArray());
		JsonObject dbJson = null;
		for(String db:dbs){
			dbJson = new JsonObject();
			dbJson.addProperty("name", db);
			String loc = client.getDatabase(db).getLocationUri();
			dbJson.addProperty("location", loc);
			int sum = 0;
			List<String> tables = client.getAllTables(db);
			JsonArray tablesJson = new JsonArray();
			for(String tb:tables){
				JsonObject tbJson = new JsonObject();
				Table table = client.getTable(db, tb);
				String name = tb;
				String location = table.getSd().getLocation();
				String type = table.getTableType();
				int last = table.getLastAccessTime();
				long size = hdfs.getContentSummary(new Path(location)).getLength();
				tbJson.addProperty("name", name);
				tbJson.addProperty("location", location);
				tbJson.addProperty("type", type);
				tbJson.addProperty("last", last);
				tbJson.addProperty("size", size);
				tablesJson.add(tbJson);
				sum += size;
			}
			dbJson.addProperty("size", sum);
			dbJson.add("tables", tablesJson);
		}
		json.get("dbs").getAsJsonArray().add(dbJson);
		return json.toString();
	}
	
	
	public String databases() throws NoSuchObjectException, TException, IOException{
		//Getting Environnement variables locations
		String hiveCf = System.getenv("HIVE_CONF");
		String hdfsCf = System.getenv("HADOOP_CONF");
		//Setting paths
		Path hivep = new Path(hiveCf);
		Path hdfsp = new Path(hdfsCf);
		//Setting HiveConf
		HiveConf hiveConf = null;
		hiveConf =  new HiveConf();
		hiveConf.addResource(hivep);
		hiveConf.addResource(hdfsp);
		HiveMetaStoreClient client = new HiveMetaStoreClient(hiveConf);
		//Setting HDFS conf
		DistributedFileSystem hdfs = null;
		FileSystem fs = FileSystem.get(/*new URI(url),*/ hiveConf);
		hdfs = (DistributedFileSystem) fs;
		hdfs.setConf(hiveConf);

		List<String> dbs = client.getAllDatabases();
		JsonObject json = new JsonObject();
		json.add("dbs", new JsonArray());
		JsonObject dbJson = null;
		for(String db:dbs){
			dbJson = new JsonObject();
			dbJson.addProperty("label", db);
			int sum = 0;
			List<String> tables = client.getAllTables(db);
			for(String tb:tables){
				Table table = client.getTable(db, tb);
				String location = table.getSd().getLocation();
				dbJson.addProperty("location", location);
				long size = hdfs.getContentSummary(new Path(location)).getLength();
				sum += size;
			}
			dbJson.addProperty("count", sum);
			json.get("dbs").getAsJsonArray().add(dbJson);
		}
		return json.toString();
	}

}
