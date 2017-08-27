package crossword;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generate a crossword puzzle given a set of words.
 * @author yihed
 *
 */
public class Crossword {
	
	//length of the crossword, one dimension
	private static final int BOARD_LEN = 200;
	private static final char[] GREEK_ALPHA;
	private static final int GREEK_ALPHA_LEN;
	private static final String DEFAULT_ENCODING = "UTF-16";
	private static final Logger logger = LogManager.getLogger(Crossword.class);
	
	static{
		GREEK_ALPHA = new char[]{'0','1','2',
				'3','4','5','6','7',
				'8','9',
				'\u03b1', '\u03b2','\u03b3',
				'\u03b4','\u03b5','\u03b6','\u03b7','\u03b8',
				'\u03b9','\u03ba','\u03bb','\u03bc','\u03bd',
				'\u03be','\u03bf','\u03c0','\u03c1','\u03c2',
				'\u03c3','\u03c4','\u03c5','\u03c7','\u03c8',
				'\u03c9'};
		System.out.println(Arrays.toString(GREEK_ALPHA));
		GREEK_ALPHA_LEN = GREEK_ALPHA.length;
	}
	
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
		/**
		 * Better boards have higher comparator value. Based on whether
		 * how many words could be incorporated into a position, and the
		 * number of word intersections, the more intersections, the higher score.
		 * 
		 */
		private static class BoardPositionComparator implements Comparator<BoardPosition>{
			
			/**
			 * map of boardPosition and total dist to any intersection.
			 */
			Map<BoardPosition, Integer> totalIntersectDistMap 
				= new HashMap<BoardPosition, Integer>();
			
			/**
			 * Constructor that creates totalIntersectDistMap.
			 * @param board
			 */
			BoardPositionComparator(Board board){
				Set<Integer> rowSet = board.rowSet;
				Set<Integer> colSet = board.colSet;
				
				for(int row : rowSet){
					for(int col : colSet){
						BoardNode curNode = board.board[row][col];
						if(null == curNode){
							continue;
						}
						Map<BoardPosition, Integer> nodeBoardPosMap = curNode.distToIntersectMap;

						for(Map.Entry<BoardPosition, Integer> entry : nodeBoardPosMap.entrySet()){
							Integer curDist = totalIntersectDistMap.get(entry.getKey());
							int distToAdd = null == curDist ? entry.getValue() : curDist + entry.getValue();
							totalIntersectDistMap.put(entry.getKey(), distToAdd);								
						}						
					}					
				}				
			}
			
			/**
			 * Larger is more optimal puzzle.
			 */
			public int compare(BoardPosition boardPos1, BoardPosition boardPos2){
				int count1 = boardPos1.totalWordIntersectionCount;
				int count2 = boardPos2.totalWordIntersectionCount;
				int wordsLeft1 = boardPos1.remainingWordsList.size();
				int wordsLeft2 = boardPos2.remainingWordsList.size();
				int totalIntersectDist1 = totalIntersectDistMap.get(boardPos1);
				int totalIntersectDist2 = totalIntersectDistMap.get(boardPos2);
				
				return wordsLeft1 < wordsLeft2 ? 1 : (wordsLeft1 > wordsLeft2 
						? -1 : count1 > count2 ? 1 : (totalIntersectDist1 < totalIntersectDist2
								? 1 : (totalIntersectDist1 > totalIntersectDist2 
										? -1 : count1 < count2 ? -1 : 0)
								)								
						);
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
			Collections.sort(satBoardPosList, new BoardPositionComparator(this));
			return satBoardPosList;	
		}
		
