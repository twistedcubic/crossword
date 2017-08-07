package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Generate a crossword puzzle given a set of words.
 * @author yihed
 *
 */
public class Crossword {
	
	//length of the crossword, one dimension
	private static final int BOARD_LEN = 200;
	
	/**
	 * A node used during the nodes gathering process.
	 */
	private static class WordNode{
		
		char letter;
		//WordNode nextNode;
		/*WordNode upNode;
		WordNode downNode;
		WordNode leftNode;
		WordNode rightNode;*/
		//row number
		int row;
		//col number
		int col;		
		WordNode(char letter_, int row_, int col_){
			this.letter = letter_;
			this.row = row_;
			this.col = col_;
			//this.nextNode = nextNode_;
		}		
		/**
		 * Whether this node is part of a horizontal word.
		 * @return
		 */
		/*boolean isHorizontalWord(){
			if(leftNode != null && leftNode.row == row - 1){
				return true;
			}
			if(rightNode != null && rightNode.row == row + 1){
				return true;
			}
			return false;
		}*/	
	}
	//whether word is horizontal or vertical on the board.
			private enum WordOrientation{
				HORIZONTAL, VERTICAL;
			}
			
	/**
	 * 
	 */
	private static class Board{
		
		BoardNode[][] board = new BoardNode[BOARD_LEN][BOARD_LEN];
		private static final char PLACEHOLDER_CHAR = ' ';
		
		//set of rows and columns containing nontrivial letter
		private Set<Integer> rowSet = new HashSet<Integer>();
		private Set<Integer> colSet = new HashSet<Integer>();
		//set of board position, 
		private Set<BoardPosition> leafBoardPosSet = new HashSet<BoardPosition>();
		private BoardPosition rootBoardPosition;
		
		public Board(String firstWord, List<String> remainingWordsList){
			//populate the starting row and column. Don't need this if not doing 
			//linked list like construction.
			/*for(int i = 0; i < BOARD_LEN; i++){
				board[0][i] = new WordNode(PLACEHOLDER_CHAR, 0, i);
				board[i][0] = new WordNode(PLACEHOLDER_CHAR, i, 0);
			}*/
			//insert first word horizontally. starting at center of board
			int boardMiddle = BOARD_LEN/2;
			int startingCol = boardMiddle - firstWord.length()/2;
			insertWord(firstWord, boardMiddle, startingCol, WordOrientation.HORIZONTAL, rootBoardPosition);
			leafBoardPosSet.add(rootBoardPosition);
			
			rootBoardPosition = new BoardPosition(null, remainingWordsList);
			/*rowSet.add(boardMiddle);
			for(int i = 0; i < firstWord.length(); i++){
				board[boardMiddle][startingCol+i] = firstWord.charAt(i);
				colSet.add(startingCol + i);
			}*/			
		}
		
		/**
		 * Builds the board.
		 */
		void build(){
			
			leafBoardPosSet;
			
			List<List<WordNode>> rowWordNodeList;
			List<List<WordNode>> colWordNodeList;
			
			//get list of leaf nodes
			BoardPosition boardPosition = ;
			board.gatherWordNodes(, rowWordNodeList, colWordNodeList);
			
			//make sure added words are removed!
			
			findLegalWordInsertion
		}
		
		/**
		 * Legality of adding the word must be determined before calling this.
		 * @param word
		 * @param rowStart
		 * @param colStart
		 * @param orient
		 * @param boardPos
		 */
		void insertWord(String word, int rowStart, int colStart, WordOrientation orient,
				BoardPosition boardPos){
			if(WordOrientation.HORIZONTAL == orient){			
				for(int i = 0; i < word.length(); i++){
					char curChar = word.charAt(i);
					BoardNode node = board[rowStart][colStart+i];
					if(null == node){
						node = new BoardNode(curChar);
						board[rowStart][colStart+i] = node;
					}
					node.addBoardPosition(boardPos);
					colSet.add(colStart + i);
				}	
				rowSet.add(rowStart);
			}else{
				for(int i = 0; i < word.length(); i++){
					char curChar = word.charAt(i);
					BoardNode node = board[rowStart+i][colStart];
					if(null == node){
						node = new BoardNode(curChar);
						board[rowStart+i][colStart] = node;
					}
					node.addBoardPosition(boardPos);
					rowSet.add(rowStart+i);
				}
				colSet.add(colStart);
			}
			
		}
		   
