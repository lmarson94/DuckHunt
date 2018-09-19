
public class Lambda {
	int N, M, T;
	double[][] a  = null;
	double[][] b = null;
	double[] pi = null;
	int[] o = null;
	double[][] alpha = null;
	double[][] alphac = null;
	double[][] beta = null;
	double[][] gamma = null;
	double[][][] digamma = null;
	double[] c = null, ct;
	int modelScore=0;
	final static double thresholdP = 0.39; //minima probabilità che la previsione deve avere per sparare
	
	public Lambda(int N, int score) {
		modelScore = score;
		this.N = N;
		M = 9;
		this.o = new int[100];
		a = new double[][] {{0.8, 0.05, 0.05, 0.05, 0.05},
			{0.075, 0.7, 0.075, 0.075, 0.075},
			{0.075, 0.075, 0.7, 0.075, 0.075},
			{0.075, 0.075, 0.075, 0.7, 0.075},
			{0.075, 0.075, 0.075, 0.075, 0.7}};
		b = new double[][] {{0.125, 0.125, 0.125, 0.125, 0.0, 0.125, 0.125, 0.125, 0.125},
			{0.36, 0.04, 0.36, 0.04, 0.04, 0.04, 0.04, 0.04, 0.04},
			{0.016, 0.016, 0.016, 0.225, 0.02, 0.225, 0.016, 0.45, 0.016},
			{0.15, 0.15, 0.15, 0.04, 0.02, 0.04, 0.15, 0.15, 0.15},
			{0.1125, 0.1125, 0.1125, 0.1125, 0.1, 0.1125, 0.1125, 0.1125, 0.1125}};
		pi = new double[N];
		double sum = 0.0;
		for(int i = 0; i < N-1; i++) {
			pi[i] = 1.0/N+(Math.random()-0.5)/100;
		}
		pi[N-1] = 1.0 - sum;
		alpha = new double[100][N];
		beta = new double[100][N];
		gamma = new double[100][N];
		digamma = new double[100][N][N];
		c = new double[100];
		ct = new double[100];
	}
	
	public Lambda(int N, int score, boolean flag) {
		this.modelScore = score;
		this.N = N;
		M = 9;
		this.o = new int[100];
		a = new double[N][N];
		b = new double[N][M];
		pi = new double[N];
		alpha = new double[100][N];
		beta = new double[100][N];
		gamma = new double[100][N];
		digamma = new double[100][N][N];
		c = new double[100];
		ct = new double[100];
		randomModel();
	}
	
	public void randomModel() {
		int i, j;
		double sum, sum2=0;
		
		for(i = 0; i < N; i++) {
			sum = 0.0;
			for(j = 0; j < N-1; j++) {
				a[i][j] = 1.0/N+(Math.random()-0.5)/100;
				sum += a[i][j];
			}
			a[i][N-1] = 1-sum;
			sum = 0;
			for(j = 0; j < M-1; j++) {
				b[i][j] = 1.0/M+(Math.random()-0.5)/100;
				sum += b[i][j];
			}
			b[i][M-1] = 1-sum;
			pi[i] = 1.0/N+(Math.random()-0.5)/100;
			sum2 += pi[i];
		}
		sum2 -= pi[N-1];
		pi[N-1] = 1-sum2;
	}
	
	//based on the collected past observation train the HMM model
	public void trainModel(int[] obs, int T) {
		double oldProbabilitySequence=10.0, probabilitySequence=20.0;
		int counter = 0;
		
		for(int i = 0; i < T; i++) {
			o[i] = obs[i];
		}
		this.T = T;
		
		while(Math.abs(probabilitySequence-oldProbabilitySequence) > 0.001 && counter < 100) { 
			createAlpha();
			createBeta();
			createGammaAndDigamma();
			
			reestimateModel();
			
			oldProbabilitySequence = probabilitySequence;
			probabilitySequence = observationProbability(); 

			counter++;
		}
	}
	
	//try to figure out which is the next most probable observation and if it's bigger than a certain threashold
	//if correct increase score else decrease it
	public void nextMoveProbability(int o) {
		double p=0, max = 0;
		int move=-1;
		
		for(int k = 0; k < Constants.COUNT_MOVE; k++) {
			p = 0;
			for(int i = 0; i < N; i++) {
				for(int j = 0; j < N; j++) {
					p += alpha[T-1][j]*a[j][i]*b[i][k];
				}
			}
			if(max < p) {
				max = p;
				move = k;
			}
		}
		
		if(max > thresholdP && move == o) {
			modelScore++;
		} else if (max > thresholdP && move != o){
			modelScore--;
		}
	}
	
	//return most probable move and its probability if bigger than a threshold else return null
	public double[] nextMove() {
		double p, max = 0;
		int move=-1;
		
		for(int k = 0; k < Constants.COUNT_MOVE; k++) {
			p = 0;
			for(int i = 0; i < N; i++) {
				for(int j = 0; j < N; j++) {
					p += alpha[T-1][j]*a[j][i]*b[i][k];
				}
			}
			if(max < p) {
				max = p;
				move = k;
			}
		}
		
		if(max > thresholdP) {
			return new double[] {move, max};
		}
		
		return null;
	}
	
