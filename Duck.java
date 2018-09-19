
public class Duck {
	//every bird has different model that describe it
	static final int MAXSTATI = 1;
	int id;
	
	Lambda[] models = new Lambda[MAXSTATI];
	
	boolean modelsExists = false;
	Lambda topModel = null;
	Lambda shootmodel = null;
	
	//save all observations associated to this bird during to the round
	int[] observations = new int[100];
	int lastObs;

	public Duck(int o, int id) {
		this.id = id;
		observations[0] = o;
		lastObs = 1;
		for(int i = 0; i < MAXSTATI; i++) {
			models[i] = new Lambda(i+1, 0, true); //true
			models[i].trainModel(observations, 1);
		}
		createShootingModel(1);
	}
	
	public int getN() {
		return MAXSTATI;
	}
	 
	public void addObs(int o) {
		observations[lastObs] = o;
		lastObs++;
	}
	
	public void createShootingModel(int t) {
		int tmp;
		
		if(modelsExists) {
			tmp = shootmodel.modelScore;
			shootmodel = new Lambda(5, tmp);
			shootmodel.trainModel(observations, t);
		} else {
			shootmodel = new Lambda(5, 0);
			shootmodel.trainModel(observations, t);
			modelsExists = true;	
		}
	}

	//recreate models for every MAXSTATI and feed them with the new observations
	public void createModel(int t) {
		int i, tmp;

		for(i = 0; i < MAXSTATI; i++) {
			tmp = models[i].modelScore;
			models[i] = new Lambda(i+1, tmp, true); //true
			models[i].trainModel(observations, t);
		}
	}

	
	//find the model among the MAXSTATI models with the highest score
	public void discardModels() {
		double topScore=Math.log10(0);
		int index=-1, i;

		for(i = 0; i < MAXSTATI; i++) {
			if(topScore < models[i].modelScore) {
				topScore = models[i].modelScore;
				index = i;
			}
		}

		if(index != -1) {
			topModel = models[index];
		}
	}
	 
}
