package analyser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ServiceException;

import Exceptions.ConfPathException;
import Exceptions.EmptyDatabaseException;
import Exceptions.HBaseConfException;
import Exceptions.HadoopConfException;
import Exceptions.HiveConfException;


public class DFSAnalyser {

	
	public DFSAnalyser(){
	}

	private int indexInArray(JsonArray array, String id){
		for(int i = 0; i < array.size(); i++){
			JsonObject tmp = (JsonObject)array.get(i);
			if(tmp.get("name").getAsString().equals(id))
				return i;
		}
		return -1;
	}

	public String diskUsage() throws HadoopConfException{
		JsonObject json = new JsonObject();
		JsonObject global = new JsonObject();
		try{
			String cf = System.getenv("HADOOP_CONF");
			Path p = new Path(cf);
			Configuration configuration = new Configuration(true);
			configuration.addResource(p);
			DistributedFileSystem hdfs = (DistributedFileSystem)DistributedFileSystem.get(configuration);
			try{
				DatanodeInfo[] dataNodes = hdfs.getDataNodeStats(DatanodeReportType.ALL);
				json.add("summary", new JsonArray());
				json.addProperty("isOk", 1);
				try{
					global.addProperty("used", hdfs.getStatus().getUsed());
					global.addProperty("unused", hdfs.getStatus().getRemaining());
					json.get("summary").getAsJsonArray().add(global);
					for(int i = 0; i < dataNodes.length; i++){
						JsonObject current = new JsonObject();
						try{
							current.addProperty("name", dataNodes[i].getName());
							current.addProperty("used", dataNodes[i].getDfsUsed());
							current.addProperty("unused", dataNodes[i].getCapacity());
							current.addProperty("percentage", dataNodes[i].getDfsUsedPercent());
							current.addProperty("nondfs", dataNodes[i].getNonDfsUsed());
							current.addProperty("isOk", 1);
							json.get("summary").getAsJsonArray().add(current);
						}
						catch(Exception e){
							System.out.println("problem in a node");
							current.addProperty("isOk", 0);
							json.get("summary").getAsJsonArray().add(current);
						}
					}
					try{
						json.addProperty("replication", hdfs.getFileStatus(new Path("/")).getReplication() + 1);
					}
					catch(Exception e){
						json.addProperty("replication", -1);
					}
				}
				catch(Exception e){
					System.out.println("used or unused problem");
					JsonObject errorGlobal = new JsonObject();
					errorGlobal.addProperty("used", -1);
					errorGlobal.addProperty("unused", -1);
					json.get("summary").getAsJsonArray().add(errorGlobal);
				}
			}
			catch(Exception e){
				System.out.println("Can't access datanodes");
				json.addProperty("isOk", 0);
			}
		}
		catch(Exception e){
			throw new HadoopConfException();
		}
		return json.toString();
	}