	//alpha pass
	public void createAlpha() {
		int i, j, t;
		
		c[0] = 0;
		// alpha for t = 0 and scaling
		for(i = 0; i < N; i++) {
			alpha[0][i] = pi[i]*b[i][o[0]];
			c[0] += alpha[0][i];
		}
		c[0] = 1/c[0];
		for(i = 0; i < N; i++) {
			alpha[0][i] = alpha[0][i]*c[0];
		}
		// alpha for 0 < t < T
		for(t = 1; t < T; t++) {
			c[t] = 0;
			for(i = 0; i < N; i++) {
				alpha[t][i] = 0;
				for(j = 0; j < N; j++) {
					alpha[t][i] += alpha[t-1][j]*a[j][i];
				}
				alpha[t][i] *= b[i][o[t]];
				c[t] += alpha[t][i];
			}
			c[t] = 1/c[t];
			//scaling
			for(i = 0; i < N; i++) {
				alpha[t][i] = alpha[t][i]*c[t];
			}
		}
	}
	
	//beta pass
	public void createBeta() {
		int i, j, t;
		
		// beta for t = T - 1
		for(i = 0; i < N; i++) {
			beta[T-1][i] = c[T-1];
		}
		// beta for 0 <= t < T - 1
		for(t = T-2; t >= 0; t--) {
			for(i = 0; i < N; i++) {
				beta[t][i] = 0;
				for(j = 0; j < N; j++) {
					beta[t][i] += a[i][j]*b[j][o[t+1]]*beta[t+1][j];
				}
				beta[t][i] = beta[t][i]*c[t];
			}
		}
	}
	
	//gamma and digamma
	public void createGammaAndDigamma() {
		double alphaSum = 0, denominator;
		int i, j, t;
		
		for(t = 0; t < T-1; t++) {
			denominator = 0;
			for(i = 0; i < N; i++) {
				for(j = 0; j < N; j++) {
					denominator += alpha[t][i]*a[i][j]*b[j][o[t+1]]*beta[t+1][j];
				}
			}
			for(i = 0; i < N; i++) {
				gamma[t][i] = 0;
				for(j = 0; j < N; j++) {
					digamma[t][i][j] = alpha[t][i]*a[i][j]*b[j][o[t+1]]*beta[t+1][j]/denominator;
					gamma[t][i] += digamma[t][i][j];
				}
			}
		}
		for(i = 0; i < N; i++) {
			alphaSum += alpha[T-1][i];
		}
		for(i = 0; i < N; i++) {
			gamma[T-1][i] = alpha[T-1][i]/alphaSum;
		}
	}
	
	//re-estimate model's parameters
	public void reestimateModel() {
		double sumDigamma, sumGamma, sumGammaO;
		int i, j, t;
		
		for(i = 0; i < N; i++) {
			// a
			for(j = 0; j < N; j++) {
				sumDigamma = 0;
				sumGamma = 0;
				for(t = 0; t < T-1; t++) {
					sumDigamma += digamma[t][i][j];
					sumGamma += gamma[t][i];
				}
				a[i][j] = sumDigamma/sumGamma;
			}
			//b
			for(j = 0; j < M; j++) {
				sumGamma = 0;
				sumGammaO = 0;
				for(t = 0; t < T; t++) {
					if(o[t] == j) {
						sumGammaO += gamma[t][i];
					}
					sumGamma += gamma[t][i];
				}
				b[i][j] = sumGammaO/sumGamma;
			}
			//pi
			pi[i] = gamma[0][i];
		}
	}
	
	//return what's the logarithm in base 10 of the probability of observing the all sequence
	public double observationProbability() {
		double p = 0;
		int t;
		
		for(t = 0; t < T; t++) {
			p += Math.log10(c[t]);
		}
		return p*(-1);
	}
	
	//return the probability of observing a sequence of observations given this model
	public double sequenceProbability(int[] o, int length) {
	 	alphac = new double[2][N];
		int i, j, t;

		ct[0] = 0;
		// alpha for t = 0 and scaling
		for(i = 0; i < N; i++) {
			alphac[0][i] = pi[i]*b[i][o[0]];
			ct[0] += alphac[0][i];
		}
		ct[0] = 1/ct[0];
	
		for(i = 0; i < N; i++) {
			alphac[0][i] = alphac[0][i]*ct[0];
		}
		// alpha for 0 < t < T
		for(t = 1; t < length-1; t++) {
			ct[t] = 0;
			for(i = 0; i < N; i++) {
				alphac[1][i] = 0;
				for(j = 0; j < N; j++) {
					alphac[1][i] += alphac[0][j]*a[j][i];
				}
				alphac[1][i] *= b[i][o[t]];
				ct[t] += alphac[1][i];
			}
			
			ct[t] = 1/ct[t];
			//scaling
			for(i = 0; i < N; i++) {
				alphac[0][i] = alphac[1][i]*ct[t];
			}
		}
		
		double p = 0.0;
		for(t = 0; t < T; t++) {
			p += Math.log10(ct[t]);
		}
		
		return -1*p;	
	}
}
