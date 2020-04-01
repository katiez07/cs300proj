package edu.cs300;
import CtCILibrary.*;
import java.util.concurrent.*;
import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.*;

// @author Katie Zucker, krzucker@crimson.ua.edu, CWID 11624565


/* Passage Processor builds Tries from the passages and searches those tries
* for the longest word which contains the prefix
* Passage Processor reqs:
*    - Written in Java with the main function in edu.cs300.PassageProcessor.java
*    - Read passage file names from passages.txt in root directory (hardcode the name)
*    - Read contents of each passages file in the root directory
*/
public class PassageProcessor{

	public static void main(String[] args){
		ArrayList<String> names = passageNames();
		int numPassages = names.size();

		// create arrays of words for each passage
		ArrayList<String[]> preTries = new ArrayList<String[]>();
		for (int a=0; a<numPassages; a++){
			preTries.add(passageToArr(names.get(a)));
		}

		// set up ArrayBlocking queues for each worker and each resultsOutputArray
		ArrayBlockingQueue[] workers = new ArrayBlockingQueue[numPassages];
		ArrayBlockingQueue resultsOutputArray = new ArrayBlockingQueue(numPassages*10);

// for testing
/*
		if (args.length == 0){
			System.out.println("Please provide a prefix arg");
		}
		else if (args[0].length() <= 2){
			System.out.println("Provide prefix (min 3 characters) for search");
			System.exit(0);
		}
		else if (args[0].length() >= 10){
			System.out.println("Please provide a shorter prefix (<10 characters)");
			System.exit(0);
		}
*/

		for (int i=0; i<numPassages; i++){
			workers[i] = new ArrayBlockingQueue(10);
		}

		// start the workers
		for (int j=0; j<numPassages; j++){
			new Worker(preTries.get(j), j, names.get(j), workers[j], resultsOutputArray).start();
		}

		// loop to check System V queue
		while (true){
			int qid = 9404478; //hard-coded for robustness
			System.out.println(new MessageJNI().readStringMsg("krzucker", qid);
			SearchRequest sr = new MessageJNI().readPrefixRequestMsg();
			assert sr != null;

			// System.out.println(sr);  //for testing

			int requestID = sr.requestID;
			String prefix = sr.prefix;

			try {
				for (int k=0; k<numPassages; k++){
					workers[k].put(prefix);
				}
			}
			catch (InterruptedException e){
			}

			// loop through passages
			int counter=0;
			while (counter < numPassages){
				try {
					String results = (String)resultsOutputArray.take();
					System.out.println("results:" + results);

					//get info back out of resultsOutputArray by finding index of '='
					int ind = results.indexOf(61);
					String pName = results.substring(0, ind-1);
					String longestWord = results.substring(ind+3);
					int found;
					if (results.trim().isnull()) found = 0;
					else found = 1;

					// send msg back through SystemVQ
					new MessageJNI().writeLongestWordResponse(id, prefix, counter, pName, longestWord, numPassages, found);
					counter++;
				}
				catch(InterruptedException e){
				}
			}
		}
	} 

	// reads the file "passages.txt" from the home directory and
	// returns an ArrayList of the passage names
	public static ArrayList<String> passageNames(){
		try{
		 	ArrayList<String> names = new ArrayList<String>();
			File file = new File("passages.txt");
			assert file.exists();
			Scanner sc = new Scanner(file);
			while (sc.hasNextLine()){
				String n = sc.nextLine().trim();
				if (n.length() > 30){
					n = n.substring(0,29);
				}
				names.add(n);
			}
			return names;
		}
		catch(Exception e){
			System.out.println("Exception reading passages.txt: " + e.getMessage());
			return null;
		}
	}

	// takes a passage name and returns an array of all the valid words
	// NOTE: assuming words at the end of a sentence count as including 
	//	punctuation, so they are thrown out (i.e. "out" in this sentence
	//	is thrown out).
	public static String[] passageToArr(String name){
		// first make the passage into an ArrayList of Strings for each valid word
		// assuming passages have <500000 words
		String[] words = new String[500000];
		int i = 0;

		try{
			File f = new File(name);
			assert f.exists();
			BufferedReader br = new BufferedReader(new FileReader(f));
			String s;
			String word = "";
			while ((s = br.readLine()) != null){
				char c;
				int count = 0;
				boolean isTrash = false;
				for (int k=0; k<s.length(); k++){
					c = s.charAt(k);

					if ((c == ' ' || c == '\t' ||  // char is space and !isTrash => new word
						c == '\n' || c == '\r')
						&& !isTrash){
						assert word.length() < 30;
						words[i] = word.toLowerCase();
						i++;
						word = "";
						count = 0;
						isTrash = false;
					}
					else if (c == ' ' || c == '\t' ||  // char is space and isTrash => new word
						c == '\n' || c == '\r'){
						word = "";
						count = 0;
						isTrash = false;
					}
					else if (count >= 100){  // don't add words > 100 characters
						isTrash = true;
					}
					else if ((c > 64 && c < 91) ||  // char is letter => add it to word
						(c > 96 && c < 123)){
						word += c;
						count++;
					}
					else{
						count++;
						isTrash = true;
					}
				}
			}
		}
		catch(IOException e){
			System.out.println("IOException building tree from " + 
				name + ": " + e.getMessage());
			return null;
		}
		return words;
	}	


}


