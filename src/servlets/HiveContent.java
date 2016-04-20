package servlets;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.thrift.TException;

import analyser.DFSAnalyser;

/**
 * Servlet implementation class HiveContent
 */
@WebServlet("/HiveContent")
public class HiveContent extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HiveContent() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String json = "";
			DFSAnalyser dfs = new DFSAnalyser(/*cookies[i].getValue()*/);
			json = dfs.getHiveContent(/*""*/);
			response.getWriter().print(json);
		}  catch (IllegalArgumentException | TException | ClassNotFoundException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} 
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
