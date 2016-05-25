package analyser;

import java.io.Serializable;
import java.util.*;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;


import com.google.common.collect.TreeTraverser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import Exceptions.HadoopConfException;

@SessionScoped
@Named("tree")
public class Tree implements Serializable, TreeI{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Node root;
	private int minSize;
	private boolean isInitilized;
	private FileSystem hdfs;


	/**
	 * Constructor: initialize root and hdfs
	 * @throws HadoopConfException
	 */
	public Tree() throws HadoopConfException{
		try{
			String cf = System.getenv("HADOOP_CONF");
			Path p = new Path(cf);
			Configuration configuration = new Configuration(true);
			configuration.addResource(p);
			hdfs = FileSystem.get(configuration);
			root = new Node("/", 0, 0, "/");
			this.minSize = 0;
			this.isInitilized = false;
		}
		catch(Exception e){
			throw new HadoopConfException();
		}
	}

	public boolean isEmpty(){
		return root==null;
	}

	/**
	 * Add a node to the current tree
	 * @param str
	 * @param size
	 * @param lastModified
	 */
	private void add(String str, long size, long lastModified){
		String other_name = "others-LT" + minSize;
		root.setSize(root.getSize()+size);
		Node current = root;
		String[] s = str.split("/");
		String path = "";

		for(int i = 0; i < s.length; i++){
			str = s[i];
			path = path+"/"+str;
			Node child = current.getChild(str);
			Node t_child = child;
			
			if(child != null && (i == s.length-1)) {
				current.getChildren().remove(child);
				child = null;
			}

			if(child == null) {
				if((i == s.length-1) && (size < minSize)){
					if(current.getChild(other_name) == null)
						current.getChildren().add(new Node(other_name, size, lastModified,path.substring(0, path.lastIndexOf("/")).substring(0, path.lastIndexOf("/"))+"/"+other_name));
					else{
						current.getChild(other_name).setSize(current.getChild(other_name).getSize() + size);
						if(lastModified > current.getChild(other_name).getLastModified())
							current.getChild(other_name).setLastModified(lastModified);
					}
					t_child = current.getChild(other_name);
				}
				else{
					current.getChildren().add(new Node(str, size, lastModified, path));
					t_child = current.getChild(str);
				}
				child = current.getChild(str);
			}
			else {	
				child.setSize(child.getSize()+size);
			}
			if(t_child != null) {
				t_child.setParent(current);
			}
				
			if((i == s.length-2) && (lastModified > current.getLastModified()))
				current.setLastModified(lastModified);
			current = child;
		}
	}

	
	public void init(int minSize, String root) throws HadoopConfException{
		try{
			this.setMinSize(minSize);
			RemoteIterator<LocatedFileStatus> it = hdfs.listFiles(new Path(root), true);
			LocatedFileStatus next;
			String path;
			String name;
			long size;
			long lastModified;
			try{
				while(it.hasNext()) {
					next = it.next();
					path = next.getPath().toString();
					name = next.getPath().getName();
					size = next.getLen();
					lastModified = next.getModificationTime();
					path = path.replace(hdfs.getConf().get("fs.defaultFS"), "");
					this.add(path, size, lastModified);
				}
			} catch(Exception e){
				throw new HadoopConfException();
			}
			this.isInitilized = true;
		}
		catch(Exception e){
			throw new HadoopConfException();
		}
	}

	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

	public int getMinSize() {
		return minSize;
	}

	public void setMinSize(int minSize) {
		this.minSize = minSize;
	}

	public boolean isInitilized() {
		return isInitilized;
	}

	public void setInitilized(boolean isInitilized) {
		this.isInitilized = isInitilized;
	}

	private void updateMinSize(int newMinSize) throws HadoopConfException {
		root = new Node("/", 0, 0, "/");
		this.minSize = newMinSize;
		this.isInitilized = false;
		init(newMinSize, "/");
	}


