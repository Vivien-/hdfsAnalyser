/**
 * @author Mohammed El Moumni, Vivien Achet
 * 
 * Utility: send a json describing the cluster architecture. Should be the same json that the send by FileContent 
 * 		servlet. But it should be way more optimized. If there is an issue you can fallback to using FileContent 
 */

package servlets;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import Exceptions.HadoopConfException;
import analyser.TreeI;

/**
 * Servlet implementation class HDFSContent
 * 
 * Get the whole tree architecture of the hdfs cluster given by a hdfs configuratio file
 */
@WebServlet("/HDFSContent")
public class HDFSContent extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	@Inject @Named("tree")
	private TreeI tree;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HDFSContent() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Ensures :
	 * 		- if the hdfs files architecture can be retrieved it sends the json representing that architecture
	 * 		- else send an error code used by the client to give an error message accordingly
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int minSize = Integer.parseInt(request.getParameter("minSize"));
		minSize = (int)Math.pow(10, minSize);
		
		if(!tree.isInitilized()){
			try{
				tree.init(minSize, "/");
				response.getWriter().print(tree.getJson());
			}
			catch(HadoopConfException e){
				response.sendError(1001);
			}
		}
		else{
			try {
				tree.update(minSize);
				response.getWriter().print(tree.getJson());
			} catch (HadoopConfException e) {
				response.sendError(1001);
			}
			
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
