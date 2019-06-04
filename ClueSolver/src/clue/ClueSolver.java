package clue;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ClueSolver {
	private int numberPlayers;
	private int numberSuspects;
	private int numberPlaces;
	private int numberWeapons;
	private boolean defaultGame;
	
	public static final String[] cardNames = {"Miss Scarlet", "Professor Plum", "Mrs. Peacock", "Mr. Green",
			"Colonel Mustard", "Mrs. White", "Kitchen", "Ballroom", "Conservatory", "Billiard Room",
			"Library", "Study", "Hall", "Lounge", "Dining Room", "Candlestick", "Knife", "Lead Pipe",
			"Revolver", "Rope", "Monkey Wrench"};
	
	/**
	 * Holds the trustworthiness for players 0 to n-1. Must be in the range of (0,1]. A value of 1
	 * implies they never lie (cheat) when responding to accusations. Value cannot be 0, as a player cannot
	 * always lie without getting caught.
	 */
	private List<Double> trust;
	
	public List<Double> getTrust() {
		return trust;
	}


	/**
	 * Holds all cards each player is assigned as well as those in the case file. Solution for proper function of AI opponents.
	 * Should only be accessed by user for verification of WCSP solver results. 
	 */
	public List<HashSet<Integer>> solution;
	
	public List<HashSet<Integer>> getSolution() {
		return solution;
	}


	/**
	 * Returns whose turn it is currently to make an accusation
	 */
	private int currentTurn;
	
	/**
	 * For each set {Suspects, Places, Weapons} holds the cards for which location is still unknown
	 */
	private List<HashSet<Integer>> unknowns;
	
	/**
	 * For each player, holds a set of the cards they are known to hold
	 */
	private List<HashSet<Integer>> hands;
	
	/**
	 * For each player, holds the number of free slots (unknown cards) in their hand 
	 */
	private List<Integer> freeSlots;
	
	/**
	 * For each player as well as the the three case file slots, 
	 * holds a set of cards that they must not be holding (information is obtained as game is played)
	 */
	private List<HashSet<Integer>> restrictions;
	
	/**
	 * Keeps track of when players admit to an accusation in the format {Suspect, Place, Weapon},
	 * so that the constraint that the player must hold at least one of these three cards
	 * can be added to the WCSP solver. Each entry is a list of ints formatted as such: {player, 
	 * suspect, place, weapon}
	 */
	private List<List<Integer>> accusationConstraints;
	
	public List<List<Integer>> getAccusationConstraints() {
		return accusationConstraints;
	}

	public void setAccusationConstraints(List<List<Integer>> accusationConstraints) {
		this.accusationConstraints = accusationConstraints;
	}
	
	private String gameMessages;
	
	public String getGameMessages() {
		return gameMessages;
	}

	/**
	 * Given card number returns whether it is {Suspect = 0, Place = 1, or Weapon = 2} card.
	 * Returns -1 if not within any of the ranges.
	 * @param card
	 * @return
	 */
	public int getCardType(int card) {
		if (card < numberSuspects) {
			return 0;
		}
		else if (card < numberSuspects + numberPlaces) {
			return 1;
		}
		else if (card < numberSuspects + numberPlaces + numberWeapons) {
			return 2;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * @param card integer between 0 and (numberSuspects + numberPlaces + numberWeapons-1)
	 * @param cardType integer between 0-2 {Suspects, Places, Weapons}
	 * @param location integer between 0-(numberPlayers+2) {Player1, ..., PlayerN, Suspect Case File, Places Case File, Weapons Case File)
	 * @return
	 */
	public double getProbability(int card, int cardType, int location) {
		//Card cannot be in this location
		if (restrictions.get(location).contains(card)) {
			return 0;
		}
		
		//Card has already been discovered to be in player's hand
		if (location < (numberPlayers)) {
			if (hands.get(location).contains(card)) {
				return 1;
			}
		}

		//If location is one of three case file spots, all unknown cards from that category have equal probability
		if (location >= numberPlayers) {
			//Probability of a card being in case file spot with different type is 0
			if (cardType != (location - numberPlayers)) {
				return 0;
			}
			double probability = ( ((double) 1) / unknowns.get(cardType).size());
			return probability;
		}
		//Else calculate as (Probability in player's hand given card not in case file) * (Probability card is not in case file)
		double possibleLocations = 0;
		for (int i = 0; i < freeSlots.size(); i++) {
			if (!restrictions.get(i).contains(card)) {
				possibleLocations += freeSlots.get(i);
			}
		}
		//This means the card's location has already been discovered and it is not this location
		if (possibleLocations == 0) {
			return 0;
		}
		double probability = ( ((double) freeSlots.get(location)) / possibleLocations) * (1.0 - ( ((double) 1) / unknowns.get(cardType).size() ));
		return probability;
	}
	
	private String accuse(Integer suspect, Integer place, Integer weapon) {
//		if (annotate) {
//			if (currentTurn == 0) {
//				System.out.println("You accuse suspect: " + suspect + " of using weapon: " +
//				weapon + " at place: " + place + " to commit the crime");
//			}
//			else {
//				System.out.println("Player " + currentTurn + " accuses suspect: " + suspect + " of using weapon: " +
//				weapon + " at place: " + place + " to commit the crime");
//			}
//		}
		
		String message = "";
		if (defaultGame) {
			if (currentTurn == 0) {
				message += "You accuse suspect: " + cardNames[suspect] + " of using weapon: " +
					cardNames[weapon] + " at place: " + cardNames[place] + " to commit the crime\n";
			}
			else {
				message += "Player " + (currentTurn+1) + " accuses suspect: " + cardNames[suspect] + " of using weapon: " +
					cardNames[weapon] + " at place: " + cardNames[place] + " to commit the crime\n";
			}
		}
		else {
			if (currentTurn == 0) {
				message += "You accuse suspect: " + suspect + " of using weapon: " +
					weapon + " at place: " + place + " to commit the crime\n";
			}
			else {
				message += "Player " + (currentTurn+1) + " accuses suspect: " + suspect + " of using weapon: " +
					weapon + " at place: " + place + " to commit the crime\n";
			}
		}
		
		int accusedPlayer = currentTurn + 1;
		if (accusedPlayer == numberPlayers) {
			accusedPlayer = 0;
		}
		//Pass accusation around in a circle until someone admits to a card or accusation
		//makes the round all the way back to whoever started it
		while (accusedPlayer != currentTurn) {
			int accusedResponse = accuseResponse(accusedPlayer, suspect, place, weapon);
			//Accused does not hold any of the three cards
			if (accusedResponse == -1) {
				message += passOnAccuse(accusedPlayer, suspect, place, weapon);

							}
			//Accused holds one of three cards
			else {
				message += admitOnAccuse(currentTurn, accusedPlayer, suspect, place, weapon);
				break;
			}
			//Pass on accusation to next player (if no one has admitted yet)
			accusedPlayer++;
			if (accusedPlayer == numberPlayers) {
				accusedPlayer = 0;
			}
		}
		//End of an accusation is end of that player's turn
		currentTurn++;
		if (currentTurn == numberPlayers) {
			currentTurn = 0;
		}
		return message;
	}
	
	private String passOnAccuse(int player, Integer suspect, Integer place, Integer weapon) {
		HashSet<Integer> playerRestrictions = restrictions.get(player);
		if (!playerRestrictions.contains(suspect)) {
			playerRestrictions.add(suspect);
		}
		if (!playerRestrictions.contains(place)) {
			playerRestrictions.add(place);
		}
		if (!playerRestrictions.contains(weapon)) {
			playerRestrictions.add(weapon);
		}
		
		String message = "";
		if (player == 0) {
			message += "You deny holding any of the three cards\n";
		}
		else {
			message += "Player " + (player+1) + " denies holding any of the three cards\n";
		}
		return message;
//		if (annotate) {
//			if (player == 0) {
//				System.out.println("You deny holding any of the three cards");
//			}
//			else {
//				System.out.println("Player " + player + " denies holding any of the three cards");
//			}
//		}
	}
	
	private boolean hasCard(int player, Integer card) {
		return solution.get(player).contains(card);
	}
	
	/**
	 * If player holds one of the three cards, returns the card they hold. If the player
	 * has previously revealed they hold one of the cards, will return the same card to simulate
	 * strategy.
	 * <p>
	 * Returns -1 otherwise.
	 * @param player
	 * @param suspect
	 * @param place
	 * @param weapon
	 * @return
	 */
	private Integer accuseResponse(int player, Integer suspect, Integer place, Integer weapon) {
		if (hands.get(player).contains(suspect)) {
			return suspect;
		}
		else if (hands.get(player).contains(place)) {
			return place;
		}
		else if (hands.get(player).contains(weapon)) {
			return weapon;
		}
		else if (hasCard(player, suspect)) {
			return suspect;
		}
		else if (hasCard(player, place)) {
			return place;
		}
		else if (hasCard(player, weapon)) {
			return weapon;
		}
		else return -1;
	}
	
	private String admitOnAccuse(int playerAccuser, int playerAccused, Integer suspect, Integer place, Integer weapon) {
		if (currentTurn == 0) {
			Integer admittedCard = accuseResponse(playerAccused, suspect, place, weapon);
			if (!hands.get(playerAccused).contains(admittedCard)) {
				hands.get(playerAccused).add(admittedCard);
				//TODO:verify
				unknowns.get(getCardType(admittedCard)).remove(admittedCard);
				//TODO: verify this works
				Integer remainingFree = freeSlots.get(playerAccused) - 1;
				freeSlots.remove(playerAccused);
				freeSlots.add(playerAccused, remainingFree);
				for (int i = 0; i < restrictions.size(); i++) {
					if (i != playerAccused) {
						if (!restrictions.get(i).contains(admittedCard)) {
							restrictions.get(i).add(admittedCard);
						}
					}
				}
			}
			
			String message = "";
			if (defaultGame) {
				message ="Player " + (playerAccused+1) + " reveals to you they are holding card " + cardNames[admittedCard] + "\n";
			}
			else {
				message ="Player " + (playerAccused+1) + " reveals to you they are holding card " + admittedCard + "\n";
			}
			return message;
//			if (annotate) {
//				System.out.println("Player " + playerAccused + " reveals to you they are holding card " + admittedCard);
//			}
		}
		else {
			List<Integer> constraint = new ArrayList<Integer>();
			constraint.add(playerAccused);
			constraint.add(suspect);
			constraint.add(place);
			constraint.add(weapon);
			if (!this.accusationConstraints.contains(constraint)) {
				accusationConstraints.add(constraint);
			}
			String message = "";
			if (playerAccused == 0) {
				message += "You reveal you are holding one of the three cards to player " + (playerAccuser+1) + "\n";
			}
			else {
				message += "Player " + (playerAccused+1) + " reveals they are holding one of the three cards to player " + (playerAccuser+1) + "\n";
			}
			return message;
//			if (annotate) {
//				System.out.println("Player " + playerAccused + " reveals they are holding one of the three cards to player " + playerAccuser);
//			}
		}
	}
	
	public ClueSolver(int numberPlayers) {
		this(numberPlayers, 6, 9, 6);
		this.defaultGame = true;
	}
	
	public ClueSolver(int numberPlayers, int numberSuspects, int numberPlaces, int numberWeapons) {
		this.numberPlayers = numberPlayers;
		this.numberSuspects = numberSuspects;
		this.numberPlaces = numberPlaces;
		this.numberWeapons = numberWeapons;
		this.defaultGame = false;
		
		this.solution = new ArrayList<HashSet<Integer>>();
		this.unknowns = new ArrayList<HashSet<Integer>>();
		this.hands = new ArrayList<HashSet<Integer>>();
		this.freeSlots = new ArrayList<Integer>();
		this.restrictions = new ArrayList<HashSet<Integer>>();
		this.accusationConstraints = new ArrayList<List<Integer>>();
		this.trust = new ArrayList<Double>();
		
		//One for each category {Suspects, Places, Weapons}
		for (int i = 0; i < 3; i++) {
			unknowns.add(new HashSet<Integer>());
			
			//Populate each category with card numbers from that category
			//All card locations are unknown at start of game
			if (i == 0) {
				for (int j = 0; j < numberSuspects; j++) {
					unknowns.get(i).add(j);
				}
			}
			if (i == 1) {
				for (int j = 0; j < numberPlaces; j++) {
					unknowns.get(i).add(numberSuspects + j);
				}
			}
			if (i == 2) {
				for (int j = 0; j < numberWeapons; j++) {
					unknowns.get(i).add(numberSuspects + numberPlaces + j);
				}
			}
		}
		
		
		
		//Total number of cards players can hold (3 belong in the case file)
		int numberTotalPlayerCards = numberSuspects + numberPlaces + numberWeapons - 3;
		
		//If there is an uneven split, some players will have one more card than others
		int unevenSplit = numberTotalPlayerCards % numberPlayers;
		
		int cardsPerPlayer = numberTotalPlayerCards / numberPlayers;
		
		for (int i = 0; i < numberPlayers; i++) {
			hands.add(new HashSet<Integer>());
			
			if (unevenSplit > 0) {
				freeSlots.add(cardsPerPlayer + 1);
				unevenSplit--;
			}
			else {
				freeSlots.add(cardsPerPlayer);
			}
			
			//All players have default trust of 1.0 (implies no cheating)
			trust.add(1.0);
		}
		
		
		//One for each player's hand and 3 case file spots {Suspect, Places, Weapons}
		for (int i = 0; i < (numberPlayers + 3); i++) {
			solution.add(new HashSet<Integer>());
			restrictions.add(new HashSet<Integer>());
		}
		
		//Create deck of all cards to be split among players and case file
		int numberCards = numberSuspects + numberPlaces + numberWeapons;
		List<Integer> deck = new ArrayList<Integer>();
		for (int i = 0; i < numberCards; i++) {
			deck.add(i);
		}
		
		Random rand = new Random();
		
		//Assign one card from each category to the case file
		Integer suspectCF = rand.nextInt(numberSuspects);
		solution.get(numberPlayers).add(suspectCF);
		deck.remove(suspectCF);
		
		Integer placesCF = numberSuspects + rand.nextInt(numberPlaces);
		solution.get(numberPlayers+1).add(placesCF);
		deck.remove(placesCF);
		
		Integer weaponsCF = numberSuspects + numberPlaces + rand.nextInt(numberWeapons);
		solution.get(numberPlayers+2).add(weaponsCF);
		deck.remove(weaponsCF);
		
		//Split remaining cards evenly between all players
		int currentPlayer = 0;
		while (!deck.isEmpty()) {
			//Deal cards one at a time in order
			if (currentPlayer == numberPlayers) {
				currentPlayer = 0;
			}
			
			int randomCard = rand.nextInt(deck.size());
			solution.get(currentPlayer).add(deck.get(randomCard));
			deck.remove(randomCard);
			
			currentPlayer++;	
		}
		
	}
	
	public void startGame() {
		for (int i = 0; i < solution.get(0).size(); i++) {
			Iterator<Integer> it = solution.get(0).iterator();
			while (it.hasNext()) {
				Integer card = it.next();
				hands.get(0).add(card);
				unknowns.get(getCardType(card)).remove(card);
				for (int j = 1; j < restrictions.size(); j++) {
					if (!restrictions.get(j).contains(card)) {
						restrictions.get(j).add(card);
					}
				}
			}
			Integer remainingFree = 0;
			freeSlots.remove(0);
			freeSlots.add(0, remainingFree);
		}
		
		currentTurn = 0;
	}
	
	public void printGameState() {
		for (int i = 0; i < numberPlayers; i++) {
			String player = "Player " + i + "'s Hand: ";
			System.out.print(player);
			System.out.println(hands.get(i) + " Free Slots: " + freeSlots.get(i));
			System.out.println("Restrictions: " + restrictions.get(i));
			System.out.println("");
		}
	}
	
	public void printSolution() {
		for (int i = 0; i < numberPlayers; i++) {
			System.out.print("Player " + i + "'s Hand: ");
			System.out.println(solution.get(i));
		}
		for (int i = numberPlayers; i < solution.size(); i++) {
			if (i == numberPlayers) {
				System.out.print("Suspect Case File Slot: ");
			}
			else if (i == (numberPlayers+1)) {
				System.out.print("Place Case File Slot: ");
			}
			else if (i == (numberPlayers+2)) {
				System.out.print("Weapon Case File Slot: ");
			}
			System.out.println(solution.get(i));
		}
	}
	
	public String simulateTurns(int numberTurns) {
		String message = "";
		Random rand = new Random();
		for (int i = 0; i < numberTurns; i++) {
			int randomSuspect = rand.nextInt(numberSuspects);
			int randomPlace = numberSuspects + rand.nextInt(numberPlaces);
			int randomWeapon = numberSuspects + numberPlaces + rand.nextInt(numberWeapons);
			message += accuse(randomSuspect, randomPlace, randomWeapon);
		}
		gameMessages += message;
		return message;
	}
	
	public String simulateOpenentTurns() {
		String message = "";
		Random rand = new Random();
		while(currentTurn != 0) {
			int randomSuspect = rand.nextInt(numberSuspects);
			int randomPlace = numberSuspects + rand.nextInt(numberPlaces);
			int randomWeapon = numberSuspects + numberPlaces + rand.nextInt(numberWeapons);
			message += accuse(randomSuspect, randomPlace, randomWeapon);
		}
		gameMessages += message;
		return message;
	}
	
	public String enterPlayerTurn(int suspect, int place, int weapon) {
		if (currentTurn != 0) {
			return "Not Player 0's (your) turn!";
		}
		
		if (getCardType(suspect) != 1 || getCardType(place) != 2 || getCardType(weapon) != 3) {
			return "Please enter a proper suspect, place, and weapon card";
		}
		
		String message = "";
		message += accuse(suspect, place, weapon);
		gameMessages += message;
		return message;
	}
	
	public void printMenu() {
		List<String> options = new ArrayList<String>();
		options.add("1: Simulate turns");
		options.add("2: Print Current Game State");
		options.add("3: Print Game Solution");
		options.add("4: Turn on annotations: prints what is happening each turn to the console");
		options.add("5: Turn off annotations: deafult setting");
		options.add("6: Write current game state to file. This file can be fed to the wcsp solver");
		options.add("7: End program");
		
		for (int i = 0; i < options.size(); i++) {
			System.out.println(options.get(i));
		}
	}
	
	//TODO: Add restrictions (cafe file spots cannot contain any other type of card)
	public static void main(String[] args) {
		System.out.println("Welcome to the WCSP Clue Game Simulator!\n ");
		UserInputManager input = new UserInputManager();
		int players = input.getNumberPlayers();
		int suspects = input.getNumberSuspects();
		int places = input.getNumberPlaces();
		int weapons = input.getNumberWeapons();
		ClueSolver game = new ClueSolver(players, suspects, places, weapons);
		game.startGame();
		System.out.println("What would you like to do? Enter the corresponding number");
		game.printMenu();
		while (true) {
			int selection = input.getIntWithinRange(0, 8);
			if (selection == 0) {
				game.printMenu();
			}
			else if (selection == 1) {
				System.out.println("How many player turns would you like simulate? Enter a number between 1 and 1000");
				int turns = input.getIntWithinRange(1, 1000);
				System.out.println(game.simulateTurns(turns));
			}
			else if (selection == 2) {
				game.printGameState();
			}
			else if (selection == 3) {
				game.printSolution();
			}
			else if (selection == 4) {
				//game.setAnnotate(true);
			}
			else if (selection == 5) {
				//game.setAnnotate(false);
			}
			else if (selection == 6) {
				ClueFileWriter clueWriter = new ClueFileWriter(game);
				String content = clueWriter.getInputString();
			    BufferedWriter writer;
				try {
					writer = new BufferedWriter(new FileWriter("ClueSolverInput", false));
					writer.write(content);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Data successfully written to file named ClueSolverInput");
			}
			else if (selection == 7) {
				input.closeScanner();
				System.out.println("Thanks for playing!");
				break;
			}
		
			System.out.println("Please enter another command or enter 0 to print the menu of commands:");
		}
		
		
//		ClueSolver test = new ClueSolver(2, 2, 3, 4);
//		test.startGame();
//		test.accuse(0, 3, 7);
//		//filler accuse to return to our turn
//		test.accuse(1, 2, 5);
//		test.accuse(1, 2, 5);
//		for (int i = 0; i < test.solution.size(); i++) {
//			System.out.println(test.solution.get(i));
//		}
//		test.printGameState();
//		ClueFileWriter test2 = new ClueFileWriter(test);
//		System.out.println(test2.initializeVariables());	
		

	}


	public int getNumberPlayers() {
		return numberPlayers;
	}



	public void setNumberPlayers(int numberPlayers) {
		this.numberPlayers = numberPlayers;
	}



	public int getNumberSuspects() {
		return numberSuspects;
	}



	public void setNumberSuspects(int numberSuspects) {
		this.numberSuspects = numberSuspects;
	}



	public int getNumberPlaces() {
		return numberPlaces;
	}



	public void setNumberPlaces(int numberPlaces) {
		this.numberPlaces = numberPlaces;
	}



	public int getNumberWeapons() {
		return numberWeapons;
	}


	public void setNumberWeapons(int numberWeapons) {
		this.numberWeapons = numberWeapons;
	}
	
	public List<HashSet<Integer>> getHands() {
		return hands;
	}

}
