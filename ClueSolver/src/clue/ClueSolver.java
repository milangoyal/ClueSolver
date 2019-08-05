package clue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Simulator for the board game Clue played from the perspective of Player 1 against
 * bots. Can be played with original 21 cards or a customer number of cards/opponents.
 * Game data about opponents hands (deduced through bot responses) is stored from the perspective 
 * of player 1 which is used by {@link ClueFileWriter} to generate a WCSP Lyft input file to
 * deduce possible game solutions. 
 * @author Milan
 *
 */
public class ClueSolver {
	
	/**
	 * Original 21 Clue card names for a default game. Used in printing game messages.
	 */
	public static final String[] cardNames = {"Miss Scarlet", "Professor Plum", "Mrs. Peacock", "Mr. Green",
			"Colonel Mustard", "Mrs. White", "Kitchen", "Ballroom", "Conservatory", "Billiard Room",
			"Library", "Study", "Hall", "Lounge", "Dining Room", "Candlestick", "Knife", "Lead Pipe",
			"Revolver", "Rope", "Monkey Wrench"};
	
	private int numberPlayers;
	private int numberSuspects;
	private int numberPlaces;
	private int numberWeapons;
	
	/**
	 * A default game uses the original 21 Clue cards. Game messages will use {@link #cardNames}.
	 */
	private boolean defaultGame;
	
	
	/**
	 * True if game is over (Player 1 (you) has won or lost...game is irrelevant after)
	 */
	private boolean gameOver;


	/**
	 * Holds all cards each player is assigned as well as those in the case file. Solution for proper function of AI opponents.
	 * Should only be accessed by user for verification of WCSP solver results. Index 0 refers to player 1 with the last
	 * index referring to the content of the weapons case file slot.
	 */
	private List<HashSet<Integer>> solution;


	/**
	 * Returns whose turn it is currently to make an accusation.
	 */
	private int currentTurn;
	
	/**
	 * Only relevant to Player 1. Set to true if player 1 has already made a suggestion and it is still their turn.
	 * An accusation can be made after a suggestion or the turn can be ended after the suggestion.
	 */
	private boolean suggestionMade;
	
	/**
	 * For each set {Suspects, Places, Weapons} holds the cards for which location is still unknown.
	 */
	private List<HashSet<Integer>> unknowns;
	
	/**
	 * For each player, holds a set of the cards they are known to hold.
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
	 * Keeps track of when players admit to a suggestion in the format {Suspect, Place, Weapon},
	 * so that the constraint that the player must hold at least one of these three cards
	 * can be added to the WCSP solver. Each entry is a list of ints formatted as such: {player, 
	 * suspect, place, weapon}
	 */
	private List<List<Integer>> suggestionConstraints;
	

	
	/**
	 * A string that holds a growing explanation of what moves are occurring each turn.
	 */
	private String gameMessages;
	

	
	/*
	 * --------------------------------------------------------------------------
	 * Public API
	 * --------------------------------------------------------------------------
	 */
	
	/**
	 * @return Number of players.
	 */
	public int getNumberPlayers() {
		return numberPlayers;
	}
	
	/**
	 * @return Number of suspect cards.
	 */
	public int getNumberSuspects() {
		return numberSuspects;
	}

	/**
	 * @return Number of place cards.
	 */
	public int getNumberPlaces() {
		return numberPlaces;
	}

	/**
	 * @return Number of weapon cards.
	 */
	public int getNumberWeapons() {
		return numberWeapons;
	}
	
	/**
	 * @return Unmodifiable copy of {@link #hands}.
	 */
	public List<HashSet<Integer>> getHands() {
		return Collections.unmodifiableList(hands);
	}
	
	/**
	 * @return Unmodifiable copy of {@link ClueSolver#solution}. 
	 */
	public List<HashSet<Integer>> getSolution() {
		return Collections.unmodifiableList(solution);
	}
	
	/**
	 * @return Unmodifiable copy of {@link ClueSolver#suggestionConstraints}. 
	 */
	public List<List<Integer>> getSuggestionConstraints() {
		return Collections.unmodifiableList(suggestionConstraints);
	}
	
