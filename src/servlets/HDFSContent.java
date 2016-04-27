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
import analyser.Tree;
import analyser.TreeI;

/**
 * Servlet implementation class HDFSContent
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
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		int minSize = Integer.parseInt(request.getParameter("minSize"));
		tree.setMinSize(minSize);
		if(!tree.isInitilized()){
			try{
				System.out.println("init");
				tree.init();
				response.getWriter().print(tree.getJson());
			}
			catch(HadoopConfException e){
				response.sendError(1001);
			}
		}
		else{
			System.out.println("update");
			tree.update();
			response.getWriter().print(tree.getJson());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