		/**
		 * Fill up given lists with WordNode's    for given board position
		 * @param boardPosition
		 * @param rowWordNodeList
		 * @param colWordNodeList
		 */
		void gatherWordNodes(BoardPosition boardPosition, List<List<WordNode>> rowWordNodeList, 
				List<List<WordNode>> colWordNodeList){
			
			//gather row nodes
			for(int rowNum : rowSet){
				List<WordNode> nodeList = new ArrayList<WordNode>();
				for(int i = 0; i < BOARD_LEN; i++){
					BoardNode boardNode = this.board[rowNum][i];
					if(null == boardNode){
						continue;
					}
					if(boardNode.containsBoardPosition(boardPosition)){
						nodeList.add(new WordNode(boardNode.letter, rowNum, i));						
					}					
				}
				rowWordNodeList.add(nodeList);
			}
			//gather column nodes
			for(int colNum : colSet){
				List<WordNode> nodeList = new ArrayList<WordNode>();
				for(int i = 0; i < BOARD_LEN; i++){
					BoardNode boardNode = this.board[i][colNum];
					if(null == boardNode){
						continue;
					}
					if(boardNode.containsBoardPosition(boardPosition)){
						nodeList.add(new WordNode(boardNode.letter, i, colNum));						
					}					
				}	
				colWordNodeList.add(nodeList);
			}			
		}
		
	}//end of Board class
	
	/**
	 * Each node contains set of Board positions and the char in that node.
	 */
	private static class BoardNode{
		char letter;
		Set<BoardPosition> boardPositionSet;
		
		BoardNode(char letter_){
			this.letter = letter_;
			this.boardPositionSet = new HashSet<BoardPosition>();
		}
		
		void addBoardPosition(BoardPosition boardPosition){
			this.boardPositionSet.add(boardPosition);
		}
		
