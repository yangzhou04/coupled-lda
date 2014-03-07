package model;

import static util.Utils.read;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import processor.MultiRoleIndexr;
import structure.Document;

public class MultiRoleTlda {

    private int[][] documents; // document data (term lists)

    private MultiRoleIndexr indexer;
    private int V; // vocabulary size
    private int[] RC; // semantic role count
  

    private int K; // number of frames
    private int R; // number of roles

    private double alpha; // Dirichlet parameter (document--frame associations)
//    private double beta;  
    private double[] betas; // frame -- role realizations
//    private double delta;
//    private double gamma;

    /**
     * frame assignments for each word.
     */
    // private int[][] z;
    
//    // frame assignments for each subject.
//    private int[][] s;
//    // frame assignments for each predicate.
//    private int[][] p;
//    // frame assignments for each object.
//    private int[][] o;
    private int[][][] r; // frame assignments for each role.

    /**
     * cwt[i][j] number of instances of word i (term?) assigned to frame j.
     */
//    private int[][] nw;
//    // number of instances of subject i assigned to frame j
//    private int[][] ns;
//    // number of instances of predicate i assigned to frame j
//    private int[][] np;
//    // number of instances of object i assigned to frame j
//    private int[][] no;
    
    private int[][][] nr; // number of instances of role i assigned to frame j

    /**
     * na[i][j] number of words in document i assigned to frame j.
     */
//    private int[][] nd;
//    // number of subjects in document i assigned to frame j.
//    private int[][] nds;
//    // number of predicates in document i assigned to frame j.
//    private int[][] ndp;
//    // number of objects in document i assigned to frame j.
//    private int[][] ndo;
    private int[][][] ndr; // number of roles in document i assigned to frame j

    /**
     * nwsum[j] total number of words assigned to frame j.
     */
//    private int[] nwsum;
//    // total number of subjects assigned to frame j
//    private int[] nssum;
//    // total number of predicates assigned to frame j
//    private int[] npsum;
//    // total number of objects assigned to frame j
//    private int[] nosum;
    private int[][] nrsum; // total number of roles assigned to frame j

    /**
     * nasum[i] total number of words in document i.
     */
//    private int[] ndsum;
//    // total number of subjects in document i.
//    private int[] ndssum;
//    // total number of predicates in document i.
//    private int[] ndpsum;
//    // total number of objects in document i.
//    private int[] ndosum;
    private int[][] ndrsum; // total number of roles in document i

    /**
     * cumulative statistics of theta, delta, gamma
     */
    private double[][] thetasum;

    /**
     * cumulative statistics of phi
     */
//    private double[][] phisum;
//    private double[][] zetasum;
//    private double[][] psisum;
    private double[][][] phisum;

    /**
     * size of statistics
     */
    private int numstats;

    /**
     * sampling lag (?)
     */
    private int THIN_INTERVAL = 20;

    /**
     * burn-in period
     */
    private int BURN_IN = 100;

    /**
     * max iterations
     */
    private int ITERATIONS = 1000;

    /**
     * sample lag (if -1 only one sample taken)
     */
    private int SAMPLE_LAG;

    // private static int dispcol = 0;

    public MultiRoleTlda(List<Document> docs, int roleNum) throws IOException {
        this.R = roleNum;
        this.indexer = new MultiRoleIndexr();
        this.documents = indexer.doIndex(docs, roleNum);
        this.V = indexer.getVocabularySize();
        this.RC = new int[roleNum];
        for (int i = 0; i < R; i++)
            this.RC[i] = indexer.getRoleCount(i);
    }

    /**
     * Initialisation: Must start with an assignment of observations to frames ?
     * Many alternatives are possible, I chose to perform random assignments
     * with equal probabilities
     * 
     * @param K
     *            number of frames
     * @return z assignment of frames to words
     */
    public void initialState(int K) {
        int M = documents.length;

        // initialise count variables.
//        ns = new int[V][K];
//        np = new int[V][K];
//        no = new int[V][K];
//        nds = new int[M][K];
//        ndp = new int[M][K];
//        ndo = new int[M][K];
//        nssum = new int[K];
//        npsum = new int[K];
//        nosum = new int[K];
//        ndssum = new int[M];
//        ndpsum = new int[M];
//        ndosum = new int[M];
        nr = new int[R][V][K];
        ndr = new int[R][M][K];
        nrsum = new int[R][K];
        ndrsum = new int[R][M];

//        s = new int[M][];
//        p = new int[M][];
//        o = new int[M][];
        r = new int[R][M][];
        for (int m = 0; m < M; m++) {
            int N = documents[m].length / 3;
            
            for (int j = 0; j < R; j++) {
                r[j][m] = new int[N];
                // total number of words in document i
                ndrsum[j][m] = N;
            }
            
            for (int n = 0; n < N; n++) {
                int f = (int) (Math.random() * K);
                for (int j = 0; j < R; j++) {
                    // assign frame to each role
                    r[j][m][n] = f;
                    // number of instances of each role assigned to frame
                    nr[j][documents[m][n*R+j]][f]++;
                    // number of each role in document m assigned to frame f
                    ndr[j][m][f]++;
                    // total number of each role assigned to frame
                    nrsum[j][f]++;
                }
            }
        }
    }
    

