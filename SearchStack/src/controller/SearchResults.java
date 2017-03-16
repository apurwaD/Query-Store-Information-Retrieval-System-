package controller;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class SearchResults
 */
@WebServlet("/SearchResults")
public class SearchResults extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchResults() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String query= request.getParameter("query");
		query =query.trim();
		HttpSession sess = request.getSession(true);
		sess.setAttribute("resultset", null);
		if(query==null || query.length()==0){
			//sess.setAttribute("resultset", null);
			sess.setAttribute("message","Please enter valid query..!!");
		}else{
			QueryProcessing obj = QueryProcessing.getInstance();
			ArrayList<QuestionNode> results = obj.processQuery(query.toLowerCase());
			sess.setAttribute("resultset", results);
			System.out.println("Result size = "+results.size());
			if(results.size()==0){
			sess.setAttribute("message","No results found for query : "+query);
			}else{
			sess.setAttribute("message","Showing result for query : "+query);
			}
			sess.setAttribute("result", "true");
			sess.setAttribute("start", 0);
			sess.setAttribute("end", 10);
		}
		
		response.sendRedirect("index.jsp?start=0&end=10");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
