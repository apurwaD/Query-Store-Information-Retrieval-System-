<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@page language="java" import="controller.*" isErrorPage="false"%>
<%@page language="java" import="java.util.*" isErrorPage="false"%>

<head>
<script type="text/javascript">
function showResults(postid) {
    window.location= "getAnswers.jsp?postid="+postid; 
}

function goNext(start) {
    window.location= "index.jsp?start="+start; 
}
</script>
<style>
.centerBody {
	background-color: #80ffd4;
	width: 100%;
}

.centerBodyTable {
	position: absolute;
	background-color: #e6fff7;
	width: 100%;
}

#search {
	float: left;
	margin-top: 9px;
	width: 100%;
	background-color: #e6f7ff
}

.search {
	padding: 5px 0;
	width: 70%;
	height: 30px;
	position: relative;
	left: 10px;
	float: left;
	line-height: 22px;
	background-color: #e6f7ff;
}

.search input {
	position: relative;
	width: 95%;
	float: Left;
	margin-left: 5px;
	-webkit-transition: all 0.7s ease-in-out;
	-moz-transition: all 0.7s ease-in-out;
	-o-transition: all 0.7s ease-in-out;
	transition: all 0.7s ease-in-out;
	height: 30px;
	line-height: 18px;
	padding: 0 2px 0 2px;
	border-radius: 1px;
}

.search:hover input, .search input:focus {
	width: 100%;
	margin-left: 0px;
}

.btn {
	height: 30px;
	position: absolute;
	right: 0;
	top: 5px;
	border-radius: 1px;
}
</style>
</head>
<html>
<%
	HttpSession sess = request.getSession();
	//sess.setAttribute("result", "false");
%>
<body class="centerBody" width="100%">
	<div class="centerBody">
		<h2 align="center" style="color: black">Query Store</h2>
		<div align="center" class="search">
			<form align="center" action="SearchResults">
				<input id="query" type="text" name="query" placeholder="Search" />
				<button type="submit" class="btn btn-primary btn-sm">Search</button>
			</form>
		</div>
	</div>

	<br>
	<br>

	<%
		String check = (String) sess.getAttribute("result");
		String msg = "";
		String query = (String) sess.getAttribute("query");
		;
		System.out.println(query);
		if (query == null) {
			query = "";
		}
		ArrayList<QuestionNode> results = new ArrayList<QuestionNode>();
		if (check != null && check.equals("true")) {
			results = (ArrayList) sess.getAttribute("resultset");
			msg = (String) sess.getAttribute("message");
	%>
	<br>
	<div class="centerBody">
		<h2 align="center">
			<%
				if (msg != null) {
			%>
			<b><%=msg%></b>
			<%
				}
			%>
		</h2>
	</div>
	<%
		if (results != null && results.size() != 0) {
				String startStr = (String) request.getParameter("start");
				int start = Integer.parseInt(startStr);
				int end = 0;
				int increase = 0;
				int decrease = 0;
				if (start + 10 > results.size()) {
					end = results.size();
					increase = 0;
				} else {
					end = start + 10;
					increase = 10;
				}

				if (start - 10 < 0) {
					decrease = 0;

				} else {
					decrease = 10;
				}
	%>


	<div class="centerBodyTable" align="center">
		<div class="centerBody">
			<a href="javascript:void(0);"
				onclick="goNext(<%=start - decrease%>);">prev</a> <a
				href="javascript:void(0);" onclick="goNext(<%=start + increase%>);">next</a>
		</div>


		<table class="centerBodyTable" border="1" width="100%">

			<tr>
				<th>Post Title</th>
				<th>Description</th>
				<th>No. Of Answers</th>
			</tr>
			<%
				int postID = 0;
						String body = null;
						String title = "";
						HashSet<Integer> listOfAns = null;
						int noOfAnswers = 0;
						for (int i = start; i < end; i++) {
							postID = results.get(i).id;
							body = results.get(i).body;
							title = results.get(i).title;
							listOfAns = results.get(i).listOfAnswers;
							noOfAnswers = listOfAns.size();
			%>
			<tr>
				<td><a href="javascript:void(0);"
					onclick="showResults(<%=postID%>);"> <%=title%>
				</a></td>
				<td><%=body%></td>
				<td><%=noOfAnswers%></td>
			</tr>
			<%
				}
					}
				}
			%>
		</table>
	</div>
</body>
</html>