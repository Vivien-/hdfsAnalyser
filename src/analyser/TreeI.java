package analyser;


import Exceptions.HadoopConfException;

public interface TreeI {

	public boolean isEmpty();

	public void init() throws HadoopConfException;

	public Node getRoot();

	public void setRoot(Node root) ;

	public int getMinSize();

	public void setMinSize(int minSize);


	public boolean isInitilized();

	public void setInitilized(boolean isInitilized);
	public void update();

	public String getJson();		
}
