package controller;
import java.util.HashSet;

public class QuestionNode {
	public int id;
	public int acceptedAnswerId;
	public String body;
	public String title;
	public int score;
	public HashSet<Integer> listOfAnswers;
	
	public QuestionNode() {
		listOfAnswers = new HashSet<>();
	}
	
}
