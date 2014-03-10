package model;

import static util.Utils.read;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import processor.MultiRoleIndexr;
import structure.Document;

public class MultiRoleTlda {
    private MultiRoleIndexr indexer;
    
    
    private int[][] documents; // document data (term lists)
    private int V; // vocabulary size
    private int[] RC; // semantic role count
    private String[] roleNames;
    private int K; // number of frames
    private int R; // number of roles

    private double alpha; // Dirichlet parameter (document--frame associations)
    private double[] betas; // frame -- role realizations

    private int[][][] r; // frame assignments for each role.
    private int[][][] nr; // number of instances of role i assigned to frame j
    private int[][][] ndr; // number of roles in document i assigned to frame j
    private int[][] nrsum; // total number of roles assigned to frame j
    private int[][] ndrsum; // total number of roles in document i

    private double[][] thetasum; // cumulative statistics of theta, delta, gamma
    private double[][][] phisum; // cumulative statistics of phi
    private int numstats; // size of statistics
    private double[][] theta;
    private double[][][] phi;

    private int THIN_INTERVAL = 20; // sampling lag (?)
    private int BURN_IN = 100; // burn-in period
    private int ITERATIONS = 1000; // max iterations
    private int SAMPLE_LAG; // sample lag (if -1 only one sample taken)

    public MultiRoleTlda(List<Document> docs, int roleNum, String[] roleNames) throws IOException {
        this.R = roleNum;
        this.indexer = new MultiRoleIndexr();
        this.documents = indexer.doIndex(docs, roleNum);
        this.V = indexer.getVocabularySize();
        this.RC = new int[roleNum];
        this.roleNames = roleNames;
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
        phi = new double[R][K][V];
        theta = new double[M][K];
        // initialise count variables.
        nr = new int[R][V][K];
        ndr = new int[R][M][K];
        nrsum = new int[R][K];
        ndrsum = new int[R][M];

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
        if (SAMPLE_LAG > 0) {
            for (int m = 0; m < documents.length; m++) 
                for (int k = 0; k < K; k++) 
                    theta[m][k] = thetasum[m][k] / numstats;
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
        if (SAMPLE_LAG > 0) {
            for (int j = 0; j < R; j++) 
                for (int k = 0; k < K; k++) 
                    for (int w = 0; w < V; w++)
                        phi[j][k][w] = phisum[j][k][w] / numstats;
        } else {
            for (int j = 0; j < R; j++) 
                for (int k = 0; k < K; k++) 
                    for (int w = 0; w < V; w++) 
                        phi[j][k][w] = (nr[j][w][k] + betas[j]) / (nrsum[j][k] + RC[j] * betas[j]);
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

    public void printDocFrameDist(PrintWriter pw) {
        double[][] theta = getTheta();
        for (int d = 0; d < documents.length; d++) {
            pw.append("Document" + d + "\n");
            for (int k = 0; k < K; k++) {
                pw.append("**Frame " + k + ": ");
                pw.append(String.valueOf(theta[d][k]));
                pw.append("\n");
            }
            pw.append("\n\n");
        }
    }
    
    public void printFrameRoleDist(PrintWriter pw, double theshold) {
        double[][][] phi = getPhi();
        for (int k = 0; k < K; k++) {
            pw.append("=== Frame " + k + " ===\n");
            for (int j = 0; j < R; j++) {
                pw.append("=>Role type: " + roleNames[j] + "\n");
                for (int w = 0; w < V; w++) {
                    if (phi[j][k][w] >= theshold)
                        pw.append(indexer.index2Word(w) + ": " + phi[j][k][w] + "\n");
                }
            }
            pw.append("\n\n");
        }
    }

    public void printFrameAssign(PrintWriter pw) {
        for (int j = 0; j < r.length; j++) {
            pw.append("======== " + roleNames[j] + " ========\n");
            for (int m = 0; m < r[j].length; m++) {
                for (int n = j; n < r[j][m].length; n+=R) {
                    if (n % 10 == 0)
                        pw.append("\n");
                    pw.append(documents[m][n]+"("+indexer.index2Word(documents[m][n])
                            +"):"+r[j][m][n] + " ");
                }
            }
            pw.append("\n\n");
        }
    }
    
            
    public void printFrame(PrintWriter pw, int topK, double theashold) {
        double[][][] phi = getPhi();
        for (int k = 0; k < K; k++) {
            pw.append("=== Frame " + k + " ===\n");
            for (int j = 0; j < R; j++) {
                pw.append("=>Role type: " + roleNames[j] + "\n");
                Map<String, Double> rolesDist = new TreeMap<String, Double>();
                for (int w = 0; w < V; w++) {
                    rolesDist.put(indexer.index2Word(w), phi[j][k][w]);
                }
                Set<Entry<String, Double>> entries = rolesDist.entrySet();
                List<Entry<String, Double>> lists = new ArrayList<Entry<String, Double>>(entries);
                final Comparator<Entry<String, Double>> COMPARE_BY_VALUE = 
                        new Comparator<Entry<String, Double>>() {
                            @Override
                            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                                return -o1.getValue().compareTo(o2.getValue());
                            }
                        };
                Collections.sort(lists, COMPARE_BY_VALUE);
                for (int z = 0; z < topK && z < lists.size(); z++) {
                    if (lists.get(z).getValue() >= theashold) {
                        pw.append(lists.get(z).getKey());
                        pw.append(":" + lists.get(z).getValue() + "\n");
                    }
                }
                pw.append("\n");
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
        final String dir = "./data/cpa/";
        List<Document> docs = new ArrayList<Document>();
        File dirFile = new File(dir);
        String[] data = dirFile.list(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                return arg1.endsWith(".dat");
            }
        });
        for (String docLabel : data) {
            docs.add(new Document(docLabel, read(dir + docLabel)));
        }

        System.out.println("Tuple Latent Dirichlet Allocation "
                + "using Gibbs Sampling.");

        int K = 800; // Frame number
        int R = 3; // role number
        String[] roleNames = new String[R];
        roleNames[0] = "subject";
        roleNames[1] = "predicate";
        roleNames[2] = "object";
        double alpha = 50/K; // good values
        double[] betas = new double[R];
        Arrays.fill(betas, 0.01);
        
        MultiRoleTlda lda = new MultiRoleTlda(docs, R, roleNames);
        int iterations = 1000;
        int burnIn = 500;
        int thinInterval = 5;
        int sampleLag = 5;
        lda.configure(iterations, burnIn, thinInterval, sampleLag);
        lda.gibbs(K, alpha, betas);

        PrintWriter fassign = new PrintWriter(dir+"model.fassign");
        PrintWriter docFrameDist = new PrintWriter(dir+"model.dfd");
        PrintWriter frameRoleDist = new PrintWriter(dir+"model.frd");
        PrintWriter frame = new PrintWriter(dir+"model.frame");
        
        int topK = 20;
        lda.printFrameAssign(fassign);
        lda.printDocFrameDist(docFrameDist);
        double theshold = 0.001;
        lda.printFrameRoleDist(frameRoleDist, theshold);
        lda.printFrame(frame, topK, theshold);
        
        fassign.close();
        docFrameDist.close();
        frameRoleDist.close();
        frame.close();
    }

}
