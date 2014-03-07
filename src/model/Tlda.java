package model;

import static util.Utils.read;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import processor.Indexr;
import structure.Document;

public class Tlda {

    /**
     * document data (term lists)
     */
    private int[][] documents;

    private Indexr indexer;

    /**
     * vocabulary size
     */
    private int V;
    /**
     * Subject count
     */
    private int S;
    /**
     * Object count
     */
    private int O;
    /**
     * Predicate count
     */
    private int P;

    /**
     * number of frames
     */
    private int K;
    /**
     * Dirichlet parameter (document--frame associations)
     */
    private double alpha;
    /**
     * Dirichlet parameter (frame--subject, predicate, 
     * object associations respectively)
     */
    private double beta;
    private double delta;
    private double gamma;

    /**
     * frame assignments for each word.
     */
    // private int[][] z;
    
    // frame assignments for each subject.
    private int[][] s;
    // frame assignments for each predicate.
    private int[][] p;
    // frame assignments for each object.
    private int[][] o;

    /**
     * cwt[i][j] number of instances of word i (term?) assigned to frame j.
     */
    private int[][] nw;
    // number of instances of subject i assigned to frame j
    private int[][] ns;
    // number of instances of predicate i assigned to frame j
    private int[][] np;
    // number of instances of object i assigned to frame j
    private int[][] no;

    /**
     * na[i][j] number of words in document i assigned to frame j.
     */
    private int[][] nd;
    // number of subjects in document i assigned to frame j.
    private int[][] nds;
    // number of predicates in document i assigned to frame j.
    private int[][] ndp;
    // number of objects in document i assigned to frame j.
    private int[][] ndo;

    /**
     * nwsum[j] total number of words assigned to frame j.
     */
    private int[] nwsum;
    // total number of subjects assigned to frame j
    private int[] nssum;
    // total number of predicates assigned to frame j
    private int[] npsum;
    // total number of objects assigned to frame j
    private int[] nosum;

    /**
     * nasum[i] total number of words in document i.
     */
    private int[] ndsum;
    // total number of subjects in document i.
    private int[] ndssum;
    // total number of predicates in document i.
    private int[] ndpsum;
    // total number of objects in document i.
    private int[] ndosum;

    /**
     * cumulative statistics of theta, delta, gamma
     */
    private double[][] thetasum;

    /**
     * cumulative statistics of phi
     */
    private double[][] phisum;
    private double[][] zetasum;
    private double[][] psisum;

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

