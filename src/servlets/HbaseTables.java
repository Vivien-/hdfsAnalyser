/**
 * @author Mohammed El Moumni, Vivien Achet
 * 
 * Utility: send a json describing the hbase tables. 
 */

package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.protobuf.ServiceException;

import Exceptions.HBaseConfException;
import Exceptions.HadoopConfException;
import analyser.DFSAnalyser;

/**
 * Servlet implementation class HbaseTables
 */
@WebServlet("/HbaseTables")
public class HbaseTables extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HbaseTables() {
        super();
    }

	/**
	 * @throws IOException 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		try{
			DFSAnalyser dfs = new DFSAnalyser();
			String json;
			json = dfs.getHbaseContent();
			response.getWriter().print(json);
		}
		catch(HadoopConfException e){
			response.sendError(1001);
		}
		catch(HBaseConfException e){
			response.sendError(1002);
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
