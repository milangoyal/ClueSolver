package clue;
import java.util.List;

public class ClueFileWriter {
	
	private ClueSolver data;
	int constraintCounter;
	double globalMax;
	
	public ClueFileWriter(ClueSolver data) {
		this.data = data;
		this.constraintCounter = 0;
		this.globalMax = 0;
	}
	
	public String getInputString() {
		String stringBuilder = "";
		stringBuilder += initializeVariables();
		stringBuilder += oneEachLocation();
		stringBuilder += caseFiles();
		stringBuilder += accusationConstraints();
		//String max = String.format("%.5g%n", globalMax);
		//Rounding up global max to an int because Top K Solutions program requires an int
		String max = Integer.toString((int) Math.ceil(globalMax));
		max = max.replaceAll("\n", "");
		stringBuilder = stringBuilder.replaceAll("-", max);
		stringBuilder = prepend() + stringBuilder;
		return stringBuilder;
	}
	
	public String prepend() {
		int numberLocations = data.getNumberPlayers() + 3;
		int numberCards = data.getNumberSuspects() + data.getNumberPlaces() + data.getNumberWeapons();
		//String stringBuilder = "ClueGame " + (numberLocations*numberCards) + " 2 " + constraintCounter + " " + String.format("%.5g%n", globalMax);
		//Had to make globalMax an int for top k solutions
		String stringBuilder = "ClueGame " + (numberLocations*numberCards) + " 2 " + constraintCounter + " " + (int) Math.ceil(globalMax) + "\n";
		stringBuilder += "2";
		for (int i = 0; i < ((numberLocations*numberCards)-1); i++) {
			stringBuilder += " 2";
		}
		stringBuilder += "\n";
		return stringBuilder;
	}
	
	public String initializeVariables() {
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
				//all constraints have been entered
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
	
	public String oneEachLocation() {
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
	
	public String caseFiles() {
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
	
	public String accusationConstraints() {
		List<List<Integer>> accusations = data.getAccusationConstraints();
		int numberLocations = data.getNumberPlayers() + 3;

		String stringBuilder = "";
		for (int i = 0; i < accusations.size(); i++) {
			int player = accusations.get(i).get(0);
			int suspect = accusations.get(i).get(1);
			int place = accusations.get(i).get(2);
			int weapon = accusations.get(i).get(3);
//			stringBuilder += "3 " + (suspect*numberLocations+player) + " "
//					+ (place*numberLocations+player) + " " + (weapon*numberLocations+player)
//					+ " " + "0 1\n";
//			stringBuilder += "0 0 0 -\n";
			//NEW
			double playerTrust = data.getTrust().get(player);

			double defaultWeight = this.probToWeight(playerTrust);
			double truthWeight = this.probToWeight((1-playerTrust)/7);
			String defaultW = String.format("%.5g%n", defaultWeight).replace("\n", "");
			String truthW = String.format("%.5g%n", truthWeight).replace("\n", "");; 
			if (truthWeight == -1) {
				truthW = "-";
			}
			int var1 = suspect*numberLocations+player;
			int var2 = place*numberLocations+player;
			int var3 = weapon*numberLocations+player;
			stringBuilder += this.createTuplesHeader(defaultW, 3, 1, var1, var2, var3);
			stringBuilder += "0 0 0 " + truthW + "\n";

			
			if (playerTrust != 1) {
				this.globalMax += Math.max(defaultWeight, truthWeight);
			}
			//END NEW
			constraintCounter++;
		}
		
		return stringBuilder;
	}
	
	/*
	 * Private helper functions
	 */
	
	private String createTuplesHeader(String defaultValue, int numberVars, int deviatingTuples, int... vars) {
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
	
	public static void main(String[] args) {
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
//		System.out.println(test2.getInputString());
//		System.out.println(test2.globalMax);
	}
	
}
