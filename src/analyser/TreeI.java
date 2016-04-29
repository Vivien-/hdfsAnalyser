package analyser;


import Exceptions.HadoopConfException;

public interface TreeI {

	public boolean isEmpty();

	public void init(int minSize, String root) throws HadoopConfException;

	public Node getRoot();

	public void setRoot(Node root) ;

	public int getMinSize();

	public void setMinSize(int minSize);


	public boolean isInitilized();

	public void setInitilized(boolean isInitilized);
	public void update(int minSize) throws HadoopConfException;

	public String getJson();		
}
