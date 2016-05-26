/**
 * @author Mohammed El Moumni, Vivien Achet
 * @deprecated servlet, to use only if Start servlet does not work
 * 		Start servlet uses Node and Tree analyser, so if those classes does not work, use this servlet coupled
 * 		with DFSAnalyser analyser.
 * 
 *  Utility: sens a json describing the cluster architecture
 */

package servlets;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import analyser.DFSAnalyser;

/**
 * Servlet implementation class FileContent
 */
@WebServlet("/FileContent")
public class FileContent extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileContent() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String json = "";
		DFSAnalyser dfs = new DFSAnalyser();
		try {
			json = dfs.jsonify(dfs.getHDFSContent(), Integer.parseInt(request.getParameter("minSize")));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		response.getWriter().print(json);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