	public String jsonify(TreeMap<String,Map<String, Long>> treemap, int msize) {
		long minSize = (long) Math.pow(10, msize);
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
			long otherSize = 0;
			Map<String, Long> files = entry.getValue();
			for(Map.Entry<String,Long> file : files.entrySet()) {
				if(file.getValue() < minSize) {
					otherSize += file.getValue();
				} else {
					JsonObject tmp = new JsonObject();
					tmp.addProperty("name", file.getKey());
					tmp.addProperty("size", file.getValue());
					next.get("children").getAsJsonArray().add(tmp);
				}
			}
			if(otherSize > 0) {
				JsonObject tmp = new JsonObject();
				tmp.addProperty("name", "Other-LT"+minSize);
				tmp.addProperty("size", otherSize);
				next.get("children").getAsJsonArray().add(tmp);
			}
		}
		json_f.get("children").getAsJsonArray().add(json);
		return json_f.toString();
	}

	public TreeMap<String,Map<String, Long>> getHDFSContent() throws IllegalArgumentException, IOException, URISyntaxException{
		TreeMap<String,Map<String, Long>> structure = new TreeMap<String, Map<String, Long>>();
		String cf = System.getenv("HADOOP_CONF");
	
		Path p = new Path(cf);
		Configuration configuration = new Configuration(true);
		configuration.addResource(p);
		FileSystem hdfs;
		hdfs = FileSystem.get(configuration);
		RemoteIterator<LocatedFileStatus> it = hdfs.listFiles(new Path("/"), true);
		LocatedFileStatus next;
		String path;
		String name;
		Long size;
		String parentPath;
		while(it.hasNext())
		{
			next = it.next();
			path = next.getPath().toString();
			name = next.getPath().getName();
			size = next.getLen();
			parentPath = path.substring(0, path.lastIndexOf("/"));
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
		FileSystem fs = FileSystem.get(hiveConf);
		hdfs = (DistributedFileSystem) fs;
		hdfs.setConf(hiveConf);
		
		
		List<String> dbs = client.getAllDatabases();
		JsonObject json = new JsonObject();
		json.add("dbs", new JsonArray());
		JsonObject dbJson = null;
		for(String db:dbs){
			try{
				dbJson = new JsonObject();
				dbJson.addProperty("name", db);
				String loc = client.getDatabase(db).getLocationUri();
				dbJson.addProperty("location", loc);
				int sum = 0;
				List<String> tables = client.getAllTables(db);
				JsonArray tablesJson = new JsonArray();
				Table table;
				String name;
				String location;
				String type;
				int last;
				long size;
				for(String tb:tables){
					try{
						JsonObject tbJson = new JsonObject();
						table = client.getTable(db, tb);
						name = tb;
						location = table.getSd().getLocation();
						type = table.getTableType();
						last = table.getLastAccessTime();
						size = hdfs.getContentSummary(new Path(location)).getLength();
						tbJson.addProperty("name", name);
						tbJson.addProperty("location", location);
						tbJson.addProperty("type", type);
						tbJson.addProperty("last", last);
						tbJson.addProperty("size", size);
						tablesJson.add(tbJson);
						sum += size;
					}
					catch( TException | IOException e){
						e.printStackTrace();
					}
				}
				dbJson.addProperty("size", sum);
				dbJson.add("tables", tablesJson);
			}
			catch(TException e){
				e.printStackTrace();
			}
		}
		json.get("dbs").getAsJsonArray().add(dbJson);
		return json.toString();
	}


	public String databases() throws HadoopConfException, HiveConfException{
		JsonObject json = new JsonObject();
		try{
			//Setting HiveConf
			String hiveCf = System.getenv("HIVE_CONF");
			Path hivep = new Path(hiveCf);
			HiveConf hiveConf =  new HiveConf();
			hiveConf.addResource(hivep);
			HiveMetaStoreClient client = new HiveMetaStoreClient(hiveConf);	
			try{
				//Setting HadoopConf
				String hdfsCf = System.getenv("HADOOP_CONF");
				Path hdfsp = new Path(hdfsCf);
				hiveConf.addResource(hdfsp);
				DistributedFileSystem hdfs = (DistributedFileSystem)DistributedFileSystem.get(hiveConf);
				try{
					json.add("dbs", new JsonArray());
					//getting all the tables
					List<String> dbs = client.getAllDatabases();
					JsonObject dbJson = null;
					for(String db:dbs){
						try{
							Database database = client.getDatabase(db);
							dbJson = new JsonObject();
							dbJson.addProperty("label", db);
							dbJson.addProperty("location", database.getLocationUri().replace(hiveConf.get("fs.defaultFS"), ""));
							int sum = 0;
							try{
								List<String> tables = client.getAllTables(db);
								Table table;
								String location;
								long size;
								for(String tb:tables){ 
									try{
										table = client.getTable(db, tb);
										location = table.getSd().getLocation();
										size = hdfs.getContentSummary(new Path(location)).getLength();
										sum += size;
									}
									catch(Exception e){
										System.out.println("hive or hdfs can't find table : "+tb);
									}
								}
								dbJson.addProperty("count", sum);
								dbJson.addProperty("isOk", 1);
								json.get("dbs").getAsJsonArray().add(dbJson);
							}
							catch(Exception e){
								dbJson.addProperty("isOk", 1);
								json.get("dbs").getAsJsonArray().add(dbJson);
							}		
						}
						catch(Exception e){
							System.out.println("hive can't find database : "+db);
							dbJson.addProperty("label", db);
							dbJson.addProperty("isOk", 0);
							json.get("dbs").getAsJsonArray().add(dbJson);
						}
					}
				}
				catch(Exception e){
					System.out.println("no database");
				}
			}
			catch(Exception e){
				throw new HadoopConfException();
			}

		}
		catch(Exception e){
			if(e instanceof HadoopConfException)
				throw new HadoopConfException();
			else
				throw new HiveConfException();
		}
		return json.toString();
	}

	public String tables(String database) throws HadoopConfException, HiveConfException{
		JsonObject json = new JsonObject();
		try{
			//Setting Hive conf
			String hiveCf = System.getenv("HIVE_CONF");
			Path hivep = new Path(hiveCf);
			HiveConf hiveConf =  new HiveConf();
			hiveConf.addResource(hivep);
			HiveMetaStoreClient client = new HiveMetaStoreClient(hiveConf);
			try{
				//Setting hdfs conf
				String hdfsCf = System.getenv("HADOOP_CONF");
				Path hdfsp = new Path(hdfsCf);
				hiveConf.addResource(hdfsp);
				DistributedFileSystem hdfs = (DistributedFileSystem)FileSystem.get(hiveConf);
				try{
					json.addProperty("database", database);
					json.add("tbls", new JsonArray());

					List<String> tables = client.getAllTables(database);
					Table table;
					String location;
					String type;
					long size;
					for(String tb:tables){
						JsonObject tmp = new JsonObject();
						try{
							table = client.getTable(database, tb);
							location = table.getSd().getLocation();
							type = table.getTableType();
							size = hdfs.getContentSummary(new Path(location)).getLength();
							location = location.replace(hiveConf.get("fs.defaultFS"), "");
							tmp.addProperty("label", tb);
							tmp.addProperty("location", location);
							tmp.addProperty("type", type);
							tmp.addProperty("count", size);
							tmp.addProperty("isOk", 1);
							json.get("tbls").getAsJsonArray().add(tmp);
						}
						catch(Exception e){
							System.out.println("hive can't find table"+tb);
							tmp.addProperty("label", tb);
							tmp.addProperty("isOk", 0);
							json.get("tbls").getAsJsonArray().add(tmp);
						}
					}
				}
				catch(Exception e){
					System.out.println("database : "+database+" is empty");
				}
			}
			catch(Exception e){
				throw new HadoopConfException();
			}
		
		}
		catch(Exception e){
			if(e instanceof HadoopConfException)
				throw new HadoopConfException();
			else
				throw new HiveConfException();
		}		
		return json.toString();
	}
	
	//works for hbase +0.98
	public String getHbaseContent() throws HBaseConfException, HadoopConfException{
		JsonObject json = new JsonObject();
		json.add("tbls", new JsonArray());
		try{
			//Setting hBase conf
			String hbaseCf = System.getenv("HBASE_CONF");
			Path hbasep = new Path(hbaseCf);
			Configuration hbaseConf = new HBaseConfiguration();
			hbaseConf.addResource(hbasep);
			HBaseAdmin.checkHBaseAvailable(hbaseConf);
			HBaseAdmin admin = new HBaseAdmin(hbaseConf);
			try{
				//Setting Hadoop Conf
				String hdfsCf = System.getenv("HADOOP_CONF");
				Path hdfsp = new Path(hdfsCf);
				hbaseConf.addResource(hdfsp);
				DistributedFileSystem hdfs = null;
				FileSystem fs = FileSystem.get(hbaseConf);
				hdfs = (DistributedFileSystem) fs;
				hdfs.setConf(hbaseConf);
				
				try{
					HTableDescriptor[] tablesDescriptor = admin.listTables();
					String name;
					String location;
					long size;
					for(int i = 0; i< tablesDescriptor.length; i++){
						JsonObject tmp = new JsonObject();
						name = tablesDescriptor[i].getNameAsString();
						if(hbaseConf.get("fs.defaultFS").endsWith("/"))
							location = hbaseConf.get("fs.defaultFS")+"hbase/data/default/"+name;
						else 
							location = hbaseConf.get("fs.defaultFS")+"/hbase/data/default/"+name;
						try{
							size = hdfs.getContentSummary(new Path(location)).getLength();
							location = location.replace(hbaseConf.get("fs.defaultFS"), "");
							tmp.addProperty("name", name);
							tmp.addProperty("location", location);
							tmp.addProperty("size", size);
							tmp.addProperty("isOk", 1);
						}
						catch(Exception e){
							tmp.addProperty("name", name);
							tmp.addProperty("location", location);
							tmp.addProperty("isOk", 0);
						}
						
						json.get("tbls").getAsJsonArray().add(tmp);
					}
				}
				catch(Exception e){
					System.out.println("no tables");
				}

			}
			catch(Exception e){
				throw new HadoopConfException();
			}
		}
		catch(Exception e){
			if(e instanceof HadoopConfException)
				throw new HadoopConfException();
			else
				throw new HBaseConfException();
		}
		return json.toString();
	}

}
