package controller;

/** 
 * @author Apurwa Dandekar
 * @author Ishan Gulhane
 */
import java.util.*;
import java.util.Map.Entry;

import javax.management.Query;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;

public class QueryProcessing extends Thread {
	String path;
	HashMap<Integer, QuestionNode> questionDB;
	HashMap<Integer, AnswerNode> answerDB;
	HashMap<String, HashMap<Integer, ArrayList<Integer>>> termListPositional;
	HashMap<String, HashMap<Integer, Integer>> termListInverted;
	HashSet<String> stopList; // stop word list
	HashSet<Integer> resultPositional;
	HashSet<Integer> resultBoollean;;
	public static QueryProcessing instance = null;

	public QueryProcessing() {
		// parsing the stop word list
		stopList = parse("D:/data/stopwords.txt");
		questionDB = new HashMap<>();
		answerDB = new HashMap<>();
		termListPositional = new HashMap<>();
		termListInverted = new HashMap<>();
		try {
			File fXmlFile = new File("D:/data/Posts.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("row");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node node = nList.item(temp);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) node;

					if (eElement.getAttribute("PostTypeId").equals("1")) {// Question
																			// Parsing
						int id = Integer.valueOf(eElement.getAttribute("Id"));
						String body = Jsoup.parse(eElement.getAttribute("Body")).text().replaceAll("<[^>]*>", "");
						String title = eElement.getAttribute("Title");

						String acceptedAnswerStr = eElement.getAttribute("AcceptedAnswerId");
						int acceptedAnswer;
						if (acceptedAnswerStr == null || acceptedAnswerStr == "") {
							acceptedAnswer = 0;
						} else {
							acceptedAnswer = Integer.parseInt(acceptedAnswerStr);
						}
						int score = Integer.valueOf(eElement.getAttribute("Score"));
						QuestionNode questionNode = new QuestionNode();
						questionNode.id = id;
						questionNode.body = body.toLowerCase();
						questionNode.title = title.toLowerCase();
						questionNode.acceptedAnswerId = acceptedAnswer;
						questionNode.score = score;
						questionDB.put(id, questionNode);
					} else {// Answer Parsing
						int id = Integer.valueOf(eElement.getAttribute("Id"));
						String body = Jsoup.parse(eElement.getAttribute("Body")).text().replaceAll("<[^>]*>", "");
						String parentIdStr = eElement.getAttribute("ParentId");
						int parentId;
						if (parentIdStr == null || parentIdStr == "") {
							parentId = 0;
						} else {
							parentId = Integer.parseInt(parentIdStr);
						}
						int score = Integer.valueOf(eElement.getAttribute("Score"));
						int commentCount = Integer.valueOf(eElement.getAttribute("CommentCount"));
						QuestionNode questionNode = questionDB.get(parentId);
						if (questionNode != null) {
							questionNode.listOfAnswers.add(id);
							questionDB.put(parentId, questionNode);
						}
						AnswerNode answerNode = new AnswerNode();
						answerNode.body = body;
						answerNode.commentCount = commentCount;
						answerNode.id = id;
						answerNode.parentId = parentId;
						answerNode.score = score;
						answerDB.put(id, answerNode);

					}

				}
			}
			// System.out.println("tagssize :"+ tagsPost.size());
			// Creating Positional and Inverted indexes for the database
			for (Integer docId : questionDB.keySet()) {
				QuestionNode node = questionDB.get(docId);
				String line = node.title + " " + node.body;
				line = line.toLowerCase();
				String tokens[] = line.split("[ (@){}\",?.&%$#!+-:;]+");
				for (String token : tokens) {
					token = stemWord(token);
					if (!stopList.contains(token)) {
						if (termListInverted.containsKey(token)) {
							HashMap<Integer, Integer> set = termListInverted.get(token);
							if (set.containsKey(node.id)) {
								set.put(node.id, set.get(node.id) + 1);
							} else {
								set.put(node.id, 1);
							}
							termListInverted.put(token, set);
						} else {
							HashMap<Integer, Integer> set = new HashMap<>();
							set.put(node.id, 1);
							termListInverted.put(token, set);
						}
					}
				}

				for (int i = 0; i < tokens.length; i++) {
					String word = tokens[i];
					if (termListPositional.containsKey(word)) {
						HashMap<Integer, ArrayList<Integer>> docuId = termListPositional.get(word);
						if (docuId.containsKey(node.id)) {
							docuId.get(node.id).add(i);
						} else {
							ArrayList<Integer> freq = new ArrayList<>();
							freq.add(i);
							docuId.put(node.id, freq);
						}
						termListPositional.put(word, docuId);
					} else {
						ArrayList<Integer> freq = new ArrayList<>();
						freq.add(i);
						HashMap<Integer, ArrayList<Integer>> docuId = new HashMap<>();
						docuId.put(node.id, freq);
						termListPositional.put(word, docuId);
					}

				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * stemWord : Stems the given word
	 */
	public String stemWord(String word) {
		Stemmer st = new Stemmer();
		st.add(word.toCharArray(), word.length());
		st.stem();
		return st.toString();
	}

	/*
	 * parse : Tokenize the contents of given file
	 */
	public HashSet<String> parse(String fileName) {
		HashSet<String> list = new HashSet<>();
		String[] tokens = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String allLines = new String();
			String line = null;
			while ((line = reader.readLine()) != null) {
				allLines += line.toLowerCase() + " "; // case folding
			}
			tokens = allLines.split("[ (@){}\",?.&%$#!+-:;]+");
			for (String string : tokens) {
				string = stemWord(string);
				list.add(string);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return list;
	}

	/**
	 * @param query
	 *            : phrase query to be searched
	 * @return returns list of document ids containing the query
	 */

	public HashSet<Integer> searchPositional(String query) {
		String phraseQuery[] = query.split(" ");
		ArrayList<HashMap<Integer, ArrayList<Integer>>> listOfDoc = new ArrayList<HashMap<Integer, ArrayList<Integer>>>();
		for (int i = 0; i < phraseQuery.length; i++) {
			if (!termListPositional.containsKey(phraseQuery[i])) {
				return new HashSet<>();
			} else {
				HashMap<Integer, ArrayList<Integer>> docWithPosition = termListPositional.get(phraseQuery[i]);
				listOfDoc.add(docWithPosition);
			}
		}
		HashMap<Integer, ArrayList<Integer>> intermediate = null;
		if (listOfDoc != null || listOfDoc.size() != 0) {
			intermediate = listOfDoc.get(0);
		}
		for (int i = 1; i < listOfDoc.size(); i++) {
			HashMap<Integer, ArrayList<Integer>> tempList = listOfDoc.get(i);
			intermediate = intersectPositional(intermediate, tempList);

		}
		HashSet<Integer> result = new HashSet<Integer>();
		result.addAll(intermediate.keySet());
		return result;

	}

	/**
	 * @param l1
	 *            list of documents to be merged
	 * @param l2
	 *            list of documents to be merged
	 * @return intersect of 2 lists
	 */
	public HashMap<Integer, ArrayList<Integer>> intersectPositional(HashMap<Integer, ArrayList<Integer>> l1,
			HashMap<Integer, ArrayList<Integer>> l2) {
		HashMap<Integer, ArrayList<Integer>> docIds = new HashMap<Integer, ArrayList<Integer>>();
		for (Integer key : l1.keySet()) {
			if (l2.containsKey(key)) {
				ArrayList<Integer> finalList = new ArrayList<Integer>();
				ArrayList<Integer> temp1 = l1.get(key);
				for (int i = 0; i < temp1.size(); i++) {
					int position = temp1.get(i);
					if (l2.get(key).contains(position + 1)) {
						finalList.add(position + 1);
					}
				}
				if (finalList.size() != 0) {
					docIds.put(key, finalList);
				}
			}
		}
		return docIds;
	}

	/**
	 * @param l1
	 *            list of documents to be merged
	 * @param l2
	 *            list of documents to be merged
	 * @return intersect of 2 lists
	 */
	public HashSet<Integer> merge(Set<Integer> l1, Set<Integer> l2) {
		HashSet<Integer> mergedList = new HashSet<Integer>();
		for (Integer integer : l1) {
			mergedList.add(integer);
		}
		for (Integer integer : l2) {
			mergedList.add(integer);
		}
		return mergedList;
	}

	/**
	 * 
	 * @param query
	 *            check
	 * @return
	 */
	public HashMap<Integer, Integer> search(String query) {
		if (termListInverted.containsKey(query)) {
			return termListInverted.get(query);
		} else {
			return null;
		}
	}

	public HashSet<Integer> searchMultipleWords(String userQuery) {

		String[] query = userQuery.split(" ");
		HashMap<String, HashMap<Integer, Integer>> map = new HashMap<>();
		for (String string : query) {
			HashMap<Integer, Integer> set = search(stemWord(string.toLowerCase()));
			if (set != null) {
				if (!map.containsKey(string)) {
					map.put(string, set);
				}
			}
		}
		// TreeSet for storing the results of terms in increasing order of the
		// size of postings
		TreeSet<Map.Entry<String, HashMap<Integer, Integer>>> result = new TreeSet<>(
				// Compare the values in set
				new Comparator<Map.Entry<String, HashMap<Integer, Integer>>>() {
					@Override
					public int compare(Entry<String, HashMap<Integer, Integer>> o1,
							Entry<String, HashMap<Integer, Integer>> o2) {
						int result = o1.getValue().size() - o2.getValue().size();
						if (result == 0) {
							return o1.getKey().compareTo(o2.getKey());
						}
						return result;
					}
				});

		for (Entry<String, HashMap<Integer, Integer>> entry : map.entrySet()) {
			result.add(entry);
		}
		int count = 0;
		Iterator<Map.Entry<String, HashMap<Integer, Integer>>> it = result.iterator();
		// Store the result of merging
		Set<Integer> resultSet = null;
		while (it.hasNext()) {
			Map.Entry<String, HashMap<Integer, Integer>> list = it.next();
			resultSet = list.getValue().keySet();
			count++;
			while (it.hasNext()) {
				Map.Entry<String, HashMap<Integer, Integer>> list1 = it.next();
				count++;
				resultSet = merge(resultSet, list1.getValue().keySet());
			}
		}
		HashSet<Integer> set = new HashSet<>();
		if (resultSet != null) {
			set.addAll(resultSet);
		}
		return set;

	}

	class ProcessQuery extends Thread {
		HashMap<Integer, Double> result;
		HashSet<Integer> queryResult;
		String query;
		int type;

		public ProcessQuery(HashMap<Integer, Double> result, String query, int type) {
			this.result = result;
			queryResult = new HashSet<>();
			this.query = query;
			this.type = type;
		}

		@Override
		public void run() {
			if (type == 1) {
				queryResult.addAll(searchMultipleWords(query));
				if (!queryResult.isEmpty()) {
					calculateTFIDFScoreBoolean(result, queryResult, query);
				}
			} else {
				queryResult = searchPositional(query);
				if (!queryResult.isEmpty()) {
					calculateTFIDFScorePositional(result, queryResult, query);
				}
			}

		}
	}

	public ArrayList<QuestionNode> processQuery(String query) {
		query = query.toLowerCase();
		HashMap<Integer, Double> booleanResult = new HashMap<>();
		HashMap<Integer, Double> positionalResult = new HashMap<>();
		ProcessQuery booleanQueries = new ProcessQuery(booleanResult, query, 1);
		booleanQueries.start();
		ProcessQuery booleanQueries1 = new ProcessQuery(positionalResult, query, 2);
		booleanQueries1.start();
		try {
			booleanQueries.join();
			booleanQueries1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<QuestionNode> result = new ArrayList<>();
		if (!positionalResult.isEmpty()) {
			getResults(result, positionalResult, 0);
			for (Integer id : positionalResult.keySet()) {
				if (booleanResult.containsKey(id)) {
					booleanResult.remove(id);
				}
			}
		}
		getResults(result, booleanResult, 0.05);
		return result;

	}

	public void calculateTFIDFScorePositional(HashMap<Integer, Double> tfIdf, HashSet<Integer> listOfDocs,
			String query) {
		int N = questionDB.size();
		String[] queryTokens = query.split(" ");
		int pattern[] = new int[query.length()];
		patternArray(pattern, query);
		int doccumentFreQ = listOfDocs.size();
		for (Integer docId : listOfDocs) {
			double tfidfScore = 0;
			int termFrequency = 0;
			QuestionNode node = questionDB.get(docId);
			String str = null;
			if (node.title != null) {
				str = node.title + " " + node.body;
			} else {
				str = node.body;
			}

			int i = 0;
			int j = 0;
			while (i < str.length()) {
				if (str.charAt(i) == query.charAt(j)) {
					i++;
					j++;
				}
				if (j == query.length()) {
					termFrequency += 10;
					j = pattern[j - 1];
				}
				if (i < str.length() && str.charAt(i) != query.charAt(j)) {
					if (j != 0) {
						j = pattern[j - 1];
					} else {
						i++;
					}
				}
			}
			tfidfScore = (Math.log(1 + termFrequency) * Math.log(N / doccumentFreQ));
			double docTFIDF = 0;
			for (String token : queryTokens) {
				int dFreQ = 0;
				double tfidf = 0;
				if (termListPositional.containsKey(token)) {
					dFreQ = termListPositional.get(token).size();
					if (termListPositional.containsKey(token)) {
						HashMap<Integer, ArrayList<Integer>> temp = termListPositional.get(token);
						if (temp.containsKey(docId)) {
							QuestionNode post = questionDB.get(docId);
							String titleString = post.title;
							String[] tokens = titleString.split(" ");
							Set<String> titleToken = new HashSet<String>(Arrays.asList(tokens));
							tfidf = (Math.log(1 + temp.get(docId).size()) * Math.log(N / dFreQ));
						}
					}
					docTFIDF = docTFIDF + tfidf;
				}
			}
			tfIdf.put(docId, tfidfScore + docTFIDF);
		}

		normalizedTFIDF(tfIdf, query.length());

	}

	public void patternArray(int[] p, String pattern) {
		if (p.length == 1) {
			return;
		}
		int j = 0;
		int i = 1;
		while (i < p.length) {
			if (pattern.charAt(i) == pattern.charAt(j)) {
				p[i] = j + 1;
				i++;
				j++;
			} else {
				if (j != 0) {
					j = p[j - 1];
				} else {
					i++;
				}
			}
		}
	}

	public void calculateTFIDFScoreBoolean(HashMap<Integer, Double> tfIdf, HashSet<Integer> listOfDocs, String query) {
		String[] queryTokens = query.split(" ");
		int N = questionDB.size();
		for (Integer docId : listOfDocs) {
			double docTFIDF = 0;
			for (String token : queryTokens) {
				token = stemWord(token);
				int doccumentFreQ = 0;
				double tfidf = 0;
				if (termListInverted.containsKey(token)) {
					doccumentFreQ = termListInverted.get(token).size();
					int termFrequency = 0;
					if (termListInverted.containsKey(token)) {
						HashMap<Integer, Integer> temp = termListInverted.get(token);
						if (temp.containsKey(docId)) {
							termFrequency = temp.get(docId);
							tfidf = (Math.log(1 + termFrequency) * Math.log(N / doccumentFreQ));
						}
					}
					docTFIDF = docTFIDF + tfidf;
				}
			}
			tfIdf.put(docId, docTFIDF);
		}

		normalizedTFIDF(tfIdf, query.length());

	}

	public void normalizedTFIDF(HashMap<Integer, Double> tfifd, int length) {
		double resultNormalized = 0;
		for (Integer key : tfifd.keySet()) {
			Double tfidfValue = tfifd.get(key);
			resultNormalized = resultNormalized + (tfidfValue * tfidfValue);
		}

		double sqrt = Math.sqrt(resultNormalized);
		double normalizedfactor = length * sqrt;

		for (Integer key : tfifd.keySet()) {
			Double tfidfValue = tfifd.get(key) / normalizedfactor;
			tfifd.put(key, tfidfValue * 10);
		}
	}

	public void getResults(ArrayList<QuestionNode> result, HashMap<Integer, Double> query, double score) {

		TreeSet<Map.Entry<Integer, Double>> set = new TreeSet<>(
				// Compare the values in set
				new Comparator<Map.Entry<Integer, Double>>() {
					@Override
					public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
						if (o1.getValue() == o2.getValue()) {
							return o1.getKey() - o2.getKey();
						}
						if (o1.getValue() < o2.getValue()) {
							return 1;
						} else {
							return -1;
						}
					}
				});
		for (Map.Entry<Integer, Double> entry : query.entrySet()) {
			set.add(entry);
		}
		// System.out.println(set.size());
		for (Entry<Integer, Double> entry : set) {
			// System.out.println(entry.getValue() + " " + entry.getKey());
			// if (entry.getValue() >= score) {
			result.add(questionDB.get(entry.getKey()));
			// }
		}
	}

	public ArrayList<AnswerNode> getAnswers(int postId) {
		ArrayList<AnswerNode> results = new ArrayList<>();
		QuestionNode docID = questionDB.get(postId);
		int acceptedAnswerId = docID.acceptedAnswerId;
		HashSet<Integer> answerList = docID.listOfAnswers;
		if (acceptedAnswerId != -1) {
			if (answerDB.get(acceptedAnswerId) != null) {
				results.add(answerDB.get(acceptedAnswerId));
				answerList.remove(acceptedAnswerId);
			}
		}

		TreeSet<AnswerNode> set = new TreeSet<>(new Comparator<AnswerNode>() {
			public int compare(AnswerNode a1, AnswerNode a2) {
				return -(a1.score - a2.score);
			}
		});
		for (Integer i : answerList) {
			if (answerDB.get(i) != null) {
				set.add(answerDB.get(i));
			} else {
				// System.out.println("Answer : "+answerDB.get(i) + " " + i);
			}
		}

		for (AnswerNode node : set) {
			results.add(node);
		}

		return results;

	}

	public static QueryProcessing getInstance() {
		if (instance == null) {
			instance = new QueryProcessing();
		}
		return instance;
	}

	public static void main(String[] args) {
		QueryProcessing bp = new QueryProcessing();
		// System.out.println(bp.termListInverted.size());
		// System.out.println(bp.termListPositional.size());
		String str = "deep learning";
		ArrayList<QuestionNode> results = bp.processQuery(str);
		// System.out.println(results.size());
		/*
		 * for (QuestionNode node : results) {
		 * 
		 * System.out .println(
		 * "---------------------------------------------------------------------------------------"
		 * ); System.out.println(node.id + " : " + node.title);
		 * System.out.println(node.body);
		 * 
		 * 
		 * ArrayList<AnswerNode> an = bp.getAnswers(node.id);
		 * 
		 * for (AnswerNode tnode : an) { System.out .println(
		 * "************************************************************************************************"
		 * ); System.out.println("Answer : "+tnode.id + " : " + tnode.score);
		 * System.out.println(tnode.body);
		 * 
		 * }
		 * 
		 * 
		 * }
		 */

	}
}