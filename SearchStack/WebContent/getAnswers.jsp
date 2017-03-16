<%@page import="controller.AnswerNode"%>
<%@page import="java.util.ArrayList"%>
<%@page import="controller.QueryProcessing"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<style type="text/css">
.centerBody {
	background-color: #80ffd4;
	width: 100%;
}

.centerBodyTable {
	background-color: #e6fff7;
	width: 100%;
}
</style>
</head>
<body class="centerBody">
	<div class="centerBody">
		<h2 align="center" style="color:black" >Query Store</h2>
	</div>
	<%
		String postId = request.getParameter("postid");
		ArrayList<AnswerNode> ansList = new ArrayList<AnswerNode>();
		QueryProcessing qb = QueryProcessing.getInstance();
		if (postId != null) {
			int id = Integer.parseInt(postId);
			ansList = qb.getAnswers(id);
		}
		if (ansList.size() == 0) {
	%>
	<div class="centerBody">
		<h2 align="center">
			<b>No Answers present for this post</b>
		</h2>
	</div>
	<%
		} else {
	%>
	<div class="centerBody">
		<h2 align="center">
			<b>List of Answers</b>
		</h2>
	</div>
	<div align="center" height="70%" class="centerBodyTable">
		<table class="center" border="1px" class="centerBodyTable">

			<tr>
				<th>Answer</th>
				<th>Score</th>
			</tr>
			<%
				int answerId = 0;
					String body = null;
					int score = 0;
					for (int i = 0; i < ansList.size(); i++) {
						answerId = ansList.get(i).id;
						body = ansList.get(i).body;
						score = ansList.get(i).score;
			%>
			<tr>
				<td><%=body%></td>
				<td><%=score%></td>
			</tr>
			<%
				}
				}
			%>
		</table>
	</div>
</body>
</html>