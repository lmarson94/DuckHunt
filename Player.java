import java.util.ArrayList;

class Player {
	
	public static final Action cDontShoot = new Action(-1, -1);
	//species variables
	ArrayList<ArrayList<Duck>> species = new ArrayList<ArrayList<Duck>>();
	ArrayList<ArrayList<Duck>> patterns = new ArrayList<ArrayList<Duck>>();
	
	ArrayList<ArrayList<Duck>> groups = null;
	
	//ducks per round
	Duck[] ducks = null;
	//counter for turns
	int turnsCounter=0;
	//flag to signal new round
	boolean newRound = true;
	
	//constants
	int startShooting = 85; //ultimo turno prima di sparare
	int maxGuess = 20; //numero massimo di guess al primo round
	int minScore = 1; //minimo punteggio che il shootmodel deve avere per essere preso in considerazione per sparare
	double threshold = -6;
	double threshold2 = -0;
	double threshold3 = Double.NEGATIVE_INFINITY;
	double threshold4 = 0;
	
    public Player() {
    	for(int i = 0; i < 6; i++) {
			species.add(new ArrayList<Duck>());
		}
    }
    /**
     * Shoot!
     *
     * This is the function where you start your work.
     *
     * You will receive a variable pState, which contains information about all
     * birds, both dead and alive. Each bird contains all past moves.
     *
     * The state also contains the scores for all players and the number of
     * time steps elapsed since the last time this function was called.
     *
     * @param pState the GameState object with observations etc
     * @param pDue time before which we must have returned
     * @return the prediction of a bird we want to shoot at, or cDontShoot to pass
     */
    public Action shoot(GameState pState, Deadline pDue) {
    	int i, j, obs=-1;
    	
    	//first turn: print round number, allocate vector of birds and give each of them observation at time t = 0
    	if(newRound) {
    		System.err.println("round " + pState.getRound());
    		ducks = new Duck[pState.getNumBirds()];
    		for(i = 0; i < pState.getNumBirds(); i++) {
    			ducks[i] = new Duck(pState.getBird(i).getObservation(0), i);
    		}
    		newRound = false;
    	}
    	
    	if(turnsCounter < startShooting) {
    		for(i = 0; i < ducks.length; i++) {
        		obs = pState.getBird(i).getLastObservation();
        		if(obs != -1) {
	        		for(j = 0; j < ducks[i].getN(); j++) {
	        			ducks[i].models[j].nextMoveProbability(obs);
	        		}
	        		ducks[i].addObs(obs);
	        		ducks[i].createModel(turnsCounter+1);
	        		ducks[i].createShootingModel(turnsCounter+1);
        		}
        	}
    		turnsCounter++;
    		return shootingDecision(pState);
    	}
    	else if(turnsCounter == startShooting){
    		for(i = 0; i < ducks.length; i++) {
        		obs = pState.getBird(i).getLastObservation();
        		if(obs != -1) {
	        		ducks[i].addObs(obs);
	        		ducks[i].createShootingModel(turnsCounter+1);
        		}
        	}
    		turnsCounter++;
    		return shootingDecision(pState);
    	} else {
    		for(i = 0; i < ducks.length; i++) {
        		obs = pState.getBird(i).getLastObservation();
        		if(obs != -1) {
        			ducks[i].shootmodel.nextMoveProbability(obs);
        			ducks[i].addObs(obs);
        			ducks[i].createShootingModel(turnsCounter+1);
        		}
        	}
    		turnsCounter++;
    		return shootingDecision(pState);
    	}
		
        // This line chooses not to shoot.
        //return cDontShoot;
    }
    