		boolean containsBoardPosition(BoardPosition boardPosition){
			if(this.boardPositionSet.contains(boardPosition)){
				return true;
			}
			//check parents
			BoardPosition parentPos = boardPosition.parentPosition;
			while(null != parentPos){
				if(this.boardPositionSet.contains(parentPos)){
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Board position recording the current position in the board tree.
	 * equals and hashcode uses default reference equality.
	 */
	private static class BoardPosition{
		BoardPosition parentPosition;
		//set of words remaining for this position.
		List<String> remainingWordsList;
		int wordIntersectionCount = 0;
		
		BoardPosition(BoardPosition parentPosition_, List<String> remainingWordsList_){
			this.parentPosition = parentPosition_;
			this.remainingWordsList = remainingWordsList_;
		}
		
		//remove words
		
		private static class WordWithWordNodes{
			String word;
			List<WordNode> wordNodesList;
			WordWithWordNodes(String word_, List<WordNode> wordNodesList_){
				this.word = word_;
				this.wordNodesList = wordNodesList_;
			}
		}
		
		/**
		 * Given row and column lists for given board, try to fit in one remaning word. 
		 * Produce list of children BoardPosition's. 
		 * Only add one word, but could be at multiple positions.
		 * @param rowWordNodeList
		 * @param colWordNodeList
		 * @return list of children BoardPosition's.
		 */
		public List<BoardPosition> findLegalWordInsertion(List<List<WordNode>> rowWordNodeList, 
				List<List<WordNode>> colWordNodeList, Board board){
			//keys are number of intersections, values are words and that intersection set
			TreeMap<Integer, List<WordWithWordNodes>> tMap = new TreeMap<Integer, List<WordWithWordNodes>>();
			
			int horizontalMax = getHorizontalWords(rowWordNodeList, tMap);
			
			//check columns			
			TreeMap<Integer, List<WordWithWordNodes>> colTMap = new TreeMap<Integer, List<WordWithWordNodes>>();
			int verticalMax = 0;//getHorizontalWords(rowWordNodeList, tMap);
			
			List<BoardPosition> childrenBoardPositionList = new ArrayList<BoardPosition>();
			WordOrientation orient = horizontalMax > verticalMax ? WordOrientation.HORIZONTAL
					: (horizontalMax < verticalMax ? WordOrientation.VERTICAL : 
						//must be equal
						null ) ;
			
			TreeMap<Integer, List<WordWithWordNodes>> chosenMap = (orient == WordOrientation.HORIZONTAL 
					? tMap : (orient == WordOrientation.VERTICAL ? colTMap : null));
			
			if(null != chosenMap){				
				addWordNodes(board, childrenBoardPositionList, orient, chosenMap);			
			}else if(horizontalMax > 0){
				//must be equal
				addWordNodes(board, childrenBoardPositionList, WordOrientation.HORIZONTAL, tMap);
				addWordNodes(board, childrenBoardPositionList, WordOrientation.VERTICAL, colTMap);
			}
			else{
				/*add one-intersection word, only consider next longest word*/
				//WordWithWordNodes list to record the positions filled
				List<WordWithWordNodes> wordWithWordNodesList = new ArrayList<WordWithWordNodes>();
				getSingleIntersectionWords(rowWordNodeList,
						wordWithWordNodesList, WordOrientation.HORIZONTAL);
				getSingleIntersectionWords(rowWordNodeList,
						wordWithWordNodesList, WordOrientation.VERTICAL);
				//add one horizontally, one vertically
				int intersectionCount = 1;
				addWordNodesFromList(board, childrenBoardPositionList, WordOrientation.HORIZONTAL, 
						wordWithWordNodesList, intersectionCount);
				addWordNodesFromList(board, childrenBoardPositionList, WordOrientation.VERTICAL, 
						wordWithWordNodesList, intersectionCount);
				
			}
			
			return childrenBoardPositionList;
		}

		/**
		 * @param board
		 * @param childrenBoardPositionList
		 * @param orient
		 * @param chosenMap
		 */
		private void addWordNodes(Board board, List<BoardPosition> childrenBoardPositionList, WordOrientation orient,
				TreeMap<Integer, List<WordWithWordNodes>> chosenMap) {
			int maxIntersectionCount = chosenMap.descendingKeySet().iterator().next();
			List<WordWithWordNodes> maxIntersectionCountList = chosenMap.get(maxIntersectionCount);
			addWordNodesFromList(board, childrenBoardPositionList, orient, maxIntersectionCountList,
					maxIntersectionCount);
		}

		private void addWordNodesFromList(Board board, List<BoardPosition> childrenBoardPositionList,
				WordOrientation orient, List<WordWithWordNodes> maxIntersectionCountList,
				int intersectionCount) {
			if(!maxIntersectionCountList.isEmpty()){
				this.wordIntersectionCount += intersectionCount;
			}
			//create BoardPosition's for top-ranking
			for(WordWithWordNodes wordWithWordNodes : maxIntersectionCountList){
				//only need first one
				WordNode firstWordNode = wordWithWordNodes.wordNodesList.get(0);
				String word = wordWithWordNodes.word;
				List<String> childRemainingWordsList = new ArrayList<String>(this.remainingWordsList);
				childRemainingWordsList.remove(word);
				BoardPosition boardPos = new BoardPosition(this, childRemainingWordsList);
				board.insertWord(word, firstWordNode.row, firstWordNode.col, orient, boardPos);					
				childrenBoardPositionList.add(boardPos);
			}
		}

		private void getSingleIntersectionWords(List<List<WordNode>> rowWordNodeList,
				List<WordWithWordNodes> wordWithWordNodesList, WordOrientation wordOrientation) {
			
			String nextLongestWord = this.remainingWordsList.get(0);					
			char[] wordCharAr = nextLongestWord.toCharArray();
			
			for(List<WordNode> wordNodeList: rowWordNodeList){
				//a row
				//space to prior word
				int prevSpace = 100;				
				for(int i = 0; i < wordNodeList.size(); i++){
					//check for two or more intersections first
					WordNode wordNode = wordNodeList.get(i);
					//WordNode nextWordNode = wordNodeList.get(i+1);
					int postSpace;					
					if(WordOrientation.HORIZONTAL == wordOrientation){
						int colNum = wordNode.col;
						//int nextColNum = nextWordNode.col;
						//int colDiff = nextColNum - colNum;
						postSpace = i == wordNodeList.size()-1 
								? 100 : wordNodeList.get(i+1).col - colNum;						
					}else{
						int rowNum = wordNode.row;
						postSpace = i == wordNodeList.size()-1 
								? 100 : wordNodeList.get(i+1).row - rowNum;
					}
					char wordNodeChar = wordNode.letter;
					
						for(int j = 0; j < wordCharAr.length; j++){
							char curChar = wordCharAr[i];
							//int intersectionCount;
							if(prevSpace > j
									&& curChar == wordNodeChar
									&& j + postSpace > wordCharAr.length 
									//&& wordCharAr[j+colDiff] == nextWordNodeChar
											//check the word fits wrt remaining words
									/*&& (intersectionCount = remainingWordFitsSingleton(wordCharAr, j+colDiff, 
											wordNodeList, i+1, WordOrientation.HORIZONTAL))>0*/
									){								
								//add previous intersection
								List<WordNode> curWordNodeList = new ArrayList<WordNode>();
								curWordNodeList.add(wordNode);
								//curWordNodeList.add(nextWordNode);
								//int key = intersectionCount + 2;
								/*if(key > horizontalMax){
									horizontalMax = key;
								}
								List<WordWithWordNodes> list = tMap.get(key);
								if(null == list){
									list = new ArrayList<WordWithWordNodes>();									
								}*/
								wordWithWordNodesList
									.add(new WordWithWordNodes(nextLongestWord, curWordNodeList));
								//only add the first suitable place for one-intersection words.
								return;
							}							
						}						
					//}
					prevSpace = postSpace;					
				}
			}
			//return horizontalMax;
		}
		/**
		 * @param rowWordNodeList
		 * @param tMap
		 */
		private int getHorizontalWords(List<List<WordNode>> rowWordNodeList,
				TreeMap<Integer, List<WordWithWordNodes>> tMap) {
			
			int horizontalMax = 0;
			for(List<WordNode> wordNodeList: rowWordNodeList){
				//a row
				//space to prior word
				int prevSpace = 100;				
				for(int i = 0; i < wordNodeList.size()-1; i++){
					//check for two or more intersections first
					WordNode wordNode = wordNodeList.get(i);
					WordNode nextWordNode = wordNodeList.get(i+1);
					int colNum = wordNode.col;
					int nextColNum = nextWordNode.col;
					int colDiff = nextColNum - colNum;
					char wordNodeChar = wordNode.letter;
					char nextWordNodeChar = nextWordNode.letter;
					//int postSpace = 0     ;
					//look over all remaining words
					for(String word : remainingWordsList){
						char[] wordCharAr = word.toCharArray();
						for(int j = 0; j < wordCharAr.length; j++){
							char curChar = wordCharAr[i];
							int intersectionCount;
							if(prevSpace > j
									&& curChar == wordNodeChar
									&& j + colDiff < wordCharAr.length 
									&& wordCharAr[j+colDiff] == nextWordNodeChar
											//check the word fits wrt remaining words
									&& (intersectionCount = remainingWordFits(wordCharAr, j+colDiff, 
											wordNodeList, i+1, WordOrientation.HORIZONTAL))>0){
								
								//add previous two intersection
								List<WordNode> curWordNodeList = new ArrayList<WordNode>();
								curWordNodeList.add(wordNode);
								curWordNodeList.add(nextWordNode);
								for(int k = 0; k < intersectionCount; k++){
									curWordNodeList.add(wordNodeList.get(i+k+2));
								}
								int key = intersectionCount + 2;
								if(key > horizontalMax){
									horizontalMax = key;
								}
								List<WordWithWordNodes> list = tMap.get(key);
								if(null == list){
									list = new ArrayList<WordWithWordNodes>();									
								}
								list.add( new WordWithWordNodes(word, curWordNodeList));
								tMap.put(key, list);
							}							
						}						
					}
					prevSpace = colDiff;					
				}
			}
			return horizontalMax;
		}
		
		/**
		 * Determines if the remaining letters in word fits
		 * wrt rest of grid.
		 * @param wordCharAr
		 * @param wordCharArIndex
		 * @param wordNodeList
		 * @param wordNodeListIndex
		 * @return 0 or more means that number intersection, -1 means don't fit, 
		 */
		private int remainingWordFits(char[] wordCharAr, int wordCharArIndex, List<WordNode> wordNodeList, 
				int wordNodeListIndex, WordOrientation wordOrientation){
			if(wordNodeList.size() == wordNodeListIndex+1){
				return 0;
			}
			int remainCharCount = wordCharAr.length - (wordCharArIndex+1);
			int count = 0;
			for(int i = wordNodeListIndex; i < wordNodeList.size()-1; i++){
				WordNode node = wordNodeList.get(i);
				WordNode nextNode = wordNodeList.get(i+1);
				int diff = wordOrientation== WordOrientation.HORIZONTAL 
						? nextNode.col - node.col 
						: nextNode.row - node.row;
				if(diff > remainCharCount ){
					//count++;
					return count;
				}else if(nextNode.letter == wordCharAr[wordCharArIndex]){
					remainCharCount -= diff;
					wordCharArIndex += diff;
					count++;
				}else{
					return -1;
				}
			}
			return -1;
		}
		
	}//end of class
	
	private static class WordComparator implements Comparator<String>{
		//long words come first
		public int compare(String word1, String word2){
			return word1.length() < word2.length() ? 1 : -1;
		}
	}
	
	/**
	 * Takes list of words, create crossword puzzle from it.
	 * @param wordsList
	 */
	private static void processSet(List<String> wordsList){
		Collections.sort(wordsList, new WordComparator());
		String firstWord = wordsList.get(0);
		wordsList.remove(0);
		Board board = new Board(firstWord, wordsList);
		
		
		
		
		Iterator<String> wordsListIter = wordsList.iterator();
		while(wordsListIter.hasNext()){
			String nextWord = wordsListIter.next();
			
		}
	}
	
	public static void main(String[] args){
	
		
		
		
	}
	
}