	private void updateLastModified(Node directory) {
		try {
			long hdfsAccessTime = hdfs.getFileStatus(new Path(hdfs.getConf().get("fs.defaultFS")+directory.getPath())).getModificationTime();
			Path p = new Path(directory.getPath());
			FileStatus[] t = hdfs.listStatus(p);
			Boolean needUpdate = (directory.getLastModified() != hdfsAccessTime);
			if(!needUpdate) {
				for(int i = 0; i < t.length; i++) {
					if(t[i].isDirectory()) {
						String pathStr = t[i].getPath().toString();
						String childName = pathStr.substring(pathStr.lastIndexOf('/')+1);
						if(directory.getChild(childName) != null) {
							updateLastModified(directory.getChild(childName));
						}
						else{
							deleteOldElement(directory);
						}
					}
				}
			} else {
				directory.setLastModified(hdfsAccessTime);
				updateFilesInPath(p);
				deleteOldElement(directory);
				for(int i = 0; i < t.length; i++) {
					if(t[i].isDirectory()) {
						String pathStr = t[i].getPath().toString();
						String childName = pathStr.substring(pathStr.lastIndexOf('/')+1);
						if(directory.getChild(childName) != null) {
							updateLastModified(directory.getChild(t[i].getPath().getName()));
						} else {
							init(minSize, t[i].getPath().toString());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void updateFilesInPath(Path p) {
		try {
			RemoteIterator<LocatedFileStatus> it = hdfs.listFiles(p, false);
			LocatedFileStatus next;
			String path;
			String name;
			long size;
			long lastModified;
			while(it.hasNext()) {
				next = it.next();
				path = next.getPath().toString();
				name = next.getPath().getName();
				size = next.getLen();
				lastModified = next.getModificationTime();
				path = path.replace(hdfs.getConf().get("fs.defaultFS"), "");
				this.add(path, size, lastModified);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	private void deleteOldElement(Node dir) {
		ArrayList<Node> children = dir.getChildren();
		ArrayList<Node> newChildren = new ArrayList<Node>();
		int size = 0;
		Node parent = dir.getParent();
		Node curent = dir;
		for(Node child : children) {
			try {
				if(hdfs.exists(new Path(child.getPath())) || child.getData().equals("others-LT" + minSize)) {
					newChildren.add(child);
					size += child.getSize();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		dir.setSize(size);
		dir.setChildren(newChildren);

		while(true) {
			if(curent != null && ! curent.equals(root) && (curent.getSize() == 0 || curent.getChildren().size() == 0)) {
				parent.getChildren().remove(curent);
				curent = parent;
				parent = curent.getParent();
			} else {
				break;
			}
		}
	}


	public void update(int minSize) throws HadoopConfException{
		if(minSize != this.getMinSize()) {
			try {
				updateMinSize(minSize);
			} catch (HadoopConfException e) {
				throw new HadoopConfException();
			}
		}
		updateLastModified(root);
	}

	public String getJson(){
		TreeTraverser<Node> traverser = new TreeTraverser<Node>() {
			@Override
			public Iterable<Node> children(Node root) {
				return root.getChildren();
			}
		};

		//this is the final json that is going to get returned
		JsonObject json_f = new JsonObject(); 
		//we store the json objects under their key so we can quickly access the parent of the current json
		Map <String, JsonObject> mapJson = new HashMap<String, JsonObject>();

		for (Node node : traverser.preOrderTraversal(root)) {
			JsonObject json = new JsonObject();
			String parentKey = node.getPath().substring(0, node.getPath().lastIndexOf("/"));

			JsonObject parentJson = json_f;
			if(parentKey != null && !parentKey.isEmpty()) {
				parentJson = mapJson.get(parentKey);
			}

			json.addProperty("name", node.getData());
			int nchild = node.getChildren().size();
			if(nchild > 0) { //node is a directory containing at least 1 child
				json.add("children",new JsonArray());
			} else {       //node is a file
				json.addProperty("size", node.getSize());	
			}

			if(parentJson != null) {
				try {
					parentJson.get("children").getAsJsonArray().add(json);
				} catch(Exception e) {
					json_f.addProperty("name", "/");
					json_f.add("children", new JsonArray());	
				}
			}
			mapJson.put(node.getPath(), json);
		}
		return json_f.toString();
	}
}