    /**
     * This method decides if to shoot and at which bird and where
     * find the model with the strongest prediction among all models of all birds still alive
     * 
     * @param pState
     * @return an Action object with the most probable move of a bird and the bird itself
     */
    public Action shootingDecision(GameState pState) {
    	double max=0;
    	int bird=-1, move=-1, i, s, u, j;
    	boolean safeSpecies;
    	int[] obs = new int[startShooting+1];
    	int length=0;
    	
    	for(i = 0; i < ducks.length; i++) {
    	    if(pState.getRound() < 3)
    	        safeSpecies = true;
    	    else
    		    safeSpecies = false;
    		length = 0;
    		for(j = 0; j < startShooting+1; j++) {
    			if(ducks[i].lastObs < j)
    				j = startShooting+1;
    			else {
    				obs[j] = ducks[i].observations[j];
    				length++;
    			}
    		}
    		for(s = 0; s < species.size()-1; s++) {
    			for(u = 0; u < species.get(s).size(); u++) {
    				for(int k=0; k<ducks[i].getN(); k++) {
    					double p = species.get(s).get(u).topModel.sequenceProbability(obs, length+1) - ducks[i].models[k].sequenceProbability(obs, length+1);
    					if(p > threshold) {
    						safeSpecies = true;
    					}
    				}
    			}
    		}
    		for(u = 0; u < species.get(5).size(); u++) {
    			for(int k=0; k<ducks[i].getN(); k++) {
    				double p = species.get(5).get(u).topModel.sequenceProbability(obs, length+1) - ducks[i].models[k].sequenceProbability(obs, length+1);
    				if(p > threshold2) {
    					safeSpecies = false;
    				}
    			}
			}
			
    		if(pState.getBird(i).isAlive() && ducks[i].shootmodel.nextMove() != null && safeSpecies) {
    			if(max < ducks[i].shootmodel.nextMove()[1] && ducks[i].shootmodel.modelScore >= 1) { //minscore
    				max = ducks[i].shootmodel.nextMove()[1];
    				bird = i;
    				move = (int) ducks[i].shootmodel.nextMove()[0];
    			}
    		}
    	}
    	
    	return new Action(bird, move);
    }

    
    
    /**
     * Guess the species!
     * This function will be called at the end of each round, to give you
     * a chance to identify the species of the birds for extra points.
     *
     * Fill the vector with guesses for the all birds.
     * Use SPECIES_UNKNOWN to avoid guessing.
     *
     * @param pState the GameState object with observations etc
     * @param pDue time before which we must have returned
     * @return a vector with guesses for all the birds
     */
    public int[] guess(GameState pState, Deadline pDue) {
    	double prob = 0.0;
    	int guess = -1, i, j, s, u, length=0;
        int[] lGuess = new int[pState.getNumBirds()];
        int[] obs = new int [startShooting+1];
        
    	newRound = true;
    	turnsCounter = 0;
    	
    	//at the end of each round I choose the best model for each bird based on the score they have hit
    	for(i = 0; i < pState.getNumBirds(); i++) {
    		ducks[i].discardModels();
    	}
    	
//    	//per ogni uccello i da indovinare, stampa numero uccello + sua sequenceProb
//        //		per ogni uccello registrato, stampa specie e sequence probability con osservazioni di i 
//    	for(i=0; i<pState.getNumBirds(); i++) {
//    		length=0;
//    		for(j = 0; j < startShooting+1; j++) {
//                if(ducks[i].lastObs < j)
//                    j = startShooting+1;
//                else {
//                    obs[j] = ducks[i].observations[j];
//                    length++;
//                }
//            }
//    		System.err.println(ducks[i].id + " " + ducks[i].topModel.sequenceProbability(obs, length+1));
//    		for(s=0; s<species.size(); s++) {
//    			System.err.println("Specie: " + s);
//    			for(u=0; u<species.get(s).size(); u++) { 
//        			double p = species.get(s).get(u).topModel.sequenceProbability(obs, length+1) - ducks[i].topModel.sequenceProbability(obs, length+1);
//    				System.err.printf( "%.4f ", Math.pow(10, p));
//    			}
//    			System.err.println();
//    		}
//    		System.err.println();
//    		System.err.println();
//    	}
    	
    	
    
    	
    	
    	for(i = 0; i < pState.getNumBirds(); i++) {
    		lGuess[i] = Constants.SPECIES_UNKNOWN;
    	}
    	
        for (i = 0; i < pState.getNumBirds(); ++i) { //per tutti gli uccelli da indovinare
        	if(pState.getRound() > 0) {
        		double max = Double.NEGATIVE_INFINITY;
        		for(s=0; s<species.size(); s++) { //per tutte le specie
                    for(u=0; u<species.get(s).size(); u++) { //per tutti gli uccelli (già registrati) di una specie
                        length = 0;
                        for(j = 0; j < startShooting+1; j++) {
                            if(ducks[i].lastObs < j)
                                j = startShooting+1;
                            else {
                                obs[j] = ducks[i].observations[j];
                                length++;
                            }
                        }
                        
                        prob = species.get(s).get(u).topModel.sequenceProbability(obs, length+1);
                        if(max < prob) {
                            max = prob;
                            guess = s;
                        }
                    }
                }
        		
        		s = -1;
        		for(j = 0; j < species.size(); j++) {
        			if(species.get(j).size() == 0) {
        				s = j;
    					j += 6;
        			}
        		}
        		
        		double p = max - ducks[i].topModel.sequenceProbability(obs, length+1);
        		if(p < threshold3 && s != -1) {
        			lGuess[i] = s;
        		} else {
        		
        			lGuess[i] = guess;
        		}
        	} else {
        		group();
        		for(j = 0; j < groups.size(); j++) {
        			lGuess[groups.get(j).get(0).id] = Constants.SPECIES_PIGEON;
        		}
        		//lGuess[i] = Constants.SPECIES_RAVEN;
        		if(i == maxGuess)
        			i = 20;
            }
        }
        
        return lGuess;
        
    }
    
