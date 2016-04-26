package analyser;

import java.util.*;
import com.google.common.collect.TreeTraverser;
import com.google.inject.servlet.SessionScoped;


@SessionScoped
public class Tree
{
   
    private Node root;
    private int minSize;
    
    
    public Tree(int minSize){
        root = new Node("/", 0, 0);
        this.minSize = minSize;
    }

    public boolean isEmpty(){
        return root==null;
    }

    public void add(String str, long size, long lastModified){
    	root.setSize(root.getSize()+size);
    	if(lastModified > root.getLastModified())
    		root.setLastModified(lastModified);
    	Node current = root;
        StringTokenizer s = new StringTokenizer(str, "/");
        while(s.hasMoreElements())
        {
            str = (String)s.nextElement();
            Node child = current.getChild(str);
            if(child==null)
            {
            	if(!s.hasMoreElements() && size < minSize){
            		if(current.getChild("others") == null)
            			current.getChildren().add(new Node("others", size, lastModified));
            		else{
            			current.getChild("others").setSize(current.getChild("others").getSize() + size);
            			if(lastModified > current.getChild("others").getLastModified())
            				current.getChild("others").setLastModified(lastModified);
            		}
            	}
            	else{
            		current.getChildren().add(new Node(str, size, lastModified));
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

	public void traverse(){
    	TreeTraverser<Node> traverser = new TreeTraverser<Node>() {
    	    @Override
    	    public Iterable<Node> children(Node root) {
    	    	return root.getChildren();
    	    }
    	};
    	int i = 0;
    	for (Node node : traverser.preOrderTraversal(root)) {
    		i++;
    		System.out.println(node.getData()+" : "+node.getSize()+" : "+node.getLastModified());
    	}
    }
	
	
	public static void main (String[] args){
		Tree t = new Tree(5000);
		t.add("/tmp/dir1/file1", 3000, 1000);
		t.add("/tmp/dir1/file2", 1500, 1430);
		t.add("/tmp/dir1/file3", 6000, 2455);
		t.add("/tmp/dir2/file4", 1000, 1065);
		t.add("/tmp/dir3/file5", 2346, 1235);
		t.add("/tmp/dir2/file6", 5423, 10);
		t.traverse();
	}
}