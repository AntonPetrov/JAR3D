package edu.bgsu.rna.jar3d;

/**
 * This class is used to simulate normal basepair nodes
 * @author meg pirrung
 *
 */
public class BasepairNode extends BasicNode {
	
	// these strings are used to print out generated strands
	String leftGen,rightGen;
	// this defines a var to hold which letter pair is chosen for generation
    int[] pair;
    // these character arrays are used with a set of probabilities to determine the pairs generated by this node
    //    Subtract 1 from each of the following numbers:
    //    % 1-AA  2-CA  3-GA  4-UA  5-AC  6-CC  7-GC  8-UC 
    //    % 9-AG 10-CG 11-GG 12-UG 13-AU 14-CU 15-GU 16-UU
    String letAry[][] = new String[][]{{"AA","AC","AG","AU"},{"CA","CC","CG","CU"},{"GA","GC","GG","GU"},{"UA","UC","UG","UU"}};

    char letters[] = new char[]{'A','C','G','U'};
    // these are arrays of probabilities that are used to determine the pairs Generated
    double[][] pairProb;
    double[][] pairLogProb;
    // arrays of logs of all probabilities
    
    // this is the probability that this basePairNode does not generate anything
    double deleteProb;
    double deleteLogProb;
    double notDeleteLogProb;
    
    double[][] maxProb;
    
    InsertionDistribution lInsDist, rInsDist;
    
	/**
	 * This is the constructor for BasepairNodes
	 * @param prev contains a pointer to the previous node
	 * @param dProb is the deletion probability for this node
	 * @param pProb is the pair probability matrix
	 * @param lLenDist is the Left Length distribution matrix
	 * @param lLetDist is the Left Letter distribution matrix
	 * @param rLenDist is the Right Length distribution matrix
	 * @param rLetDist is the Right Letter distribution matrix
	 */
	BasepairNode(Node prev, double dProb, double[][] pProb, double[] lLenDist, double[] lLetDist, double[] rLenDist, double[] rLetDist, int lI, int rI)
	{
		super(prev, "BasepairNode", lI, rI);
		deleteProb = dProb;
		// normalize is a function that makes sure all the array's elements add up to 1
		pairProb = rnaMath.normalize(pProb);
        
        pairLogProb = rnaMath.logd(pairProb);
        deleteLogProb = Math.log(deleteProb);
		notDeleteLogProb = Math.log(1-deleteProb);            // probability of not being deleted

        lInsDist = new InsertionDistribution(lLenDist, lLetDist);
        rInsDist = new InsertionDistribution(rLenDist, rLetDist);
 	}

	
	public String generate(boolean del)
  	{        
		// use the transition probability and del boolean to change
		// current deleteProb?
		if(Math.random() < deleteProb)
		{
			return "(" + super.child.generate(true) + ")"; // using true just as a holder
		}
		else
		{
			pair = rnaMath.randod(pairProb);
			leftGen = letAry[pair[0]][pair[1]].charAt(0) + lInsDist.generate();
			rightGen = rInsDist.generate() + letAry[pair[0]][pair[1]].charAt(1);
			return "(" + leftGen + super.child.generate(true) + rightGen + ")"; // using true as a holder
		}
  	}

   	
	void computeMaxLogProb(Sequence seq, int i, int j)
  	{
  		if ((i >= super.iMin) && (i <= super.iMax) && (j >= super.jMin) && (j <= super.jMax)  && (i <= j)) {
			double p;	// maximum log probability found so far
			double pll;	// contribution to total log prob from left insertion length
			double pli;	// contribution to total log prob from left inserted letters
			double prl; // contribution to total log prob from right insertion length
			double pri; // contribution to total log prob from right inserted letters
			double priarray[] = new double[rInsDist.logLengthDist.length];
			double pnew; // probability of current insertion possibility
			int a; 		// number of insertions on the left
			int b; 		// number of insertions on the right
			int aa=0, bb=0;
			//int[] pc;		// paircode for the letters at i and j
			boolean Deleted;
			
			// consider the possibility that this node generates nothing at all
			
			p = deleteLogProb + super.child.getMaxLogProb(i,j);
			Deleted = true;                          // default, can be changed later

			// consider the possibility of generating a pair and various numbers of insertions
			
			// for loop that sets maxLogProb[i-super.iMin][j-super.jMin]
			
			pli = 0;					// 0 left insertions so far
			
			priarray[0] = 0;
			// this loop sets the probabilities of insertions on the right
			for(b = 1; b < Math.min(j-i-1,rInsDist.lengthDist.length); b++)
  			{
				priarray[b] = priarray[b-1] + rInsDist.logLetterDist[seq.code[j-b]];
  			} // end for loop

			// for loop from 0 to the distance between i and j, or to the length of the left
			// length distribution array, whichever is less
			for(a = 0; a < Math.min(j-i-1,lInsDist.logLengthDist.length); a++)
  	  		{
  				pll = lInsDist.logLengthDist[a];
  				if (a > 0) {
  					pli = pli + lInsDist.logLetterDist[seq.code[i+a]];
  				}
  				pri = 0;				// 0 right insertions so far
  				for(b = 0; b < Math.min(j-i-1-a,rInsDist.lengthDist.length); b++)
  	  			{
  					prl = rInsDist.logLengthDist[b];
  					pri = priarray[b];
					pnew = super.child.getMaxLogProb(i+a+1,j-b-1) + notDeleteLogProb;
					pnew = pnew + pairLogProb[seq.code[i]][seq.code[j]];
					pnew = pnew + pll + pli + prl + pri;
  	  				if (pnew > p) {
  	  					p = pnew;
  	  					aa = a;
  	  					bb = b;
  	  					Deleted = false;
  	  				} // end if
  	  			} // end for loop
  	  		} // end for loop

			maxLogProb[i-iMin][j-jMin] = p;
  			
			if ((p < -99999999) && (!Deleted))
			{
				System.out.println("Basepair node with -Inf prob.  "+i+" "+j+" "+leftIndex+" "+rightIndex);
			}

  			if (Deleted)
  			{
  	  			myGen[i-iMin][j-jMin] = new genData(true);
  			}
  			else
  			{
  				// Should we keep both gendatas here? how to set that up?
  				
  				// LOOK AT THIS! I don't know if I'm getting the pairs out correctly,
  				// I dont know if we ever even determine the max probability for pairs.
  	  			myGen[i-iMin][j-jMin] = new genData(seq.nucleotides.charAt(i),seq.nucleotides.charAt(j),aa,bb);
  			}
  		} // if loop
  	} // method declaration
   	
   	
  	