    public void group() {
    	double max, p;
    	int index, length;
    	int[] obs = new int[startShooting+1];
    	
    	groups = new ArrayList<ArrayList<Duck>>();
    	
    	for(int i = 0; i < ducks.length; i++) {
    		index = -1;
    		max = Math.log10(0);
    		for(int g = 0; g < groups.size(); g++) {
    			for(int u = 0; u < groups.get(g).size(); u++) {
    				length = 0;
    				for(int j = 0; j < startShooting+1; j++) {
    					if(ducks[i].lastObs < j)
    	    				j = startShooting+1;
    	    			else {
    	    				obs[j] = ducks[i].observations[j];
    	    				length++;
    	    			}
    				}
    				p = groups.get(g).get(u).topModel.sequenceProbability(obs, length) - ducks[i].topModel.sequenceProbability(obs, length+1);
    				
    				if( p > threshold4 && p > max ) {
    					index = g;
    					max = p;
    				}
    			}
    		}
    		if(index == -1) {
    			groups.add(new ArrayList<Duck>());
    			groups.get(groups.size()-1).add(ducks[i]);
    		} else {
    			groups.get(index).add(ducks[i]);
    		}
    	}
    }
    
    /**
     * If you hit the bird you were trying to shoot, you will be notified
     * through this function.
     *
     * @param pState the GameState object with observations etc
     * @param pBird the bird you hit
     * @param pDue time before which we must have returned
     */
    public void hit(GameState pState, int pBird, Deadline pDue) {
        System.err.println("HIT BIRD!!!");
    }

    /**
     * If you made any guesses, you will find out the true species of those
     * birds through this function.
     *
     * @param pState the GameState object with observations etc
     * @param pSpecies the vector with species
     * @param pDue time before which we must have returned
     */
    
    public void reveal(GameState pState, int[] pSpecies, Deadline pDue) {
    	int i;
    	
    	for(i=0; i<pSpecies.length; i++) {
    		if(pSpecies[i] != -1)
    			species.get(pSpecies[i]).add(ducks[i]);
    	}
    }
    
}
