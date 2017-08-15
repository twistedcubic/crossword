package crossword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
		
		public String toString(){
			return "[(" +row + ", "+ col + ") " + letter + "] ";
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
		
		WordOrientation getOpposite(){
			if(this == HORIZONTAL){
				return VERTICAL;
			}else{
				return HORIZONTAL;
			}
		}
	}
			
	/**
	 * Object representing the puzzle board.
	 */
	private static class Board{
		
		BoardNode[][] board = new BoardNode[BOARD_LEN][BOARD_LEN];
		private static final char PLACEHOLDER_CHAR = ' ';
		
		/*
		 * Set of rows and columns containing nontrivial letter,
		 * to make lookup and retrieval more efficient.
		 */
		private Set<Integer> rowSet = new TreeSet<Integer>();
		private Set<Integer> colSet = new TreeSet<Integer>();
		//set of board positions. 
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
			
			rootBoardPosition = new BoardPosition(null, remainingWordsList);
			insertWord(firstWord, boardMiddle, startingCol, WordOrientation.HORIZONTAL, rootBoardPosition);
			leafBoardPosSet.add(rootBoardPosition);
			
		}
		
		private static class BoardPositionComparator implements Comparator<BoardPosition>{
			public int compare(BoardPosition boardPos1, BoardPosition boardPos2){
				int count1 = boardPos1.totalWordIntersectionCount;
				int count2 = boardPos2.totalWordIntersectionCount;
				return count1 > count2 ? 1 : (count1 < count2 ? -1 : 0);
			}
		}
		
		/**
		 * Builds the board.
		 * @return the board positions with a solution. Ranked by total number 
		 * of intersections amongst words in a BoardPosition.
		 */
		List<BoardPosition> build(){
			
			List<BoardPosition> satBoardPosList = new ArrayList<BoardPosition>();
			
			while(satBoardPosList.size() < 1 && !this.leafBoardPosSet.isEmpty()){
				
				System.out.println("this.leafBoardPosSet "+ this.leafBoardPosSet.size() 
					+ "  " +this.leafBoardPosSet);
				
				Set<BoardPosition> newLeafBoardPosSet = new HashSet<BoardPosition>();
				
				for(BoardPosition leafBoardPos : this.leafBoardPosSet){
					//System.out.println("leafBoardPos.remainingWordsList "+leafBoardPos.remainingWordsList);
					if(leafBoardPos.remainingWordsList.isEmpty()){
						satBoardPosList.add(leafBoardPos);
						continue;
					}
					
					List<List<WordNode>> rowWordNodeList = new ArrayList<List<WordNode>>();
					List<List<WordNode>> colWordNodeList = new ArrayList<List<WordNode>>();
					
					gatherWordNodes(leafBoardPos, rowWordNodeList, colWordNodeList);
					System.out.println("rowWordNodeList "+rowWordNodeList);
					System.out.println("colWordNodeList "+colWordNodeList);
					
					List<BoardPosition> childrenBoardPosList = leafBoardPos
							.findLegalWordInsertion(rowWordNodeList, colWordNodeList, this);
					System.out.println("childrenBoardPosList "+childrenBoardPosList);
					//get list of leaf BoardPositions
					newLeafBoardPosSet.addAll(childrenBoardPosList);
				}
				this.leafBoardPosSet = newLeafBoardPosSet;
			}
			//Set<BoardPosition> tSet = new TreeSet<BoardPosition>(new BoardPositionComparator());
			Collections.sort(satBoardPosList, new BoardPositionComparator());
			return satBoardPosList;	
		}
		
		/**
		 * Prints out visualization of the given BoardPosition.
		 * @param boardPos
		 */
		public void visualizeBoardPosition(BoardPosition boardPos){
			List<char[]> boardRowList = new ArrayList<char[]>();
			
			List<Integer> colSetList = new ArrayList<Integer>(colSet);
			int smallestCol = colSetList.get(0);
			int largestCol = colSetList.get(colSetList.size()-1);
			int colSetDiff = largestCol - smallestCol + 1;
			//System.out.println("colSetDiff "+colSetDiff);
			for(int j : rowSet){
				//List<WordNode> nodeList = new ArrayList<WordNode>();
				char[] rowAr = new char[colSetDiff];
				for(int i : colSet){
					BoardNode boardNode = this.board[j][i];
					if(null == boardNode){
						rowAr[i-smallestCol] = PLACEHOLDER_CHAR;
						continue;
					}
					BoardPosition posContained = boardNode.containsBoardPosition(boardPos);
					if(null != posContained){
						rowAr[i-smallestCol] = boardNode.boardPosCharMap.get(posContained);
					}else{
						rowAr[i-smallestCol] = PLACEHOLDER_CHAR;
					}
				}
				boardRowList.add(rowAr);
			}
			for(char[] rowAr : boardRowList){
				for(char c : rowAr){
					System.out.print(c + " ");
				}
				System.out.println();
				//System.out.println(Arrays.toString(rowAr));
			}
		}
		
		/**
		 * Prints out visualization of the given BoardPosition.
		 * @param boardPos
		 */
		public void visualizeBoardPositionPuzzle(BoardPosition boardPos){
			List<char[]> boardRowList = new ArrayList<char[]>();
			char blackSquareChar = '\u25a0';
			//char whiteSquareChar = '\u25a1';
			
			List<Integer> colSetList = new ArrayList<Integer>(colSet);
			int smallestCol = colSetList.get(0);
			int largestCol = colSetList.get(colSetList.size()-1);
			int colSetDiff = largestCol - smallestCol + 1;
			int wordCounter = -1;
			Map<Integer, String> horIntWordMap = new HashMap<Integer, String>();
			Map<Integer, String> verIntWordMap = new HashMap<Integer, String>();
			
			//List<String> horWordsList = new ArrayList<String>();
			//List<String> verWordsList = new ArrayList<String>();
			
			//System.out.println("colSetDiff "+colSetDiff);
			for(int j : rowSet){
				//List<WordNode> nodeList = new ArrayList<WordNode>();
				char[] rowAr = new char[colSetDiff];
				for(int i : colSet){
					BoardNode boardNode = this.board[j][i];
					if(null == boardNode){
						rowAr[i-smallestCol] = blackSquareChar;
						continue;
					}
					BoardPosition posContained = boardNode.containsBoardPosition(boardPos);
					if(null != posContained){
						String horWordStart = boardNode
								.containsWordStart(boardPos, WordOrientation.HORIZONTAL);
						String verWordStart;
						boolean wordAdded = false;
						if(null != horWordStart){
							wordCounter++;
							horIntWordMap.put(wordCounter, horWordStart);
							rowAr[i-smallestCol] = Integer.toString(wordCounter).charAt(0);
							wordAdded = true;
						}
						if(null != (verWordStart=boardNode
								.containsWordStart(boardPos, WordOrientation.VERTICAL))){
							if(!wordAdded){
								wordCounter++;
							}
							verIntWordMap.put(wordCounter, verWordStart);
							//take care of double digits!!
							rowAr[i-smallestCol] = Integer.toString(wordCounter).charAt(0);
							wordAdded = true;
						}
						if(!wordAdded){
							rowAr[i-smallestCol] = PLACEHOLDER_CHAR;/////HERE
						}						
						///rowAr[i-smallestCol] = boardNode.boardPosCharMap.get(posContained);
					}else{
						rowAr[i-smallestCol] = blackSquareChar;
					}
				}
				boardRowList.add(rowAr);
			}
			for(char[] rowAr : boardRowList){
				for(char c : rowAr){
					System.out.print(c + " ");
				}
				System.out.println();
				//System.out.println(Arrays.toString(rowAr));
			}
			System.out.println("horIntWordMap "+horIntWordMap);
			System.out.println("verIntWordMap "+verIntWordMap);
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
						node = new BoardNode(curChar, boardPos);
						board[rowStart][colStart+i] = node;
					}
					node.addBoardPosition(curChar, boardPos, orient);
					colSet.add(colStart + i);
				}	
				rowSet.add(rowStart);
			}else{
				for(int i = 0; i < word.length(); i++){
					char curChar = word.charAt(i);
					BoardNode node = board[rowStart+i][colStart];
					if(null == node){
						node = new BoardNode(curChar, boardPos);
						board[rowStart+i][colStart] = node;
					}
					node.addBoardPosition(curChar, boardPos, orient);
					rowSet.add(rowStart+i);
				}
				colSet.add(colStart);
			}
			board[rowStart][colStart].addBoardPosWordStart(boardPos, word, orient);
		}
		   
		/**
		 * Fill up given lists with WordNode's, for given board position
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
					BoardPosition posContained = boardNode.containsBoardPosition(boardPosition);
					if(null != posContained){
						char boardNodeLetter = boardNode.boardPosCharMap.get(posContained);
						nodeList.add(new WordNode(boardNodeLetter, rowNum, i));					
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
					BoardPosition posContained = boardNode.containsBoardPosition(boardPosition);
					if(null != posContained){
						char boardNodeLetter = boardNode.boardPosCharMap.get(posContained);
						nodeList.add(new WordNode(boardNodeLetter, i, colNum));						
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
		//board positions and their corresponding characters, 
		//since different pos in board can overlap with diff letters.
		Map<BoardPosition, Character> boardPosCharMap 
			= new HashMap<BoardPosition, Character>();
		Set<BoardPosition> boardPositionHorSet;
		Set<BoardPosition> boardPositionVerSet;
		//membership indicates start of word at this node for the BoardPosition key.
		//Used for creating final puzzle visualization.
		Map<BoardPosition, String> horBoardPosWordMap = new HashMap<BoardPosition, String>();
		Map<BoardPosition, String> verBoardPosWordMap = new HashMap<BoardPosition, String>();
		
		/**
		 * @param letter_
		 * @param boardPos First BoardPosition, associated with letter.
		 */
		BoardNode(char letter_, BoardPosition boardPos){
			//this.letter = letter_;
			this.boardPosCharMap.put(boardPos, letter_);
			//this.boardPositionSet = new HashSet<BoardPosition>();
			this.boardPositionHorSet = new HashSet<BoardPosition>();
			this.boardPositionVerSet = new HashSet<BoardPosition>();
		}
		
		void addBoardPosWordStart(BoardPosition boardPos, String word, WordOrientation orient){
			if(WordOrientation.HORIZONTAL == orient){
				horBoardPosWordMap.put(boardPos, word);
			}else{
				verBoardPosWordMap.put(boardPos, word);
			}
		}
		
		/**
		 * Word if the word begins at this Node, could be a parent. 
		 * Null if no word starts in this Node.
		 * @param boardPosition
		 * @param orientAr
		 * @return
		 */
		String containsWordStart(BoardPosition boardPosition, WordOrientation orient){
		
			Map<BoardPosition, String> orientedBoardPosMap;
			if(WordOrientation.HORIZONTAL == orient){
				orientedBoardPosMap = this.horBoardPosWordMap;
			}else{
				orientedBoardPosMap = this.verBoardPosWordMap;
			}
			
			//check parents
			BoardPosition parentPos = boardPosition;
			while(null != parentPos){
				String word = orientedBoardPosMap.get(parentPos);
				if(null != word){
					return word;
				}
				boardPosition = parentPos;
				parentPos = boardPosition.parentPosition;
			}
			return null;
		}
		
		/**
		 * Adds board position to this node, along with corresponding
		 * letter.
		 * @param letter_
		 * @param boardPosition
		 */
		void addBoardPosition(char letter_, BoardPosition boardPosition, 
				WordOrientation orient){
			this.boardPosCharMap.put(boardPosition, letter_);
			if(WordOrientation.HORIZONTAL == orient){
				this.boardPositionHorSet.add(boardPosition);				
			}else{
				this.boardPositionVerSet.add(boardPosition);
			}
		}
		
		/**
		 * @param boardPosition
		 * @return BoardPosition that's contained in this Node, could be a parent.
		 * null if none of ancestors is contained in this Node.
		 */
		BoardPosition containsBoardPosition(BoardPosition boardPosition, WordOrientation... orientAr ){
			int orientArLen = orientAr.length;
			BoardPosition boardPosFound;
			
			if(2 == orientArLen || 0 == orientArLen){
				if(null != (boardPosFound=
						containsBoardPosition(boardPosition, WordOrientation.HORIZONTAL))){
					return boardPosFound;
				}else if(null != (boardPosFound=
						containsBoardPosition(boardPosition, WordOrientation.VERTICAL))){
					return boardPosFound;
				}
			}else{
				return containsBoardPosition(boardPosition, orientAr[0]);
			}
			return null;
		}
		
		/**
		 * @param boardPosition
		 * @return BoardPosition that's contained in this Node, could be a parent.
		 * null if none of ancestors is contained in this Node.
		 */
		BoardPosition containsBoardPosition(BoardPosition boardPosition, WordOrientation orient){
			
			Set<BoardPosition> orientedBoardPosSet;
			if(WordOrientation.HORIZONTAL == orient){
				orientedBoardPosSet = this.boardPositionHorSet;
			}else{
				orientedBoardPosSet = this.boardPositionVerSet;
			}
			if(orientedBoardPosSet.contains(boardPosition)){
				return boardPosition;
			}
			
			//check parents
			BoardPosition parentPos = boardPosition.parentPosition;
			while(null != parentPos){
				if(orientedBoardPosSet.contains(parentPos)){
					return parentPos;
				}
				boardPosition = parentPos;
				parentPos = boardPosition.parentPosition;
			}
			return null;
		}
	}//end of BoardNode class.
	
	/**
	 * Board position recording the current position in the board tree.
	 * equals and hashcode uses default reference equality.
	 */
	private static class BoardPosition{
		BoardPosition parentPosition;
		//set of words remaining for this position.
		List<String> remainingWordsList;
		//used for ranking different BoardPosition's.
		int totalWordIntersectionCount = 0;
		
		BoardPosition(BoardPosition parentPosition_, List<String> remainingWordsList_){
			this.parentPosition = parentPosition_;
			this.remainingWordsList = remainingWordsList_;
			if(null != parentPosition_){
				this.totalWordIntersectionCount = parentPosition_.totalWordIntersectionCount;				
			}
		}
		
		private static class WordWithWordNodes{
			String word;
			List<WordNode> wordNodesList;
			//starting index of WordNode in word, i.e. char index in word
			//that should be inserted at WordNode.
			int wordStartingIndex;
			
			WordWithWordNodes(String word_, List<WordNode> wordNodesList_, 
					int wordStartingIndex_){
				this.word = word_;
				this.wordNodesList = wordNodesList_;
				this.wordStartingIndex = wordStartingIndex_;
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
			
			int horizontalMax = getMultiIntersectWords(board, rowWordNodeList, tMap, WordOrientation.HORIZONTAL);
			
			//check columns			
			TreeMap<Integer, List<WordWithWordNodes>> colTMap = new TreeMap<Integer, List<WordWithWordNodes>>();
			int verticalMax = getMultiIntersectWords(board, colWordNodeList, colTMap, WordOrientation.VERTICAL);
			System.out.println("verticalMax "+verticalMax);
			System.out.println("colTMap "+colTMap);
			
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
				List<WordWithWordNodes> wordWithWordNodesHorList = new ArrayList<WordWithWordNodes>();
				List<WordWithWordNodes> wordWithWordNodesVerList = new ArrayList<WordWithWordNodes>();
				getSingleIntersectionWords(board, rowWordNodeList,
						wordWithWordNodesHorList, WordOrientation.HORIZONTAL);
				getSingleIntersectionWords(board, colWordNodeList,
						wordWithWordNodesVerList, WordOrientation.VERTICAL);
				
				System.out.println("remainingWordsList "+remainingWordsList);
				/*if(!wordWithWordNodesHorList.isEmpty() || !wordWithWordNodesVerList.isEmpty()){
					this.remainingWordsList.remove(0);
				}*/
				//add one horizontally, one vertically
				int intersectionCount = 1;
				addWordNodesFromList(board, childrenBoardPositionList, WordOrientation.HORIZONTAL, 
						wordWithWordNodesHorList, intersectionCount);
				addWordNodesFromList(board, childrenBoardPositionList, WordOrientation.VERTICAL, 
						wordWithWordNodesVerList, intersectionCount);				
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

		/**
		 * Add word to node. Word insertion legality must be determined before this.
		 * @param board
		 * @param childrenBoardPositionList
		 * @param orient
		 * @param maxIntersectionCountList
		 * @param intersectionCount
		 */
		private void addWordNodesFromList(Board board, List<BoardPosition> childrenBoardPositionList,
				WordOrientation orient, List<WordWithWordNodes> maxIntersectionCountList,
				int intersectionCount) {
			if(!maxIntersectionCountList.isEmpty()){
				this.totalWordIntersectionCount += intersectionCount;
			}
			//create BoardPosition's for top-ranking
			for(WordWithWordNodes wordWithWordNodes : maxIntersectionCountList){
				//only need first one
				WordNode firstWordNode = wordWithWordNodes.wordNodesList.get(0);
				String word = wordWithWordNodes.word;
				int startingIndex = wordWithWordNodes.wordStartingIndex;
				int startingRow = firstWordNode.row;
				int startingCol = firstWordNode.col;
				if(WordOrientation.HORIZONTAL == orient){
					startingCol -= startingIndex;
				}else{
					startingRow -= startingIndex;
				}
				//this.remainingWordsList.remove(word);
				List<String> childRemainingWordsList = new ArrayList<String>(this.remainingWordsList);
				childRemainingWordsList.remove(word);
				BoardPosition boardPos = new BoardPosition(this, childRemainingWordsList);
				board.insertWord(word, startingRow, startingCol, orient, boardPos);					
				childrenBoardPositionList.add(boardPos);
			}
		}

		private void getSingleIntersectionWords(Board board, List<List<WordNode>> rowWordNodeList,
				List<WordWithWordNodes> wordWithWordNodesList,
				WordOrientation orient) {
			
			String nextLongestWord = this.remainingWordsList.get(0);					
			char[] wordCharAr = nextLongestWord.toCharArray();
			int wordCharArLen = wordCharAr.length;
			
			for(List<WordNode> wordNodeList: rowWordNodeList){
				//a row
				//space to prior word
				int prevSpace = 100;				
				for(int i = 0; i < wordNodeList.size(); i++){
					//check for two or more intersections first
					WordNode wordNode = wordNodeList.get(i);
					//WordNode nextWordNode = wordNodeList.get(i+1);
					int postSpace;	
					int boardNodeRowNum = wordNode.row;
					int boardNodeColNum = wordNode.col;
					if(WordOrientation.HORIZONTAL == orient){
						//int colNum = wordNode.col;
						//colOrRowAbove = colNum - 1;
						//int nextColNum = nextWordNode.col;
						//int colDiff = nextColNum - colNum;
						postSpace = i == wordNodeList.size()-1 
								? 100 : wordNodeList.get(i+1).col - boardNodeColNum;						
					}else{
						//int rowNum = wordNode.row;
						//colOrRowAbove = rowNum - 1;
						postSpace = i == wordNodeList.size()-1 
								? 100 : wordNodeList.get(i+1).row - boardNodeRowNum;
					}
					char wordNodeChar = wordNode.letter;
					
					for(int j = 0; j < wordCharArLen; j++){
						char curChar = wordCharAr[j];
						//System.out.println("curChar, wordNodeChar "+curChar + ", "+ wordNodeChar);

						int firstLetterRow;
						int firstLetterCol;
						int lastLetterRow;
						int lastLetterCol;
						//to ensure the top or left of starting word isn't
						//the end of another word with same BoardPosition.
						int colOrRowAboveRow;
						int colOrRowAboveCol;
						if(WordOrientation.HORIZONTAL == orient){
							firstLetterRow = boardNodeRowNum;
							firstLetterCol = boardNodeColNum - j;
							colOrRowAboveRow = firstLetterRow - 1;
							colOrRowAboveCol = firstLetterCol;
							lastLetterRow = boardNodeRowNum;
							lastLetterCol = boardNodeColNum + wordCharArLen - (j+1);
						}else{
							firstLetterRow = boardNodeRowNum - j;
							firstLetterCol = boardNodeColNum;
							colOrRowAboveRow = firstLetterRow;
							colOrRowAboveCol = firstLetterCol - 1;
							lastLetterRow = boardNodeRowNum + wordCharArLen - (j+1);
							lastLetterCol = boardNodeColNum;
						}
						/*if(WordOrientation.HORIZONTAL == orient){
							colOrRowAboveRow = boardNodeRowNum - 1;
							colOrRowAboveCol = boardNodeColNum - j;
						}else{
							colOrRowAboveRow = boardNodeRowNum - j;
							colOrRowAboveCol = boardNodeColNum - 1;
						}*/
												
						BoardNode colOrRowAboveNode = board.board[colOrRowAboveRow][colOrRowAboveCol];
						//avoid e.g. playarn for "play" and "yarn"
						BoardNode colOrRowBeforeNode = board
								.board[WordOrientation.HORIZONTAL == orient ? firstLetterRow : firstLetterRow-1]
										[WordOrientation.HORIZONTAL == orient ? firstLetterCol-1 : firstLetterCol];
						
						if(prevSpace > j 
									&& curChar == wordNodeChar
									//+1 because don't want last letter to be 
									//immediately followed by another word.
									&& j + postSpace > wordCharAr.length + 1
									//check the top of the would-be starting point
									///*
									&& (null == colOrRowAboveNode
										|| null == colOrRowAboveNode
										.containsBoardPosition(this, orient.getOpposite()))
										//****/
									&& (null == colOrRowBeforeNode
										|| null == colOrRowBeforeNode
										.containsBoardPosition(this, orient))
									&& checkWordBody(board, firstLetterRow, firstLetterCol, 
											lastLetterRow, lastLetterCol, orient)
									//around the last letter
									&& checkAroundLastLetter(board, orient, lastLetterRow, lastLetterCol)
									){								
								//add previous intersection
								List<WordNode> curWordNodeList = new ArrayList<WordNode>();
								curWordNodeList.add(wordNode);
								
								wordWithWordNodesList
									.add(new WordWithWordNodes(nextLongestWord, curWordNodeList, j));
								
								this.totalWordIntersectionCount++;
								System.out.println("this.totalWordIntersectionCount "+this.totalWordIntersectionCount);
								//only add the first suitable place for one-intersection words.
								return;
							}							
						}						
					//}
					prevSpace = postSpace;					
				}
			}
			if(wordWithWordNodesList.isEmpty()){
				//need better way, this could lead to infinite loop if both 
				//words don't fit!
				if(this.remainingWordsList.size() > 1){
					this.remainingWordsList.remove(0);
					this.remainingWordsList.add(1, nextLongestWord);					
				}else if(this.remainingWordsList.size() == 1){
					//have size one in this case.
					//last word doesn't fit, put on board at some place
					/*if(WordOrientation.VERTICAL == orient){
						String lastWord = this.remainingWordsList.get(0);
						int startingPos = BOARD_LEN/2 - BOARD_LEN/3;
						board.insertWord(lastWord, startingPos, 
								startingPos, orient, this);
						this.remainingWordsList.clear();
						//don't delete this comment yet!
						if(true) throw new IllegalStateException();
					}*/
					String lastWord = this.remainingWordsList.get(0);
					System.out.println("Crossword - last word didn't fit "+lastWord);
					//this.remainingWordsList.clear();
				}				
			}
			//return horizontalMax;
		}

		private boolean checkWordBody(Board board, int firstLetterRow, int firstLetterCol, 
				int lastLetterRow, int lastLetterCol,
				WordOrientation orient) {
			boolean freeAroundLetter = true;
			if(WordOrientation.HORIZONTAL == orient){
				for(int curCol = firstLetterCol; freeAroundLetter && curCol <= lastLetterCol; curCol++){
					freeAroundLetter = freeAroundLetter && 
							checkAroundBodyLetter(board, orient, firstLetterRow, curCol);
				}
			}else{
				for(int curRow = firstLetterRow; freeAroundLetter && curRow <= lastLetterRow; curRow++){
					freeAroundLetter = freeAroundLetter && 
							checkAroundBodyLetter(board, orient, curRow, firstLetterCol);
				}
			}
			return freeAroundLetter;
		}

		/**
		 * Check around a letter that's in middle of the word, excluding
		 * first and last letters.
		 * @param board
		 * @param orient
		 * @param letterRow
		 * @param letterCol
		 * @return True if no word conflicts around last letter. 
		 */
		private boolean checkAroundBodyLetter(Board board, WordOrientation orient, int letterRow, int letterCol) {
			boolean freeAroundLetter = true;
			BoardNode lastLetterNode = board.board[letterRow][letterCol];
			if(null != lastLetterNode && null != lastLetterNode.containsBoardPosition(this)){
				return true;
			}
			if(WordOrientation.HORIZONTAL == orient){
				//BoardNode lastLetterRightNode = board.board[letterRow][letterCol+1];
				//ensure the letter above is not end of a vertical word
				BoardNode lastLetterUpNode = board.board[letterRow-1][letterCol];
				freeAroundLetter = null == lastLetterUpNode ||
						null == lastLetterUpNode.containsBoardPosition(this, WordOrientation.VERTICAL);
											
			}else{
				//ensure the letter above is not end of a vertical word
				BoardNode lastLetterLeftNode = board.board[letterRow][letterCol-1];
				//only check if current node last letter is free, so the previous Node is conclusion of a word.
				freeAroundLetter = null == lastLetterLeftNode ||
						null == lastLetterLeftNode.containsBoardPosition(this, WordOrientation.HORIZONTAL);
				
			}
			return freeAroundLetter;
		}
		
		/**
		 * Checks last letter placement legitimacy.
		 * @param board
		 * @param orient
		 * @param lastLetterRow
		 * @param lastLetterCol
		 * @return True if no word conflicts around last letter. 
		 */
		private boolean checkAroundLastLetter(Board board, WordOrientation orient, int lastLetterRow, int lastLetterCol) {
			boolean freeAroundLastLetter = true;
			BoardNode lastLetterNode = board.board[lastLetterRow][lastLetterCol];
			if(null != lastLetterNode && null != lastLetterNode.containsBoardPosition(this)){
				return true;
			}
			if(WordOrientation.HORIZONTAL == orient){
				BoardNode lastLetterRightNode = board.board[lastLetterRow][lastLetterCol+1];
				freeAroundLastLetter = null == lastLetterRightNode ||
						null == lastLetterRightNode.containsBoardPosition(this);
				if(freeAroundLastLetter){
					BoardNode lastLetterUpNode = board.board[lastLetterRow-1][lastLetterCol];
					freeAroundLastLetter = null == lastLetterUpNode ||
							null == lastLetterUpNode.containsBoardPosition(this, WordOrientation.VERTICAL);
				}							
			}else{
				BoardNode lastLetterLeftNode = board.board[lastLetterRow][lastLetterCol-1];
				//only check if current node last letter is free, so the previous Node is conclusion of a word.
				freeAroundLastLetter = null == lastLetterLeftNode ||
						null == lastLetterLeftNode.containsBoardPosition(this, WordOrientation.HORIZONTAL);
					
				if(freeAroundLastLetter){
					BoardNode lastLetterDownNode = board.board[lastLetterRow+1][lastLetterCol];
					freeAroundLastLetter = null == lastLetterDownNode ||
							null == lastLetterDownNode.containsBoardPosition(this);
				}
			}
			return freeAroundLastLetter;
		}
		
		/**
		 * @param rowWordNodeList
		 * @param tMap
		 */
		private int getMultiIntersectWords(Board board, List<List<WordNode>> rowWordNodeList,
				TreeMap<Integer, List<WordWithWordNodes>> tMap, WordOrientation orient) {
			
			int horizontalMax = 0;
			for(List<WordNode> wordNodeList: rowWordNodeList){
				//a row
				//space to prior word
				int prevSpace = 100;				
				for(int i = 0; i < wordNodeList.size()-1; i++){
					//check for two or more intersections first
					WordNode wordNode = wordNodeList.get(i);
					WordNode nextWordNode = wordNodeList.get(i+1);

					int colNum;
					int nextColNum;			
					int boardNodeRowNum;
					int boardNodeColNum;
					if(WordOrientation.HORIZONTAL == orient){
						boardNodeRowNum = wordNode.row;
						boardNodeColNum = colNum = wordNode.col;
						nextColNum = nextWordNode.col;
					}else{
						boardNodeRowNum = colNum = wordNode.row;
						boardNodeColNum = wordNode.col;
						nextColNum = nextWordNode.row;						
					}
					int colDiff = nextColNum - colNum;
					
					char wordNodeChar = wordNode.letter;
					char nextWordNodeChar = nextWordNode.letter;
					//int postSpace = 0     ;
					//look over all remaining words
					for(String word : remainingWordsList){
						char[] wordCharAr = word.toCharArray();
						int wordCharArLen = wordCharAr.length;
						
						for(int j = 0; j < wordCharArLen; j++){
							char curChar = wordCharAr[j];

							int firstLetterRow;
							int firstLetterCol;
							int lastLetterRow;
							int lastLetterCol;
							//to ensure the top or left of starting word isn't
							//the end of another word with same BoardPosition.							
							int colOrRowAboveRow;
							int colOrRowAboveCol;
							if(WordOrientation.HORIZONTAL == orient){
								firstLetterRow = boardNodeRowNum;
								firstLetterCol = boardNodeColNum - j;
								colOrRowAboveRow = firstLetterRow - 1;
								colOrRowAboveCol = firstLetterCol;
								lastLetterRow = boardNodeRowNum;
								lastLetterCol = boardNodeColNum + wordCharArLen - (j+1);
							}else{
								firstLetterRow = boardNodeRowNum - j;
								firstLetterCol = boardNodeColNum;
								colOrRowAboveRow = firstLetterRow;
								colOrRowAboveCol = firstLetterCol - 1;
								lastLetterRow = boardNodeRowNum + wordCharArLen - (j+1);
								lastLetterCol = boardNodeColNum;
							}
							BoardNode colOrRowAboveNode = board.board[colOrRowAboveRow][colOrRowAboveCol];
							//avoid e.g. playarn for "play" and "yarn"
							BoardNode colOrRowBeforeNode = board
									.board[WordOrientation.HORIZONTAL == orient ? firstLetterRow : firstLetterRow-1]
											[WordOrientation.HORIZONTAL == orient ? firstLetterCol-1 : firstLetterCol];
							
							int intersectionCount;
							if(prevSpace > j
									&& curChar == wordNodeChar
									&& j + colDiff < wordCharAr.length 
									&& wordCharAr[j+colDiff] == nextWordNodeChar
									///*
									&& (null == colOrRowAboveNode || 
											null == colOrRowAboveNode.containsBoardPosition(this, orient.getOpposite()))
									//*/
									&& (null == colOrRowBeforeNode ||
											null == colOrRowBeforeNode.containsBoardPosition(this, orient))
									&& checkWordBody(board, firstLetterRow, firstLetterCol, 
											lastLetterRow, lastLetterCol, orient)
									&& checkAroundLastLetter(board, orient, lastLetterRow, lastLetterCol)
									//check the word fits wrt remaining words
									&& (intersectionCount = remainingWordFits(wordCharAr, j+colDiff, 
											wordNodeList, i+1, orient))>0){
								
								//add previous two intersection
								List<WordNode> curWordNodeList = new ArrayList<WordNode>();
								curWordNodeList.add(wordNode);
								curWordNodeList.add(nextWordNode);
								
								for(int k = 0; k < intersectionCount; k++){
									curWordNodeList.add(wordNodeList.get(i+k+2));
								}
								int key = intersectionCount + 2;
								this.totalWordIntersectionCount += key;
								
								if(key > horizontalMax){
									horizontalMax = key;
								}
								List<WordWithWordNodes> list = tMap.get(key);
								if(null == list){
									list = new ArrayList<WordWithWordNodes>();									
								}
								list.add(new WordWithWordNodes(word, curWordNodeList, j));
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
		 * @return 0 or more means that number of intersections, -1 means don't fit, 
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
				//+1 because don't want last letter to be 
				//immediately followed by another word.
				if(diff > remainCharCount + 1){
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
			return word1.length() < word2.length() ? 1 : (word1.length() > word2.length() ? -1 : 0);
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
		
		List<BoardPosition> satBoardPosList = board.build();
		
		for(BoardPosition boardPos : satBoardPosList){
			board.visualizeBoardPosition(boardPos);
			board.visualizeBoardPositionPuzzle(boardPos);
			System.out.println("intersection count: " + boardPos.totalWordIntersectionCount);
		}		
	}
	
	public static void main(String[] args){
		
		String[] wordsAr = {"apple","pear", "play", "alps", "yarn","woman", "orange", "crocodile"};
		//wordsAr = new String[]{"french","apple","beach","perry","sadie","capemay"};
		wordsAr = new String[]{"french", "eth", "groupon", "epic"};
		wordsAr = new String[]{"none", "groupon", "chess", "epic","perry","beach","capemay",
				"physician","funny","math"};
		wordsAr = new String[]{"shirt", "scarf", "gloves", "love","sudoku","cheese","tunic",
				"happy","smile","cocktail"};
		List<String> wordsList = new ArrayList<String>();
		for(String word : wordsAr){
			wordsList.add(word);
		}
		processSet(wordsList);
		
	}
	
}