	public void traceback(int i, int j)
  	{
  		if ((i >= super.iMin) && (i <= super.iMax) && (j >= super.jMin) && (j <= super.jMax)  && (i <= j))
  		{
   			setOptimalAndRelease(i,j);

   			// if setOptimalAndReleased was already done, don't go on to the child!
   			// alreadyDone = setOptimalAndRelease(i,j);
   			
   			if(optimalGen1.deleted)
  			{
  				super.child.traceback(i,j);
  			}
  			else
  			{	  			
	  			int a = optimalGen1.numLeftIns;
	  			int b = optimalGen1.numRightIns;
	  			super.child.traceback(i+1+a,j-1-b); // i+1+a = i +pair+insertions
  			}
  		}
  		else
  		{
  			System.out.println("BasepairNode.traceback: Basepair node out of range");
  			System.out.println("Indices "+leftIndex+" "+rightIndex);
  			System.out.println("Left side:  " + super.iMin + "<=" + i + "<=" + super.iMax);
  			System.out.println("Right side: " + super.jMin + "<=" + j + "<=" + super.jMax);
  			System.out.println("Indices of basepair: " + i + "<=" + j);
  			System.out.println("Deletion probability: " + deleteProb);
  		}
  	}
    	
	public String showParse(String n)
  	{
  		// I think that the one that should be the deleted one to keep is optimalGen2?
  		if (optimalGen1.deleted)
  		{
  			String left = "";
  			for(int f = 0; f < lInsDist.lengthDist.length; f++)
  				left += "-";
  			
  			String right = "";
  			for(int g = 0; g < rInsDist.lengthDist.length; g++)
  				right = "-"+right;
  		
  			return "(" + left + super.child.showParse(n) + right + ")";
  		}
  		else
  		{
  			int a = optimalGen1.numLeftIns;
  			int b = optimalGen1.numRightIns;
  			int i = optimalGen1.i;
  			int j = optimalGen1.j;
  			
  			String left = n.substring(i,i+a+1);
  			int lSize = left.length();
  			for(int f = 0; f < lInsDist.lengthDist.length - lSize; f++)
  				left += "-";
  			
  			String right = n.substring(j-b,j+1);
  			int rSize = right.length();
  			for(int g = 0; g < rInsDist.lengthDist.length - rSize; g++)
  				right = "-"+right;
  			
  			return "(" + left + super.child.showParse(n) + right + ")";
  		}
  	}
  	  	
