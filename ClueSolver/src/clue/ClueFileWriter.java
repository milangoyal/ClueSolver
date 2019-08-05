package clue;
import java.util.List;

/**
 * Generates a WCSP Lyft input data from {@link ClueSolver} game data. Used to solve top k
 * possible solutions to Clue game state (location of all cards).
 * @author Milan
 *
 */
public class ClueFileWriter {
	
	private ClueSolver data;
	int constraintCounter;
	double globalMax;
	
	public ClueFileWriter(ClueSolver data) {
		this.data = data;
		this.constraintCounter = 0;
		this.globalMax = 0;
	}
	
	/**
	 * Returns the full header and body of the data file containing all variables and constraints
	 * @return
	 */
	public String getInputString() {
		String stringBuilder = "";
		stringBuilder += initializeVariables();
		stringBuilder += oneEachLocation();
		stringBuilder += caseFiles();
		stringBuilder += suggestionConstraints();
		//Rounding up global max to an int because Top K Solutions program requires an int
		String max = Integer.toString((int) Math.ceil(globalMax));
		max = max.replaceAll("\n", "");
		stringBuilder = stringBuilder.replaceAll("-", max);
		stringBuilder = prepend() + stringBuilder;
		return stringBuilder;
	}
	
	/*
	 * ------------------------Private helper functions-------------------------------------------------
	 */
	
	/**
	 * Returns the header to the input data file. Should be added last, as the header contains the global maximum
	 * which can only be calculated after all constraints have been added.
	 * @return
	 */
	private String prepend() {
		int numberLocations = data.getNumberPlayers() + 3;
		int numberCards = data.getNumberSuspects() + data.getNumberPlaces() + data.getNumberWeapons();
		//Rounding up global max to an int because Top K Solutions program requires an int
		String stringBuilder = "ClueGame " + (numberLocations*numberCards) + " 2 " + constraintCounter + " " + (int) Math.ceil(globalMax) + "\n";
		stringBuilder += "2";
		for (int i = 0; i < ((numberLocations*numberCards)-1); i++) {
			stringBuilder += " 2";
		}
		stringBuilder += "\n";
		return stringBuilder;
	}
	
	
	/**
	 * Initializes all variables where each variable represents one possible location for each of the cards.
	 * @return
	 */
	private String initializeVariables() {
		String builder = "";
		
		int numberPlayers = data.getNumberPlayers();
		int numberSuspects = data.getNumberSuspects();
		int numberPlaces = data.getNumberPlaces();
		int numberWeapons = data.getNumberWeapons();
		int numberLocations = numberPlayers + 3;
		int numberCards = numberSuspects + numberPlaces + numberWeapons;
		
		for (int i = 0; i < numberCards; i++) {
			for (int j = 0; j < numberLocations; j++) {
				String constraint = "";

				double weightTrue = probToWeight(data.getProbability(i, data.getCardType(i), j));
				double weightFalse = probToWeight(1 - data.getProbability(i, data.getCardType(i), j));
				String wTrue = "";
				String wFalse = "";
				//"-" represent probability of 0 but needs to be replaced by global maximum after
				//all constraints have been entered. 
				if (weightTrue == -1) {
					wTrue = "-\n";
				}
				else {
					wTrue = String.format("%.5g%n", weightTrue);
				}
				if (weightFalse == -1) {
					wFalse = "-\n";
				}
				else {
					wFalse = String.format("%.5g%n", weightFalse);
				}
				int cardNumber = i*numberLocations + j;
				constraint += 1 + " " + cardNumber + " " + 0 + " " + 2 + "\n";
				constraint += 0 + " " + wFalse;
				constraint += 1 + " " +  wTrue;
				
				builder += constraint;
				globalMax += Math.max(weightTrue, weightFalse);
				constraintCounter++;
			}
		}
		return builder;
		
	}
	
	/**
	 * Adds constraint that each card must be in one and only one location. However, this location can be anywhere.
	 * @return
	 */
	private String oneEachLocation() {
		int numberPlayers = data.getNumberPlayers();
		int numberSuspects = data.getNumberSuspects();
		int numberPlaces = data.getNumberPlaces();
		int numberWeapons = data.getNumberWeapons();
		int numberLocations = numberPlayers + 3;
		
		String stringBuilder = "";
		
		for (int i = 0; i < numberSuspects; i++) {
			stringBuilder += numberLocations;
			for (int j = 0; j < numberLocations; j++) {
				stringBuilder += " " + (i*numberLocations+j);
			}
			stringBuilder += " - " + numberLocations + "\n";
			stringBuilder += createTuplesMatrix(numberLocations);
			constraintCounter++;
		}
		
		for (int i = numberSuspects; i < numberSuspects + numberPlaces; i++) {
			stringBuilder += numberLocations;
			for (int j = 0; j < numberLocations; j++) {
				stringBuilder += " " + (i*numberLocations+j);
			}
			stringBuilder += " - " + numberLocations + "\n";
			stringBuilder += createTuplesMatrix(numberLocations);
			constraintCounter++;
		}
		
		for (int i = numberSuspects+numberPlaces; i < numberSuspects+numberPlaces+numberWeapons; i++) {
			stringBuilder += numberLocations;
			for (int j = 0; j < numberLocations; j++) {
				stringBuilder += " " + (i*numberLocations+j);
			}
			stringBuilder += " - " + numberLocations + "\n";
			stringBuilder += createTuplesMatrix(numberLocations);
			constraintCounter++;
		}
		
		return stringBuilder;
	}
	
