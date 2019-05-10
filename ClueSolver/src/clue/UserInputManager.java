package clue;

import java.util.Scanner;

public class UserInputManager {
	private Scanner scanner;
	
	public UserInputManager() {
		scanner = new Scanner(System.in);
	}
	
	public void setGame(ClueSolver game) {
	}
	

	
	public int getNumberPlayers() {
		int players = 0;
		while (true) {
			System.out.println("Please enter the number of players");
			try {
				players = scanner.nextInt();
			} catch(Exception e) {
				System.out.println("Please enter a proper integer");
				continue;
			}
			if (players < 2) {
				System.out.println("Must have at least 2 players to play the game");
			}
			else {
				break;
			}
		}
		return players;
	}
	
//	public List<Double> getPlayerTrustScores() {
//		int numberPlayers = game.getNumberPlayers();
//		int counter = 0;
//		double value = 0;
//		while (true) {
//			System.out.println("Please enter a trust score for player " + counter + " between (0,1]. "
//					+ "0 is not valid because it would imply someone lies no matter what. A score of 1 implies they always "
//					+ "tell the truth (never cheat). A score of 0.7 implies you expect them to lie 30% of the time");
//			try {
//				value = scanner.nextDouble();
//			} catch(Exception e) {
//				System.out.println("Please enter a proper decimal value");
//				continue;
//			}
//			if (value < 2) {
//				System.out.println("Must have at least 2 players to play the game");
//			}
//			else {
//				break;
//			}
//		}
//		List<Double> trustScores = new ArrayList<Double>();
//		for (int i = 0; i < numberPlayers; i++) {
//			try {
//				trustScores.add(scanner.nextDouble());
//			} catch(Exception e) {
//				System.out.println(e);
//			}
//		}
//		//TODO: building GUI instead
//	}
	
	public int getNumberSuspects() {
		int suspects = 0;
		while (true) {
			System.out.println("Please enter the number of suspect cards");
			try {
				suspects = scanner.nextInt();
			} catch(Exception e) {
				System.out.println("Please enter a proper integer");
				continue;
			}
			if (suspects < 1) {
				System.out.println("Must have at least 1 suspect to play the game");
			}
			else {
				break;
			}
		}
		return suspects;
	}
	
	public int getNumberPlaces() {
		int place = 0;
		while (true) {
			System.out.println("Please enter the number of place cards");
			try {
				place = scanner.nextInt();
			} catch(Exception e) {
				System.out.println("Please enter a proper integer");
				continue;
			}
			if (place < 1) {
				System.out.println("Must have at least 1 place to play the game");
			}
			else {
				break;
			}
		}
		return place;
	}
	
	public int getNumberWeapons() {
		int weapons = 0;
		while (true) {
			System.out.println("Please enter the number of weapons cards");
			try {
				weapons = scanner.nextInt();
			} catch(Exception e) {
				System.out.println("Please enter a proper integer");
				continue;
			}
			if (weapons < 1) {
				System.out.println("Must have at least 1 weapon to play the game");
			}
			else {
				break;
			}
		}
		return weapons;
	}
	
	public int getIntWithinRange(int lowerBound, int upperBound) {
		int input = 0;
		while (true) {
			try {
				input = scanner.nextInt();
			} catch(Exception e) {
				System.out.println("Please enter a proper integer");
				continue;
			}
			if (input < lowerBound || input > upperBound) {
				System.out.println("Please enter a number within the range [" + lowerBound + ", " + upperBound + "]");
			}
			else {
				break;
			}
		}
		return input;
	}
	
	public void closeScanner() {
		scanner.close();
	}
	
}