    /**
     * Main method: Select initial state ? Repeat a large number of times: 1.
     * Select an element 2. Update conditional on other elements. If
     * appropriate, output summary for each run.
     * 
     * @param K
     *            number of frames
     * @param alpha
     *            symmetric prior parameter on document--frame associations
     * @param beta
     *            symmetric prior parameter on frame--term associations
     */
    private void gibbs(int K, double alpha, double[] betas) {
        this.K = K;
        this.alpha = alpha;
        this.betas = new double[betas.length];
        System.arraycopy(betas, 0, this.betas, 0, betas.length);

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][K];
            phisum = new double[R][K][V];
            numstats = 0;
        }

        // initial state of the Markov chain:
        initialState(K);

        System.out.println("Sampling " + ITERATIONS
                + " iterations with burn-in of " + BURN_IN + " (B/S="
                + THIN_INTERVAL + ").");

        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println("Iteration: " + i);

            // sample frame for subjects
            for (int j = 0; j < r.length; j++)
                for (int m = 0; m < r[j].length; m++)
                    for (int n = 0; n < r[j][m].length; n++)
                        r[j][m][n] = sampleRoleFullConditional(j, m, n);

            // get statistics after burn-in
            if ((i > BURN_IN) && (SAMPLE_LAG > 0) && (i % SAMPLE_LAG == 0)) {
                updateParams();
            }
        }
    }
    
    // j  role type
    // m document index
    // n word index
    private int sampleRoleFullConditional(int j, int m, int n) {
        // remove f_i from the count variables
        int f = r[j][m][n];
        nr[j][documents[m][n*R+j]][f]--; // mapping position
        ndr[j][m][f]--;
        nrsum[j][f]--;
        ndrsum[j][m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            double framerole = 1;
            for (int i = 0; i < R; i++) {
                framerole *= (nr[i][documents[m][n*R+i]][k] + betas[i]) / (nrsum[i][k] + RC[i]*betas[i]);
            }
            int ndrcount = 0, ndrsumcount = 0;
            for (int i = 0; i < R; i++) {
                ndrcount += ndr[i][m][k];
                ndrsumcount += ndrsum[i][m];
            }
            double docframe = (ndrcount + alpha) / (ndrsumcount + K*alpha);
            p[k] = docframe * framerole;
//            p[k] = (no[documents[m][n*3+2]][k] + beta) / (nosum[k] + O * beta)
//                * (ns[documents[m][n*3]][k] + gamma) / (nosum[k] + S * gamma)
//                * (np[documents[m][n*3+1]][k] + delta) / (npsum[k] + P * delta)
//* (nds[m][k]+ndp[m][k]+ndo[m][k] + alpha) / (ndssum[m]+ndpsum[m]+ndosum[m] + K * alpha);
        }
        // cumulate multinomial parameters
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        // scaled sample because of unnormalised p[]
        double u = Math.random() * p[K - 1];
        for (f = 0; f < p.length; f++) {
            if (u < p[f])
                break;
        }

        // add newly estimated z_i to count variables
        nr[j][documents[m][n*R+j]][f]++; // mapping position
        ndr[j][m][f]++;
        nrsum[j][f]++;
        ndrsum[j][m]++;

        return f;
    }


    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    private void updateParams() {
        for (int m = 0; m < documents.length; m++) {
            for (int k = 0; k < K; k++) {
                int ndrcount = 0, ndrsumcount = 0;
                for (int j = 0; j < R; j++) {
                    ndrcount += ndr[j][m][k];
                    ndrsumcount += ndrsum[j][m];
//                    thetasum[m][k] += (nds[m][k]+ndp[m][k]+ndo[m][k] + alpha) 
//                            / (ndssum[m]+ndpsum[m]+ndosum[m] + K * alpha);
                }
                thetasum[m][k] += (ndrcount+alpha) / (ndrsumcount + K*alpha);
            }
        }
        for (int k = 0; k < K; k++) {
            for (int w = 0; w < V; w++) {
                for (int j = 0; j < R; j++) {
                    phisum[j][k][w] += (nr[j][w][k] + betas[j]) / (nrsum[j][k] + RC[j] * betas[j]);
                }
//                phisum[k][w] += (no[w][k] + beta) / (nosum[k] + O * beta);
//                zetasum[k][w] += (np[w][k] + delta) / (npsum[k] + P * delta);
//                psisum[k][w] += (ns[w][k] + gamma) / (nssum[k] + S * gamma);
            }
        }
        numstats++;
    }

    /**
     * Retrieve estimated document--frame associations. If sample lag > 0 then
     * the mean value of all sampled statistics for theta[][] is taken.
     * 
     * @return theta multinomial mixture of document frames (M x K)
     */
    public double[][] getTheta() {
        double[][] theta = new double[documents.length][K];

        if (SAMPLE_LAG > 0) {
            for (int m = 0; m < documents.length; m++) {
                for (int k = 0; k < K; k++) {
                    theta[m][k] = thetasum[m][k] / numstats;
                }
            }
        } else {
            for (int m = 0; m < documents.length; m++) {
                for (int k = 0; k < K; k++) {
                    int ndrcount = 0, ndrsumcount = 0;
                    for (int j = 0; j < R; j++) {
                        ndrcount += ndr[j][m][k];
                        ndrsumcount += ndrsum[j][m];
                    }
//                    theta[m][k] = (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
                    theta[m][k] = (ndrcount + alpha) / (ndrsumcount + K * alpha);
                }
            }
        }

        return theta;
    }
    
    

    /**
     * Retrieve estimated frame--word associations. If sample lag > 0 then the
     * mean value of all sampled statistics for phi[][] is taken.
     * 
     * @return phi multinomial mixture of frame words (K x V)
     */
    public double[][][] getPhi() {
        double[][][] phi = new double[R][K][V];
        if (SAMPLE_LAG > 0) {
            for (int j = 0; j < R; j++) {
                for (int k = 0; k < K; k++) {
                    for (int w = 0; w < V; w++) {
                        phi[j][k][w] = phisum[j][k][w] / numstats;
                    }
//                    phi[k][w] = phisum[k][w] / numstats;
                }
            }
        } else {
            for (int j = 0; j < R; j++) {
                for (int k = 0; k < K; k++) {
                    for (int w = 0; w < V; w++) {
                        phi[j][k][w] = (nr[j][w][k] + betas[j]) / (nrsum[j][k] + RC[j] * betas[j]);
                    }
//                        phi[k][w] = (nw[w][k] + beta) / (nwsum[k] + V * beta);
                }
            }
        }
        return phi;
    }

    /**
     * Configure the gibbs sampler
     * 
     * @param iterations
     *            number of total iterations
     * @param burnIn
     *            number of burn-in iterations
     * @param thinInterval
     *            update statistics interval
     * @param sampleLag
     *            sample interval (-1 for just one sample at the end)
     */
    public void configure(int iterations, int burnIn, int thinInterval,
            int sampleLag) {
        this.ITERATIONS = iterations;
        this.BURN_IN = burnIn;
        this.THIN_INTERVAL = thinInterval;
        this.SAMPLE_LAG = sampleLag;
    }

    public void printDistributions(PrintWriter pw) {
        double[][] theta = getTheta();
        double[][][] phi = getPhi();
//        double[][] zeta = getZeta();
//        double[][] psi = getPsi();

        pw.append("=========== Document-Frame distribution============\n");
        for (int d = 0; d < documents.length; d++) {
            pw.append("Document" + d + "\n");
            for (int k = 0; k < K; k++) {
                pw.append("**Frame " + k + ": ");
                pw.append(String.valueOf(theta[d][k]));
                pw.append("\n");
            }
            pw.append("\n\n");
        }

        pw.append("===========Frame-SR distribution============\n");
        double hold = 0.01;
        for (int k = 0; k < K; k++) {
            pw.append("***Frame " + k + "***\n");
            
            for (int j = 0; j < R; j++) {
                pw.append("----Role type: " + j + "\n");
                for (int w = 0; w < V; w++) {
                    if (phi[j][k][w] < hold)
                        pw.append("\t");
                    pw.append(indexer.index2Word(w) + ": " + phi[j][k][w] + "\n");
                }
            }

            pw.append("\n\n");
        }

    }

    /**
     * Driver with example data.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Read tuple "./data/muc34-tuple/";
        final String dir = "./data/artificial/";
        List<Document> docs = new ArrayList<Document>();
        File dirFile = new File(dir);
        for (String docLabel : dirFile.list()) {
            docs.add(new Document(docLabel, read(dir + docLabel)));
        }

        System.out.println("Tuple Latent Dirichlet Allocation "
                + "using Gibbs Sampling.");

        
        int K = 4; // Frame number
        int R = 3; // role number
        String[] roleNames = new String[R];
        roleNames[0] = "subject";
        roleNames[1] = "predicate";
        roleNames[2] = "object";
        double alpha = 0.5; // good values
        double[] betas = new double[R];
        Arrays.fill(betas, 0.05);
        
        MultiRoleTlda lda = new MultiRoleTlda(docs, R);
        int iterations = 10000;
        int burnIn = 5000;
        int thinInterval = 10;
        int sampleLag = 10;
        lda.configure(iterations, burnIn, thinInterval, sampleLag);
        lda.gibbs(K, alpha, betas);

        PrintWriter pw = new PrintWriter("./data/reconstruct.txt");
        lda.printDistributions(pw);
//        lda.printTuple(pw);
        pw.close();
    }

}
