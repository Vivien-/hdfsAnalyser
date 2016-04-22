package Exceptions;

public class EmptyDatabaseException extends Exception{
	public EmptyDatabaseException(){
		super();
	}
	
	public EmptyDatabaseException(String s){
		super(s);
	}
}
