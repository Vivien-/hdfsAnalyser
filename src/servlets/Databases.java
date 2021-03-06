/**
 * @author Mohammed El Moumni, Vivien Achet
 * 
 * Utility: send a json describing the hive databases. 
 */

package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.thrift.TException;

import Exceptions.ConfPathException;
import Exceptions.HadoopConfException;
import Exceptions.HiveConfException;
import analyser.DFSAnalyser;

/**
 * Servlet implementation class Databases
 * 
 * Get the Hive databases
 * 
 */
@WebServlet("/Databases")
public class Databases extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Databases() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String json = "";
		DFSAnalyser dfs = new DFSAnalyser();
		try {
			json = dfs.databases();
			response.getWriter().print(json);
		} catch (HiveConfException e) {
			System.out.println("hive servlet");
			response.sendError(1000);
		} catch (HadoopConfException e) {
			System.out.println("hadoop servlet");
			response.sendError(1001);
		} 
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
