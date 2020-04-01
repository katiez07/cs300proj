package edu.cs300;
import CtCILibrary.*;
import java.util.concurrent.*;
import java.util.HashMap;

// initial file from Dr. Anderson's github
// modified by Katie Zucker, krzucker@crimson.ua.edu, CWID 11624565

class Worker extends Thread{

	Trie textTrieTree;
	ArrayBlockingQueue prefixRequestArray;
	ArrayBlockingQueue resultsOutputArray;
	int id;
	String passageName;

	public Worker(String[] words,int id, String name, ArrayBlockingQueue prefix, ArrayBlockingQueue results){
		this.textTrieTree=new Trie(words);
		this.prefixRequestArray=prefix;
		this.resultsOutputArray=results;
		this.id=id;
		this.passageName="Passage "+Integer.toString(id)+ " - " + name;//put name of passage here
	}

	public void run() {
		System.out.println("Worker-"+this.id+" ("+this.passageName+") thread started ...");
		//while (true){
		try {
			String prefix=(String)this.prefixRequestArray.take();

/*
			boolean found = this.textTrieTree.contains(prefix);

			if (!found){
				//System.out.println("Worker-"+this.id+" "+req.requestID+":"+ prefix+" ==> not found ");
				resultsOutputArray.put(passageName+":"+prefix+" not found");
			} else{
				//System.out.println("Worker-"+this.id+" "+req.requestID+":"+ prefix+" ==> "+word);
				resultsOutputArray.put(passageName+":"+prefix+" found");
			}
*/

			String longestWord = findLongestWord(this.textTrieTree, prefix);

			if (longestWord == ""){
				resultsOutputArray.put(passageName + ":" + prefix + " ==> not found");
			}
			else{
				resultsOutputArray.put(passageName + ":" + prefix + " ==> " + longestWord);
			}
		} 
		catch(InterruptedException e){
			System.out.println(e.getMessage());
		}
	}

	// recursively searches the trie for words containing the prefix, 
	// continually updating longestWord with the new longest word found
	public String findLongestWord(Trie trie, String prefix){
		return beforePrefixFound(trie.getRoot(), "", "", prefix, 0);
	}

	// before prefix is found, use extra parameters to first determine
	// that the word contains the prefix
	private String beforePrefixFound(TrieNode trie, String longestWord, String word, String prefix, int letter){
		for (int i=97; i<123; i++){
			char ch = (char)i;
			TrieNode newTrie = trie.getChild(ch);
			String newWord = word + ch;
			//System.out.println("looking for " + prefix +
			//	" at index " + letter); 
			if (newTrie == null){
			}
			else{
				if (newTrie.getChar() == prefix.charAt(letter)){
					if (letter+1 == prefix.length()){  //entire prefix has been found
						return afterPrefixFound(newTrie, newWord, newWord);
					}
					else{
						return beforePrefixFound(newTrie, longestWord, newWord, prefix, letter+1);
					}
				}
				else{
					longestWord = beforePrefixFound(newTrie, longestWord, newWord, prefix, 0);
				}
			}
		}
		return longestWord;	
}

	// after prefix is found, figure out the longest word without worrying
	// about the prefix
	private String afterPrefixFound(TrieNode trie, String longestWord, String word){
		if (trie == null || trie.terminates()){
			System.out.println("Unknown error: trie is null");
			System.exit(666);
		}
		for (int a=97; a<123; a++){
			char ch = (char)a;
			TrieNode newTrie = trie.getChild(ch);
			if (newTrie == null || newTrie.terminates()){
				if (word.length() > longestWord.length()){
					longestWord = word;
				}
			}
			else{
				longestWord = afterPrefixFound(newTrie, longestWord, word + ch);
			}
			
		}

		return longestWord;
	}

}
