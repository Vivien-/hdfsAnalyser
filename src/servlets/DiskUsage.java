package servlets;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import analyser.DFSAnalyser;

/**
 * Servlet implementation class DiskUsage
 */
@WebServlet("/DiskUsage")
public class DiskUsage extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DiskUsage() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		response.setDateHeader("Expires", 0); // Proxies.
		Cookie[] cookies = request.getCookies();
		String json = "";
		boolean isok = false;
		if(cookies != null){
			for(int i = 0; i < cookies.length; i++){
				if(cookies[i].getName().equals("url")){
					try {
						DFSAnalyser dfs = new DFSAnalyser(cookies[i].getValue());
						json = dfs.diskUsage();
						isok = true;
					} catch (URISyntaxException e) {	
						isok = false;
						e.printStackTrace();
					}
				}
			}
		}
		if(isok)
			response.getWriter().print(json);
		else
			request.getRequestDispatcher("url.html").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
