package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.thrift.TException;

import Exceptions.ConfPathException;
import Exceptions.EmptyDatabaseException;
import Exceptions.HadoopConfException;
import Exceptions.HiveConfException;
import analyser.DFSAnalyser;

/**
 * Servlet implementation class Tables
 */
@WebServlet("/Tables")
public class Tables extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Tables() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String database = request.getParameter("database");
		String json = "";
		DFSAnalyser dfs = new DFSAnalyser();
		try {
			json = dfs.tables(database);
			response.getWriter().print(json);
		}  catch (HadoopConfException e) {
			response.sendError(1001);
		} catch (HiveConfException e) {
			response.sendError(1000);
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
