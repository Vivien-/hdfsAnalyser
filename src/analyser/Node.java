package analyser;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Node{
	
	
    private String data;
    private long size;
    private Timestamp lastModified;
    private String path;
	



	private ArrayList<Node> children;

	
	
    public Node(String data, long size, long lastModified, String path){
        this.data = data;
        this.size = size;
        this.lastModified = new Timestamp(lastModified);
        this.path = path;
        children = new ArrayList<Node>();
    }

    
    
    
    public String getData() {
		return data;
	}




	public void setData(String data) {
		this.data = data;
	}




	public long getSize() {
		return size;
	}




	public void setSize(long size) {
		this.size = size;
	}




	public ArrayList<Node> getChildren() {
		return children;
	}




	public void setChildren(ArrayList<Node> children) {
		this.children = children;
	}




	public Node getChild(String data){
        for(Node n : children)
            if(n.data.equals(data))
                return n;

        return null;
    }
	
    
    public long getLastModified() {
		return lastModified.getTime();
	}




	public void setLastModified(long lastModified) {
		this.lastModified.setTime(lastModified);
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}


}