	/**
	 * @return Public getter for {@link #gameMessages}
	 */
	public String getGameMessages() {
		return this.gameMessages;
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
	 * @return numerical probability given card is in given location depending on game state information from Player 1's perspective
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
	
	/**
	 * Creates a game using the original 21 cards from the board game Clue
	 * @param numberPlayers
	 */
	public ClueSolver(int numberPlayers) {
		this(numberPlayers, 6, 9, 6);
		this.defaultGame = true;
	}
	
	/**
	 * Creates a clue game with a custom number of cards
	 */
	public ClueSolver(int numberPlayers, int numberSuspects, int numberPlaces, int numberWeapons) {
		this.numberPlayers = numberPlayers;
		this.numberSuspects = numberSuspects;
		this.numberPlaces = numberPlaces;
		this.numberWeapons = numberWeapons;
		this.defaultGame = false;
		this.gameOver = false;
		
		this.solution = new ArrayList<HashSet<Integer>>();
		this.unknowns = new ArrayList<HashSet<Integer>>();
		this.hands = new ArrayList<HashSet<Integer>>();
		this.freeSlots = new ArrayList<Integer>();
		this.restrictions = new ArrayList<HashSet<Integer>>();
		this.suggestionConstraints = new ArrayList<List<Integer>>();
		this.gameMessages = "";
		
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
	
	/**
	 * Starts the game by revealing Player 1's hand and giving them the first turn
	 */
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
		suggestionMade = false;
	}
	
	public String simulateOpenentTurns() {
		String message = "";
		if (gameOver) {
			message = "The game is already over\n";
		}
		else {
			Random rand = new Random();
			while(currentTurn != 0 && !this.gameOver) {
				int randomSuspect = rand.nextInt(numberSuspects);
				int randomPlace = numberSuspects + rand.nextInt(numberPlaces);
				int randomWeapon = numberSuspects + numberPlaces + rand.nextInt(numberWeapons);
				message += suggest(randomSuspect, randomPlace, randomWeapon);
			}
		}
		gameMessages += message;
		return message;
	}
	
	public String enterPlayer1Turn(int suspect, int place, int weapon) {
		String message = "";
		if (gameOver) {
			message = "The game is already over\n";
		}
		else if (currentTurn != 0) {
			message = "Not Player 1's (your) turn!\n";
		}
		else if (suggestionMade) {
			message = "Player 1 (you) already made a suggestion this turn. You may make an accusation or end your turn\n";
		}
		else if (getCardType(suspect) != 0 || getCardType(place) != 1 || getCardType(weapon) != 2) {
			message = "Please enter a proper suspect, place, and weapon card\n";
		}
		else {
			message += suggest(suspect, place, weapon);
		}
		gameMessages += message;
		return message;
	}
	
	public void endPlayer1Turn() {
		if (currentTurn == 0) {
			currentTurn++;
			suggestionMade = false;
		}
	}
	
	public String accuse(int suspect, int place, int weapon) {
		String message = "";
		HashSet<Integer> weapon_soln = solution.get(solution.size()-1);
		HashSet<Integer> place_soln = solution.get(solution.size()-2);
		HashSet<Integer> suspect_soln = solution.get(solution.size()-3);
		
		if (gameOver) {
			message = "The game is already over\n";
		}
		else if (currentTurn != 0) {
			message = "Not Player 1's (your) turn!\n";
		}
		
		else if (getCardType(suspect) != 0 || getCardType(place) != 1 || getCardType(weapon) != 2) {
			message = "Please enter a proper suspect, place, and weapon card\n";
		}
		else if (suspect_soln.contains(suspect) && place_soln.contains(place) && weapon_soln.contains(weapon)) {
			this.gameOver = true;
			message = "You correctly guessed the case file and WIN THE GAME!\n";
		}
		else {
			this.gameOver = true;
			message = "You guessed the case file wrong and LOSE THE GAME\n";
		}
		this.gameMessages += message;
		return message;
	}
	
	/*
	 * ----------------------
	 * Private Helper Functions
	 * ----------------------
	 */
	
	/**
	 * Current player makes a suggestion of case file contents, which is then passed around to other players until someone
	 * can refute the claim or it makes it all the way back to the suggester implying the case file contents
	 * have been correctly guessed.
	 * @param suspect
	 * @param place
	 * @param weapon
	 * @return
	 */
	private String suggest(Integer suspect, Integer place, Integer weapon) {
		String message = "";
		if (gameOver) {
			message = "The game is already over\n";
			this.gameMessages += message;
			return message;
		}
		
		if (currentTurn == 0) {
			if (suggestionMade) {
				message = "You have already made a suggestion this turn. Please make an accusation or end your turn\n";
				this.gameMessages += message;
				return message;
			}
		}
		
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
		//Pass suggestion around in a circle until someone admits to a card or suggestion
		//makes the round all the way back to whoever started it
		while (accusedPlayer != currentTurn) {
			int accusedResponse = suggestResponse(accusedPlayer, suspect, place, weapon);
			//Accused does not hold any of the three cards
			if (accusedResponse == -1) {
				message += passOnSuggest(accusedPlayer, suspect, place, weapon);

							}
			//Accused holds one of three cards
			else {
				message += admitOnSuggest(currentTurn, accusedPlayer, suspect, place, weapon);
				break;
			}
			//Pass on suggestion to next player (if no one has admitted yet)
			accusedPlayer++;
			if (accusedPlayer == numberPlayers) {
				accusedPlayer = 0;
			}
		}
		
		if (currentTurn != 0 && solution.get(solution.size()-3).contains(suspect) && solution.get(solution.size()-2).contains(place) 
				&& solution.get(solution.size()-1).contains(weapon)) {
			message += "Player " + (currentTurn+1) + " correctly guessed the contents of the case file and wins the game!\n";
			this.gameOver = true;
		}
		
		//Player 1 may still make an accusation before ending their turn
		if (currentTurn == 0) {
			suggestionMade = true;
		}
		//End of a suggestion is end of other players' (AI) turns
		else {
			currentTurn++;
			if (currentTurn == numberPlayers) {
				currentTurn = 0;
			}
		}
		return message;
	}
	
	/**
	 * Passes on a suggest, meaning the player does not hold any of three cards form the suggestion and
	 * therefore cannot refute the claim.
	 * @param player
	 * @param suspect
	 * @param place
	 * @param weapon
	 * @return
	 */
	private String passOnSuggest(int player, Integer suspect, Integer place, Integer weapon) {
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
	private Integer suggestResponse(int player, Integer suspect, Integer place, Integer weapon) {
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
	
	/**
	 * Player who has received a suggestion refutes the claim by revealing to suggester that they hold one of the cards in the
	 * suggestion.
	 * @param playerAccuser
	 * @param playerAccused
	 * @param suspect
	 * @param place
	 * @param weapon
	 * @return
	 */
	private String admitOnSuggest(int playerAccuser, int playerAccused, Integer suspect, Integer place, Integer weapon) {
		if (currentTurn == 0) {
			Integer admittedCard = suggestResponse(playerAccused, suspect, place, weapon);
			if (!hands.get(playerAccused).contains(admittedCard)) {
				hands.get(playerAccused).add(admittedCard);
				unknowns.get(getCardType(admittedCard)).remove(admittedCard);
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
		}
		else {
			List<Integer> constraint = new ArrayList<Integer>();
			constraint.add(playerAccused);
			constraint.add(suspect);
			constraint.add(place);
			constraint.add(weapon);
			if (!this.suggestionConstraints.contains(constraint)) {
				suggestionConstraints.add(constraint);
			}
			String message = "";
			if (playerAccused == 0) {
				message += "You reveal you are holding one of the three cards to player " + (playerAccuser+1) + "\n";
			}
			else {
				message += "Player " + (playerAccused+1) + " reveals they are holding one of the three cards to player " + (playerAccuser+1) + "\n";
			}
			return message;
		}
	}

}
