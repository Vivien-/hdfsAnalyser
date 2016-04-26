package analyser;

import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import com.google.common.collect.TreeTraverser;
import com.google.inject.servlet.SessionScoped;

import Exceptions.HadoopConfException;


@SessionScoped
public class Tree
{
   
    private Node root;
    private int minSize;
    private boolean isInitilized;
    
    public Tree(int minSize){
        root = new Node("/", 0, 0, "/");
        this.minSize = minSize;
        this.isInitilized = false;
    }

    public boolean isEmpty(){
        return root==null;
    }

    private void add(String str, long size, long lastModified){
    	root.setSize(root.getSize()+size);
    	if(lastModified > root.getLastModified())
    		root.setLastModified(lastModified);
    	Node current = root;
        StringTokenizer s = new StringTokenizer(str, "/");
        String path = str;
        while(s.hasMoreElements())
        {
            str = (String)s.nextElement();
            Node child = current.getChild(str);
            if(child==null)
            {
            	if(!s.hasMoreElements() && size < minSize){
            		if(current.getChild("others") == null)
            			current.getChildren().add(new Node("others", size, lastModified,path.substring(0, path.lastIndexOf("/")+1)+"others"));
            		else{
            			current.getChild("others").setSize(current.getChild("others").getSize() + size);
            			if(lastModified > current.getChild("others").getLastModified())
            				current.getChild("others").setLastModified(lastModified);
            		}
            	}
            	else{
            		current.getChildren().add(new Node(str, size, lastModified, path));
            	}
            	child = current.getChild(str);
            }
            else{	
            	child.setSize(child.getSize()+size);
            }
            if(lastModified > current.getLastModified())
            	current.setLastModified(lastModified);
            current = child;
        }
    }

	public void init() throws HadoopConfException{
		try{
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
			long size;
			long lastModified;
			while(it.hasNext())
			{
				next = it.next();
				path = next.getPath().toString();
				name = next.getPath().getName();
				size = next.getLen();
				lastModified = next.getAccessTime();
				path = path.replace(configuration.get("fs.defaultFS"), "");
				this.add(path, size, lastModified);
			}
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

	public void update(){
    	TreeTraverser<Node> traverser = new TreeTraverser<Node>() {
    	    @Override
    	    public Iterable<Node> children(Node root) {
    	    	return root.getChildren();
    	    }
    	};
    	for (Node node : traverser.preOrderTraversal(root)) {
    		System.out.println(node.getData()+" : "+node.getSize()+" : "+node.getLastModified()+" : "+node.getPath());
    	}
    }
	
	public String getJson(){
		return null;
	}
	public static void main (String[] args){
		Tree t = new Tree(5000);
		t.add("/tmp/dir1/file1", 3000, 1000);
		t.add("/tmp/dir1/file2", 1500, 1430);
		t.add("/tmp/dir1/file3", 6000, 2455);
		t.add("/tmp/dir2/file4", 1000, 1065);
		t.add("/tmp/dir3/file5", 2346, 1235);
		t.add("/tmp/dir2/file6", 5423, 10);
		//t.traverse();
	}
}