	/**
	 * Adds constraints related to the case files. Each case file slot can only hold a specific type of card (Suspect,
	 * Weapon, Place) and each slot must hold one and only card.
	 * @return
	 */
	private String caseFiles() {
		int numberPlayers = data.getNumberPlayers();
		int numberSuspects = data.getNumberSuspects();
		int numberPlaces = data.getNumberPlaces();
		int numberWeapons = data.getNumberWeapons();
		int numberLocations = numberPlayers + 3;
		
		String stringBuilder = "";
		stringBuilder += numberSuspects;
		for (int i = 0; i < numberSuspects; i++) {
			stringBuilder += " " + (i*numberLocations+(numberLocations-3));
		}
		stringBuilder += " - " + numberSuspects + "\n";
		stringBuilder += createTuplesMatrix(numberSuspects);
		constraintCounter++;
		
		stringBuilder += numberPlaces;
		for (int i = numberSuspects; i < numberSuspects + numberPlaces; i++) {
			stringBuilder += " " + (i*numberLocations+(numberLocations-2));
		}
		stringBuilder += " - " + numberPlaces + "\n";
		stringBuilder += createTuplesMatrix(numberPlaces);
		constraintCounter++;
		
		stringBuilder += numberWeapons;
		for (int i = numberSuspects + numberPlaces; i < numberSuspects + numberPlaces + numberWeapons; i++) {
			stringBuilder += " " + (i*numberLocations+(numberLocations-1));
		}
		stringBuilder += " - " + numberWeapons + "\n";
		stringBuilder += createTuplesMatrix(numberWeapons);
		constraintCounter++;
		
		return stringBuilder;
	}
	
	/**
	 * Converts suggestion information into constraints, i.e. whenever a player passes
	 * on a suggestion they must not hold any of three cards from the suggestion
	 * @return
	 */
	private String suggestionConstraints() {
		List<List<Integer>> suggestions = data.getSuggestionConstraints();
		int numberLocations = data.getNumberPlayers() + 3;

		String stringBuilder = "";
		for (int i = 0; i < suggestions.size(); i++) {
			int player = suggestions.get(i).get(0);
			int suspect = suggestions.get(i).get(1);
			int place = suggestions.get(i).get(2);
			int weapon = suggestions.get(i).get(3);
			stringBuilder += "3 " + (suspect*numberLocations+player) + " "
					+ (place*numberLocations+player) + " " + (weapon*numberLocations+player)
					+ " " + "0 1\n";
			stringBuilder += "0 0 0 -\n";
			constraintCounter++;
		}
		
		return stringBuilder;
	}
	
	private String createTuplesHeader(int numberVars, String defaultValue, int deviatingTuples, int... vars) {
	    String stringBuilder = "" + numberVars + " " + vars[0];
		for (int i = 1; i < vars.length; i++) {
	        stringBuilder += " " + vars[i];
	    }
		stringBuilder += " " + defaultValue + " " + deviatingTuples + "\n";
		return stringBuilder;
	}
	
	/**
	 * Creates a matrix of tuples to signify mutual exclusion.
	 * Only one of the variables in the set can be true. Valid
	 * tuples have a weight of 0, and all other tuples should
	 * be defaulted to global maximum or a large number in constraint
	 * header.
	 * @param columns
	 * @return
	 */
	private String createTuplesMatrix(int columns) {
		String stringBuilder = "";
		for (int i = 0; i < columns; i++) {
			for (int j = 0; j < columns; j++) {
				if (i == j) {
					stringBuilder += "1 ";
				}
				else {
					stringBuilder += "0 ";
				}
			}
			stringBuilder += "0\n";
		}
		return stringBuilder;
	}
	
	/**
	 * Converts probability into weights for constraints by taking the the -log.
	 * Higher probabilities lead to lower weights.
	 * @param prob
	 * @return
	 */
	private double probToWeight(double prob) {
		if (prob == 0) {
			return -1;
		}
		if (prob == 1) {
			return 0;
		}
		double weight = Math.log(prob) * -1;
		return weight;
	}
	
}