    public Tlda(List<Document> docs) throws IOException {
        this.indexer = Indexr.getInstance();
        this.documents = indexer.doIndex(docs);
        this.V = indexer.getVocabularySize();
        this.S = indexer.getSubjectSize();
        this.P = indexer.getPredicateCount();
        this.O = indexer.getObjectCount();
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
//        nw = new int[V][K];
//        nd = new int[M][K];
//        nwsum = new int[K];
//        ndsum = new int[M];
        
        ns = new int[V][K];
        np = new int[V][K];
        no = new int[V][K];
        nds = new int[M][K];
        ndp = new int[M][K];
        ndo = new int[M][K];
        nssum = new int[K];
        npsum = new int[K];
        nosum = new int[K];
        ndssum = new int[M];
        ndpsum = new int[M];
        ndosum = new int[M];

        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.

//        z = new int[M][];
        s = new int[M][];
        p = new int[M][];
        o = new int[M][];
        for (int m = 0; m < M; m++) {
            // every tuple contains 3 elements.
            int N = documents[m].length / 3;
            
//            z[m] = new int[N];
            s[m] = new int[N];
            p[m] = new int[N];
            o[m] = new int[N];
            for (int n = 0; n < N; n++) {
                int f = (int) (Math.random() * K);
                // z[m][n] = frame;
                // number of instances of word i assigned to frame j
//                nw[documents[m][n]][frame]++;
                // number of words in document i assigned to frame j.
//                nd[m][frame]++;
                // total number of words assigned to frame j.
//                nwsum[frame]++;
                
                // assign frame to SPO
                s[m][n] = f;
                p[m][n] = f;
                o[m][n] = f;
                // number of instances of SPOs assigned to frame
                ns[documents[m][n*3]][f]++;
                np[documents[m][n*3+1]][f]++;
                no[documents[m][n*3+2]][f]++;
                // number of SPOs in document m assigned to frame f
                nds[m][f]++;
                ndp[m][f]++;
                ndo[m][f]++;
                // total number of SPOs assigned to frame
                nssum[f]++;
                npsum[f]++;
                nosum[f]++;
            }
            // total number of words in document i
            // ndsum[m] = N;
            ndssum[m] = N;
            ndpsum[m] = N;
            ndosum[m] = N;
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
    private void gibbs(int K, double alpha, double beta, double delta, double gamma) {
        this.K = K;
        this.alpha = alpha;
        this.beta = beta;
        this.delta = delta;
        this.gamma = gamma;

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][K];
            phisum = new double[K][V];
            psisum = new double[K][V];
            zetasum = new double[K][V];
            numstats = 0;
        }

        // initial state of the Markov chain:
        initialState(K);

        System.out.println("Sampling " + ITERATIONS
                + " iterations with burn-in of " + BURN_IN + " (B/S="
                + THIN_INTERVAL + ").");

        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println("Iteration: " + i);
            // the full conditional is the same, but the statistical matrix 
            // used is different, so separate them into 3 function
            
            // sample frame for subjects
            for (int m = 0; m < s.length; m++)
                for (int n = 0; n < s[m].length; n++)
                    s[m][n] = sampleSubjectFullConditional(m, n);
            // sample frame for predicates
            for (int m = 0; m < p.length; m++)
                for (int n = 0; n < p[m].length; n++)
                    p[m][n] = samplePredicateFullConditional(m, n);
            // sample frame for objects
            for (int m = 0; m < o.length; m++)
                for (int n = 0; n < o[m].length; n++)
                    o[m][n] = sampleObjectFullConditional(m, n);

            // get statistics after burn-in
            if ((i > BURN_IN) && (SAMPLE_LAG > 0) && (i % SAMPLE_LAG == 0)) {
                updateParams();
            }
        }
    }

    private int sampleSubjectFullConditional(int m, int n) {
        // remove f_i from the count variables
        int f = s[m][n];
        ns[documents[m][n*3]][f]--; // mapping position
        nds[m][f]--;
        nssum[f]--;
        ndssum[m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            p[k] = (no[documents[m][n*3+2]][k] + beta) / (nosum[k] + O * beta)
                * (ns[documents[m][n*3]][k] + gamma) / (nosum[k] + S * gamma)
                * (np[documents[m][n*3+1]][k] + delta) / (npsum[k] + P * delta)
* (nds[m][k]+ndp[m][k]+ndo[m][k] + alpha) / (ndssum[m]+ndpsum[m]+ndosum[m] + K * alpha);
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
        ns[documents[m][n*3]][f]++; // mapping position
        nds[m][f]++;
        nssum[f]++;
        ndssum[m]++;

        return f;
    }
    
    private int samplePredicateFullConditional(int m, int n) {
        // remove f_i from the count variables
        int f = p[m][n];
        np[documents[m][n*3+1]][f]--; // mapping position
        ndp[m][f]--;
        npsum[f]--;
        ndpsum[m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            p[k] = (no[documents[m][n*3+2]][k] + beta) / (nosum[k] + O * beta)
                * (ns[documents[m][n*3]][k] + gamma) / (nosum[k] + S * gamma)
                * (np[documents[m][n*3+1]][k] + delta) / (npsum[k] + P * delta)
* (nds[m][k]+ndp[m][k]+ndo[m][k] + alpha) / (ndssum[m]+ndpsum[m]+ndosum[m] + K * alpha);
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
        np[documents[m][n*3+1]][f]++; // mapping position
        ndp[m][f]++;
        npsum[f]++;
        ndpsum[m]++;

        return f;
    }
    
    private int sampleObjectFullConditional(int m, int n) {
        // remove f_i from the count variables
        int f = o[m][n];
        no[documents[m][n*3+2]][f]--; // mapping position
        ndo[m][f]--;
        nosum[f]--;
        ndosum[m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            p[k] = (no[documents[m][n*3+2]][k] + beta) / (nosum[k] + O * beta)
                * (ns[documents[m][n*3]][k] + gamma) / (nosum[k] + S * gamma)
                * (np[documents[m][n*3+1]][k] + delta) / (npsum[k] + P * delta)
* (nds[m][k]+ndp[m][k]+ndo[m][k] + alpha) / (ndssum[m]+ndpsum[m]+ndosum[m] + K * alpha);
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
        no[documents[m][n*3+2]][f]++; // mapping position
        ndo[m][f]++;
        nosum[f]++;
        ndosum[m]++;

        return f;
    }

    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    private void updateParams() {
        for (int m = 0; m < documents.length; m++) {
            for (int k = 0; k < K; k++) {
                thetasum[m][k] += (nds[m][k]+ndp[m][k]+ndo[m][k] + alpha) 
                        / (ndssum[m]+ndpsum[m]+ndosum[m] + K * alpha);
            }
        }
        for (int k = 0; k < K; k++) {
            for (int w = 0; w < V; w++) {
                phisum[k][w] += (no[w][k] + beta) / (nosum[k] + O * beta);
                zetasum[k][w] += (np[w][k] + delta) / (npsum[k] + P * delta);
                psisum[k][w] += (ns[w][k] + gamma) / (nssum[k] + S * gamma);
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
                    theta[m][k] = (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
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
    public double[][] getPhi() {
        double[][] phi = new double[K][V];
        if (SAMPLE_LAG > 0) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = phisum[k][w] / numstats;
                }
            }
        } else {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = (nw[w][k] + beta) / (nwsum[k] + V * beta);
                }
            }
        }
        return phi;
    }
    
    public double[][] getZeta() {
        double[][] zeta = new double[K][V];
        if (SAMPLE_LAG > 0) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    zeta[k][w] = zetasum[k][w] / numstats;
                }
            }
        } else {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    zeta[k][w] = (np[w][k] + delta) / (npsum[k] + P * delta);
                }
            }
        }
        return zeta;
    }
    
    public double[][] getPsi() {
        double[][] psi = new double[K][V];
        if (SAMPLE_LAG > 0) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    psi[k][w] = psisum[k][w] / numstats;
                }
            }
        } else {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    psi[k][w] = (ns[w][k] + beta) / (nssum[k] + S * beta);
                }
            }
        }
        return psi;
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
        double[][] phi = getPhi();
        double[][] zeta = getZeta();
        double[][] psi = getPsi();

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
            
            pw.append("**subject**\n");
            for (int w = 0; w < V; w++) {
                if (psi[k][w] < hold)
                    pw.append("\t");
                pw.append(indexer.index2Word(w) + ": " + psi[k][w] + "\n");
            }
            pw.append("**predicate**\n");
            for (int w = 0; w < V; w++) {
                if (zeta[k][w] < hold)
                    pw.append("\t");
                pw.append(indexer.index2Word(w) + ": " + zeta[k][w] + "\n");
            }
            pw.append("**object**\n");
            for (int w = 0; w < V; w++) {
                if (phi[k][w] < hold)
                    pw.append("\t");
                pw.append(indexer.index2Word(w) + ": " + phi[k][w] + "\n");
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

        Tlda lda = new Tlda(docs);
        int iterations = 10000;
        int burnIn = 5000;
        int thinInterval = 10;
        int sampleLag = 10;
        lda.configure(iterations, burnIn, thinInterval, sampleLag);

        int K = 4; // Frame number
        double alpha = 0.5; // good values
        double beta = .05; // good values
        double delta = .05; // good values
        double gamma = .05; // good values
        lda.gibbs(K, alpha, beta, delta, gamma);

        PrintWriter pw = new PrintWriter("./data/reconstruct_simple.txt");
        lda.printDistributions(pw);
//        lda.printTuple(pw);
        pw.close();
    }

}