	public String header()
  	{
		String left = "(";
		for(int f = 0; f < lInsDist.lengthDist.length-1; f++)
			left += "-";
		
		String right = ")";
		for(int g = 0; g < rInsDist.lengthDist.length-1; g++)
			right = "-"+right;
		
		return "(" + left + super.child.header() + right + ")";
  	}

	public String showCorrespondences(String letters)
  	{
  		if (optimalGen1.deleted)
  		{
  			return super.child.showCorrespondences(letters);
  		}
  		else
  		{
  			int a = optimalGen1.numLeftIns;
  			int b = optimalGen1.numRightIns;
  			int i = optimalGen1.i;
  			int j = optimalGen1.j;
  			
  			String left = "SSS_Position_" + (i+1) + "_" + letters.charAt(i) + " JAR3D_aligns_to " + "MMM_Node_" + number + "_Position_1" + "\n";

  			for(int k = i+1; k <= i + a; k++)
  				left += "SSS_Position_" + (k+1) + "_" + letters.charAt(k) + " JAR3D_aligns_to " + "MMM_Node_" + number + "_Position_1_Insertion" + "\n";
  			
  			String right = "SSS_Position_" + (j+1) + "_" + letters.charAt(j) + " JAR3D_aligns_to " + "MMM_Node_" + number + "_Position_2" + "\n";

//  			for(int k = j-1; k >= j-b; k--)
//  				right = "SSS_Position_" + (k+1) + "_" + letters.charAt(k) + " JAR3D_aligns_to " + "MMM_Node_" + number + "_Position_2_Insertion" + "\n" + right;

  			for(int k = j-b; k <= j-1; k++)
  				right += "SSS_Position_" + (k+1) + "_" + letters.charAt(k) + " JAR3D_aligns_to " + "MMM_Node_" + number + "_Position_2_Insertion" + "\n";

  			return left + super.child.showCorrespondences(letters) + right;
  		}
  	}
	
	
	/**
	 * This method loops through all ways to insert letters on left and right
	 * and calculates the total probability of generating the subsequence from i to j
	 * @author Craig Zirbel
	 *
	 */

	void computeTotalProbability(Sequence seq, int i, int j)
  	{
  		if ((i >= super.iMin) && (i <= super.iMax) && (j >= super.jMin) && (j <= super.jMax)  && (i <= j)) {
			double p;	// total probability so far
			double pli;	// contribution to total prob from left inserted letters
			double priarray[] = new double[rInsDist.logLengthDist.length];
			double pnew; // probability of current insertion possibility
			int a; 		// number of insertions on the left
			int b; 		// number of insertions on the right

			p = 0;      // start with zero probability
			
			// consider the possibility of generating a pair and various numbers of insertions
			
			// for loop that sets maxLogProb[i-super.iMin][j-super.jMin]
			
			pli = 1;					// 0 left insertions so far
			
			priarray[0] = 1;            // letter probability when 0 insertions on the right
			// this loop sets the probabilities of insertions on the right
			for(b = 1; b < Math.min(j-i-1,rInsDist.lengthDist.length); b++)
  			{
				priarray[b] = priarray[b-1] * rInsDist.letterDist[seq.code[j-b]];
  			} // end for loop

			// for loop from 0 to the distance between i and j, or to the length of the left
			// length distribution array, whichever is less
			for(a = 0; a < Math.min(j-i-1,lInsDist.lengthDist.length); a++)
  	  		{
  				if (a > 0) {
  					pli *= lInsDist.letterDist[seq.code[i+a]];
  				}
  				for(b = 0; b < Math.min(j-i-1-a,rInsDist.lengthDist.length); b++)
  	  			{
  					pnew = lInsDist.lengthDist[a];
  					pnew *= pli;
					pnew *= pairProb[seq.code[i]][seq.code[j]];     // the pair is always i with j
					pnew *= super.child.getTotalProb(i+a+1,j-b-1);
  					pnew *= rInsDist.lengthDist[b];
  					pnew *= priarray[b];
					p += pnew;
  	  			} // end for loop
  	  		} // end for loop

			p *= (1 - deleteProb);
			
			// consider the possibility that this node generates nothing at all
			
			p += deleteProb * super.child.getTotalProb(i,j);

			totalProb[i-iMin][j-jMin] = p;

  		} // if loop
  	} // method declaration

	
} // class