		/**
		 * Prints out visualization of the given BoardPosition.
		 * @param boardPos
		 */
		public String visualizeBoardPosition(BoardPosition boardPos){
			List<char[]> boardRowList = new ArrayList<char[]>();
			
			List<Integer> colSetList = new ArrayList<Integer>(colSet);
			int smallestCol = colSetList.get(0);
			int largestCol = colSetList.get(colSetList.size()-1);
			int colSetDiff = largestCol - smallestCol + 1;
			//System.out.println("colSetDiff "+colSetDiff);
			for(int j : rowSet){
				//List<WordNode> nodeList = new ArrayList<WordNode>();
				char[] rowAr = new char[colSetDiff];
				int[] colRangeAr = boardPos.getColRangeToPrint(this);
				//System.out.println("colRangeAr "+Arrays.toString(colRangeAr));
				for(int i = colRangeAr[0]; i < colRangeAr[1]; i++){
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
			StringBuilder sb = new StringBuilder(500);
			for(char[] rowAr : boardRowList){
				for(char c : rowAr){
					String s = c + " ";
					System.out.print(s);
					sb.append(s);
				}
				System.out.println();
				sb.append("\n");
				//System.out.println(Arrays.toString(rowAr));
			}
			return sb.toString();
		}
		
		/**
		 * Prints out visualization of the given BoardPosition.
		 * @param boardPos
		 */
		public String visualizeBoardPositionPuzzle(BoardPosition boardPos){
			List<char[]> boardRowList = new ArrayList<char[]>();
			char blackSquareChar = '\u25a0';
			//char whiteSquareChar = '\u25a1';
			
			List<Integer> colSetList = new ArrayList<Integer>(colSet);
			int smallestCol = colSetList.get(0);
			int largestCol = colSetList.get(colSetList.size()-1);
			int colSetDiff = largestCol - smallestCol + 1;
			int wordCounter = -1;
			Map<Character, String> horIntWordMap = new HashMap<Character, String>();
			Map<Character, String> verIntWordMap = new HashMap<Character, String>();
			
			//List<String> horWordsList = new ArrayList<String>();
			//List<String> verWordsList = new ArrayList<String>();
			
			//System.out.println("colSetDiff "+colSetDiff);
			for(int j : rowSet){
				//List<WordNode> nodeList = new ArrayList<WordNode>();
				char[] rowAr = new char[colSetDiff];
				int[] colRangeAr = boardPos.getColRangeToPrint(this);
				
				for(int i = colRangeAr[0]; i < colRangeAr[1]; i++){
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
							char keyChar = GREEK_ALPHA[wordCounter%GREEK_ALPHA_LEN];
							horIntWordMap.put(keyChar, horWordStart);
							rowAr[i-smallestCol] = keyChar;
									//Integer.toString(wordCounter).charAt(0);
							wordAdded = true;
						}
						if(null != (verWordStart=boardNode
								.containsWordStart(boardPos, WordOrientation.VERTICAL))){
							if(!wordAdded){
								wordCounter++;
							}
							char keyChar = GREEK_ALPHA[wordCounter % GREEK_ALPHA_LEN];
							//if(true) throw new RuntimeException(keyChar+"");
							verIntWordMap.put(keyChar, verWordStart);
							//take care of double digits!!
							rowAr[i-smallestCol] = keyChar;
									//Integer.toString(wordCounter).charAt(0);
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
			StringBuilder sb = new StringBuilder(500);
			for(char[] rowAr : boardRowList){
				for(char c : rowAr){
					String s = c + " ";
					System.out.print(s);
					sb.append(s);
				}
				System.out.println();
				sb.append("\n");
				//System.out.println(Arrays.toString(rowAr));
			}
			System.out.println("horIntWordMap "+horIntWordMap);
			sb.append("horIntWordMap "+horIntWordMap).append("\n");
			System.out.println("verIntWordMap "+verIntWordMap);
			sb.append("verIntWordMap "+verIntWordMap).append("\n");
			return sb.toString();
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
					node.addBoardPosition(curChar, boardPos, this, orient, rowStart, colStart+i);
					colSet.add(colStart + i);
					//
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
					node.addBoardPosition(curChar, boardPos, this, orient, rowStart+i, colStart);
					rowSet.add(rowStart+i);
					//
					
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
		/**board positions and their corresponding characters, 
		since different pos in board can overlap with diff letters.*/
		Map<BoardPosition, Character> boardPosCharMap 
			= new HashMap<BoardPosition, Character>();
		Set<BoardPosition> boardPositionHorSet;
		Set<BoardPosition> boardPositionVerSet;
		/**membership indicates start of word (value) at this node for the BoardPosition key.
		Used for creating final puzzle visualization.*/
		Map<BoardPosition, String> horBoardPosWordMap = new HashMap<BoardPosition, String>();
		Map<BoardPosition, String> verBoardPosWordMap = new HashMap<BoardPosition, String>();
		/**distance to closest intersection, whether vertical or horizontal. Updated when
		adding to either BoardPosWordMap.*/
		Map<BoardPosition, Integer> distToIntersectMap = new HashMap<BoardPosition, Integer>();
		
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
				Board board, WordOrientation orient, int row, int col){
			
			//intersection if this node already contains boardPosition
			if(boardPosCharMap.containsKey(boardPosition)){
				this.distToIntersectMap.put(boardPosition, 0);
				//update distances in all four directions
				updateNeighborIntersectDist(boardPosition, board, row, col);
			}else{
				//add to distToIntersectMap based on dist along orient direction
				if(WordOrientation.HORIZONTAL == orient){
					Integer nearColDist;
					if(null != board.board[row][col-1] &&
							(nearColDist=board.board[row][col-1].distToIntersectMap.get(boardPosition)) != null){
						this.distToIntersectMap.put(boardPosition, nearColDist+1);
					}else if(null != board.board[row][col+1]
							&& (nearColDist=board.board[row][col+1].distToIntersectMap.get(boardPosition)) != null){
						this.distToIntersectMap.put(boardPosition, nearColDist+1);
					}else{
						//in case of first letter in word
						this.distToIntersectMap.put(boardPosition, Integer.MAX_VALUE);
					}
				}else{
					Integer nearRowDist;
					if(null != board.board[row-1][col]
							&& (nearRowDist=board.board[row-1][col].distToIntersectMap.get(boardPosition)) != null){
						this.distToIntersectMap.put(boardPosition, nearRowDist+1);
					}else if(null != board.board[row+1][col]
							&& (nearRowDist=board.board[row+1][col].distToIntersectMap.get(boardPosition)) != null){
						this.distToIntersectMap.put(boardPosition, nearRowDist+1);
					}else{
						this.distToIntersectMap.put(boardPosition, Integer.MAX_VALUE);
					}
				}
			}
			
			this.boardPosCharMap.put(boardPosition, letter_);
			if(WordOrientation.HORIZONTAL == orient){
				this.boardPositionHorSet.add(boardPosition);				
			}else{
				this.boardPositionVerSet.add(boardPosition);
			}
			//add or adjust dist to nearest intersection of this node.
			//also need board to walk and figure out distance.			
		}
		
		/**
		 * Add or update dist to nearest intersection of this node, dist
		 * at this node is 0, since it's intersection.
		 * @param boardPosition
		 * @param board
		 * @param row
		 * @param col
		 */
		void updateNeighborIntersectDist(BoardPosition boardPosition, Board board, 
				int row, int col){

			//look before
			updateNeighborIntersectDist(boardPosition, board, WordOrientation.HORIZONTAL,
					row, col, -1);
			//look after
			updateNeighborIntersectDist(boardPosition, board, WordOrientation.HORIZONTAL,
					row, col, 1);
			
			//look up
			updateNeighborIntersectDist(boardPosition, board, WordOrientation.VERTICAL,
					row, col, -1);
			//look down
			updateNeighborIntersectDist(boardPosition, board, WordOrientation.VERTICAL,
					row, col, 1);
		}
		
		/**
		 * 
		 * @param boardPosition
		 * @param board
		 * @param orient
		 * @param row
		 * @param col
		 * @param toAdd amount to add each step, indicates whether going up (-1) or down (1)
		 * (or before or after), depending on orient, resp.
		 */
		void updateNeighborIntersectDist(BoardPosition boardPosition, Board board, 
				WordOrientation orient, int row, int col, int toAdd){
			
			final int initialDist = 1;
			int curDist = initialDist;
			BoardNode nearNode;
			if(WordOrientation.HORIZONTAL == orient){
				int curCol = col+toAdd;
				Integer nearColDist;
				nearNode = board.board[row][curCol];
				while(null != nearNode && (nearColDist=nearNode.distToIntersectMap.get(boardPosition)) != null
						&& nearColDist > curDist){					
					nearNode.distToIntersectMap.put(boardPosition, curDist);
					curDist++;
					curCol += toAdd;
					nearNode = board.board[row][curCol];
				}
			}else{
				int curRow = row+toAdd;
				Integer nearRowDist;
				nearNode = board.board[curRow][col];
				while(null != nearNode && (nearRowDist=nearNode.distToIntersectMap.get(boardPosition)) != null
						&& nearRowDist > curDist){					
					nearNode.distToIntersectMap.put(boardPosition, curDist);
					curDist++;
					curRow += toAdd;
					nearNode = board.board[curRow][col];
				}
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
				/*add single-intersection word, only consider next longest word*/
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
		
		/**
		 * 
		 * @param board
		 * @return integer array of first and last col to print.
		 */
		int[] getColRangeToPrint(Board board){
			
			//these sets are sorted.
			Set<Integer> rowSet = board.rowSet;
			List<Integer> colList = new ArrayList<Integer>(board.colSet);
			
			BoardNode[][] boardNodeAr = board.board;
			
			int firstCol = colList.get(0);
			int colListSz = colList.size();
			int lastCol = colList.get(colListSz-1);
			//int midCol = colList.get(colListSz/2);			
			//boolean colContains = true;
			int curColIndex = colListSz/2;
			colLoop: while(curColIndex > -1){
				//go through all applicable rows in a column
				//boolean curColContains = false;
				for(int curRow : rowSet){
					BoardNode curNode = boardNodeAr[curRow][colList.get(curColIndex)];
					if(null != curNode
							&& null != curNode.containsBoardPosition(this)){
						//curColContains = true;
						curColIndex--;
						continue colLoop;
					}					
				}
				//if(!curColContains){
				if(curColIndex > 0){
					//include extra col for padding
					curColIndex--;
				}
					firstCol = colList.get(curColIndex);
					break colLoop;
				//}
			}
			curColIndex = colListSz/2;
			lastColLoop: while(curColIndex < colListSz){
				//go through all applicable rows in a column
				//boolean curColContains = false;
				for(int curRow : rowSet){
					BoardNode curNode = boardNodeAr[curRow][colList.get(curColIndex)];
					if(null != curNode
							&& null != curNode.containsBoardPosition(this)){
						//curColContains = true;
						curColIndex++;
						continue lastColLoop;
					}					
				}
				if(curColIndex < colListSz-1){
					//include extra col for padding
					curColIndex++;
				}
				lastCol = colList.get(curColIndex);
				break lastColLoop;
			}
			return new int[]{firstCol, lastCol};	
		}		
		
	}/*end of BoardPosition class*/
	
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
		//remove duplicates
		wordsList = new ArrayList<String>(new HashSet<String>(wordsList));
		Collections.sort(wordsList, new WordComparator());
		String firstWord = wordsList.get(0);
		wordsList.remove(0);
		Board board = new Board(firstWord, wordsList);
		
		List<BoardPosition> satBoardPosList = board.build();
		//StringBuilder sb = new StringBuilder(5000);
		List<String> puzzleList = new ArrayList<String>();
		for(BoardPosition boardPos : satBoardPosList){
			String solStr = board.visualizeBoardPosition(boardPos);
			//sb.append(solStr).append("\n");
			puzzleList.add(solStr);
			String puzzleStr = board.visualizeBoardPositionPuzzle(boardPos);
			//sb.append(puzzleStr).append("\n");
			puzzleList.add(puzzleStr);
			System.out.println("intersection count: " + boardPos.totalWordIntersectionCount);
			puzzleList.add("Words intersection count: "+ boardPos.totalWordIntersectionCount);
		}
		puzzleList.add("total number of solutions: " + satBoardPosList.size());
		String fileStr = "src/crossword/data/puzzles.txt";
		writeToFile(puzzleList, Paths.get(fileStr));
	}
	
	public static void writeToFile(List<? extends CharSequence> contentList, Path fileToPath) {
		try {
			Files.write(fileToPath, contentList, Charset.forName(DEFAULT_ENCODING));
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("IOException while writing results to file: " + e);
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
		
		wordsAr = new String[]{"hyphae", "barbie", "canadian", "pythagorus","binomials","lobster","hamilton",
				"cosine","burritos","charlesvillage","messiah","orioles","yankees"
				};//*/
		wordsAr = new String[]{"handel","stnick","violin","stnick","ravens","pickle","phonebook",
				"fortran","harpsichord","workbench","teacup","redwine","eyebee","blubes","phonebook","allbutone"
				};
		//"handel","sam","stnick","ravens","violin","vlp",bach,"stnick","ravens","orioles","yankees","seven","mikecharlie","pickle","phonebook","crab",
		//"fortran"
		wordsAr = new String[]{"mikecharlie","towson","bach","seven","crab","uptheroad","totowson","iliad",
				"quadratic","hypotenuse","eclipse","bailey","kepler","pluto"};
		wordsAr = new String[]{"newton","rowboat","zero","capecod","hood","capemay",
				"avalon","thecod","buffalo","frangipane","blueberry","baritone",
				"tycho","sam"};
		wordsAr = new String[]{"maine","barefoot","makeitup","abulge","bowdoin","cherubs",
				"sarsaparilla","meter","nightshift","sargents","cosmic","blue","yago",
				"holehouse","brothertrap","nookieplace","benandjerry"};
		
		wordsAr = new String[]{"watermelon","banana","apple","pineapple","orange","lemon",
				"kiwi","cherry","blueberry"};
		List<String> wordsList = new ArrayList<String>();
		for(String word : wordsAr){
			wordsList.add(word);
		}
		processSet(wordsList);		
	}
	
}
