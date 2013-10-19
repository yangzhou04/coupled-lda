package model;

/*
 * (C) Copyright 2005, Gregor Heinrich (gregor :: arbylon : net) (This file is
 * part of the org.knowceans experimental software packages.)
 */
/*
 * LdaGibbsSampler is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 */
/*
 * LdaGibbsSampler is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Mar 6, 2005
 */

import static util.Utils.read;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import processor.IndexProcessor;
import structure.Document;
import structure.SemanticRole;
import flag.SemanticRoleType;

/**
 * Modified from :Gibbs sampler for estimating the best assignments of topics
 * for words and documents in a corpus. The algorithm is introduced in Tom
 * Griffiths' paper
 * "Gibbs sampling in the generative model of Latent Dirichlet Allocation"
 * (2002). origin author: Gregor Heinrich
 * 
 * @author zhouyang
 */

public class Tlda {

    /**
     * document data (term lists)
     */
    private int[][] documents;

    private IndexProcessor indexConventer;

    /**
     * vocabulary size
     */
    private int V;
    /**
     * Subject count
     */
    // private int SC;
    /**
     * Object count
     */
    // private int OC;
    /**
     * Verb count
     */
    // private int VC;

    /**
     * number of topics
     */
    private int K;
    /**
     * Dirichlet parameter (document--topic associations)
     */
    private double alpha;
    /**
     * Dirichlet parameter (topic--term associations)
     */
    private double beta;

    /**
     * topic assignments for each word.
     */
    private int z[][];

    /**
     * cwt[i][j] number of instances of word i (term?) assigned to topic j.
     */
    private int[][] nw;

    /**
     * na[i][j] number of words in document i assigned to topic j.
     */
    private int[][] nd;

    /**
     * nwsum[j] total number of words assigned to topic j.
     */
    private int[] nwsum;

    /**
     * nasum[i] total number of words in document i.
     */
    private int[] ndsum;

    /**
     * cumulative statistics of theta
     */
    private double[][] thetasum;

    /**
     * cumulative statistics of phi
     */
    private double[][] phisum;

    /**
     * size of statistics
     */
    private int numstats;

    /**
     * sampling lag (?)
     */
    private static int THIN_INTERVAL = 20;

    /**
     * burn-in period
     */
    private static int BURN_IN = 100;

    /**
     * max iterations
     */
    private static int ITERATIONS = 1000;

    /**
     * sample lag (if -1 only one sample taken)
     */
    private static int SAMPLE_LAG;

    // private static int dispcol = 0;

    public Tlda(List<Document> docs) throws IOException {

        this.indexConventer = IndexProcessor.getInstance();
        int[][] indexedDocs = indexConventer.mapping(docs);
        int V = indexConventer.getVocabulary();
        // int WC = ic.getWordCount();
        // int SC = ic.getSubjectCount();
        // int OC = ic.getObjectCount();
        // int VC = ic.getVerbCount();

        this.documents = indexedDocs;
        this.V = V;
        // this.SC = SC;
        // this.VC = VC;
        // this.OC = OC;
    }

    /**
     * Initialisation: Must start with an assignment of observations to topics ?
     * Many alternatives are possible, I chose to perform random assignments
     * with equal probabilities
     * 
     * @param K
     *            number of topics
     * @return z assignment of topics to words
     */
    public void initialState(int K) {
        int M = documents.length;

        // initialise count variables.
        nw = new int[V][K];
        nd = new int[M][K];
        nwsum = new int[K];
        ndsum = new int[M];

        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.

        z = new int[M][];
        for (int m = 0; m < M; m++) {
            int N = documents[m].length;
            z[m] = new int[N];
            for (int n = 0; n < N; n++) {
                if (indexConventer.getNullIndex() == documents[m][n]) { // ignore
                                                                        // null
                                                                        // role
                    continue;
                }

                int topic = (int) (Math.random() * K);
                z[m][n] = topic;
                // number of instances of word i assigned to topic j
                nw[documents[m][n]][topic]++;
                // number of words in document i assigned to topic j.
                nd[m][topic]++;
                // total number of words assigned to topic j.
                nwsum[topic]++;
            }
            // total number of words in document i
            ndsum[m] = N;
        }
    }

    /**
     * Main method: Select initial state ? Repeat a large number of times: 1.
     * Select an element 2. Update conditional on other elements. If
     * appropriate, output summary for each run.
     * 
     * @param K
     *            number of topics
     * @param alpha
     *            symmetric prior parameter on document--topic associations
     * @param beta
     *            symmetric prior parameter on topic--term associations
     */
    private void gibbs(int K, double alpha, double beta) {
        this.K = K;
        this.alpha = alpha;
        this.beta = beta;

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][K];
            phisum = new double[K][V];
            numstats = 0;
        }

        // initial state of the Markov chain:
        initialState(K);

        System.out.println("Sampling " + ITERATIONS
                + " iterations with burn-in of " + BURN_IN + " (B/S="
                + THIN_INTERVAL + ").");

        for (int i = 0; i < ITERATIONS; i++) {

            // for all z_i
            for (int m = 0; m < z.length; m++) {
                for (int n = 0; n < z[m].length; n++) {
                    if (indexConventer.getNullIndex() == documents[m][n]) {
                        continue;
                    }
                    // (z_i = z[m][n])
                    // sample from p(z_i|z_-i, w)
                    int topic = sampleFullConditional(m, n);
                    z[m][n] = topic;
                }
            }

            /*
             * if ((i < BURN_IN) && (i % THIN_INTERVAL == 0)) {
             * System.out.print("B"); dispcol++; } // display progress if ((i >
             * BURN_IN) && (i % THIN_INTERVAL == 0)) { System.out.print("S");
             * dispcol++; }
             */
            // get statistics after burn-in
            if ((i > BURN_IN) && (SAMPLE_LAG > 0) && (i % SAMPLE_LAG == 0)) {
                updateParams();
                // System.out.print("|");
                // if (i % THIN_INTERVAL != 0)
                // dispcol++;
            }
            /*
             * if (dispcol >= 100) { System.out.println(); dispcol = 0; }
             */
        }
    }

    /**
     * Sample a topic z_i from the full conditional distribution: p(z_i = j |
     * z_-i, w) = (n_-i,j(w_i) + beta)/(n_-i,j(.) + W * beta) * (n_-i,j(d_i) +
     * alpha)/(n_-i,.(d_i) + K * alpha)
     * 
     * @param m
     *            document
     * @param n
     *            word
     */
    private int sampleFullConditional(int m, int n) {

        // remove z_i from the count variables
        int topic = z[m][n];
        nw[documents[m][n]][topic]--;
        nd[m][topic]--;
        nwsum[topic]--;
        ndsum[m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            p[k] = (nw[documents[m][n]][k] + beta) / (nwsum[k] + V * beta)
                    * (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
        }
        // cumulate multinomial parameters
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        // scaled sample because of unnormalised p[]
        double u = Math.random() * p[K - 1];
        for (topic = 0; topic < p.length; topic++) {
            if (u < p[topic])
                break;
        }

        // add newly estimated z_i to count variables
        nw[documents[m][n]][topic]++;
        nd[m][topic]++;
        nwsum[topic]++;
        ndsum[m]++;

        return topic;
    }

    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    private void updateParams() {
        for (int m = 0; m < documents.length; m++) {
            for (int k = 0; k < K; k++) {
                thetasum[m][k] += (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
            }
        }
        for (int k = 0; k < K; k++) {
            for (int w = 0; w < V; w++) {
                phisum[k][w] += (nw[w][k] + beta) / (nwsum[k] + V * beta);
            }
        }
        numstats++;
    }

    /**
     * Retrieve estimated document--topic associations. If sample lag > 0 then
     * the mean value of all sampled statistics for theta[][] is taken.
     * 
     * @return theta multinomial mixture of document topics (M x K)
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
     * Retrieve estimated topic--word associations. If sample lag > 0 then the
     * mean value of all sampled statistics for phi[][] is taken.
     * 
     * @return phi multinomial mixture of topic words (K x V)
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

    /**
     * Print table of multinomial data
     * 
     * @param data
     *            vector of evidence
     * @param fmax
     *            max frequency in display
     * @return the scaled histogram bin values
     */
    public static void hist(double[] data, int fmax) {

        double[] hist = new double[data.length];
        // scale maximum
        double hmax = 0;
        for (int i = 0; i < data.length; i++) {
            hmax = Math.max(data[i], hmax);
        }
        double shrink = fmax / hmax;
        for (int i = 0; i < data.length; i++) {
            hist[i] = shrink * data[i];
        }

        NumberFormat nf = new DecimalFormat("00");
        String scale = "";
        for (int i = 1; i < fmax / 10 + 1; i++) {
            scale += "    .    " + i % 10;
        }

        System.out.println("x" + nf.format(hmax / fmax) + "\t0" + scale);
        for (int i = 0; i < hist.length; i++) {
            System.out.print(i + "\t|");
            for (int j = 0; j < Math.round(hist[i]); j++) {
                if ((j + 1) % 10 == 0)
                    System.out.print("]");
                else
                    System.out.print("|");
            }
            System.out.println();
        }
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
        ITERATIONS = iterations;
        BURN_IN = burnIn;
        THIN_INTERVAL = thinInterval;
        SAMPLE_LAG = sampleLag;
    }

    public void printTuple() {
        double[][] phi = getPhi();
        SemanticRoleType roleType;
        String word;
        double value;
        SemanticRole role;
        for (int k = 0; k < K; k++) {
            System.out.println("Frame " + k);
            List<SemanticRole> subjects = new LinkedList<SemanticRole>();
            List<SemanticRole> verbs = new LinkedList<SemanticRole>();
            List<SemanticRole> objects = new LinkedList<SemanticRole>();
            for (int w = 0; w < V; w++) {
                roleType = indexConventer.getRoleType(w);
                word = indexConventer.index2Word(w);
                value = phi[k][w];
                role = new SemanticRole(roleType, word, value);
                if (roleType == SemanticRoleType.SUBJECT) {
                    subjects.add(role);
                } else if (roleType == SemanticRoleType.PREDICATE) {
                    verbs.add(role);
                } else {
                    objects.add(role);
                }
                // System.out.println(w + ": " + ic.index2Word(w) + ": "
                // + phi[k][w] + ": " + role);
            }
            Collections.sort(subjects);
            Collections.sort(verbs);
            Collections.sort(objects);

            System.out.println("Subject:");
            printSemanticRole(subjects);
            System.out.println("Verb:");
            printSemanticRole(verbs);
            System.out.println("Object:");
            printSemanticRole(objects);
        }
    }

    private void printSemanticRole(List<SemanticRole> role) {
        for (int i = 0; i < role.size(); i++) {
            System.out.print(role.get(i).getWord() + "  ");
        }
        System.out.println();
    }

    public void printFrame() {
        double[][] theta = getTheta();
        double[][] phi = getPhi();

        for (int d = 0; d < documents.length; d++) {
            for (int k = 0; k < K; k++) {
                System.out.print("Document" + d + " containing frame:" + k
                        + ": ");
                System.out.println(theta[d][k]);
            }
        }

        for (int k = 0; k < K; k++) {
            System.out.println("Frame " + k);
            for (int w = 0; w < V; w++) {
                System.out.println(w + ": " + indexConventer.index2Word(w)
                        + ": " + phi[k][w] + ": "
                        + indexConventer.getRoleType(w));
            }
        }
    }

    /**
     * Driver with example data.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Read tuple
        final String dir = "data/tuples/";
        List<Document> docs = new ArrayList<Document>();
        File f = new File(dir);
        for (String docLabel : f.list()) {
            docs.add(new Document(docLabel, read(dir + docLabel)));
        }

        System.out.println("Tuple Latent Dirichlet Allocation "
                + "using Gibbs Sampling.");

        Tlda lda = new Tlda(docs);
        int iterations = 1000000;
        int burnIn = 20000;
        int thinInterval = 100;
        int sampleLag = 10;
        lda.configure(iterations, burnIn, thinInterval, sampleLag);

        int K = 13; // Frame number
        double alpha = 2; // good values
        double beta = .5; // good values
        lda.gibbs(K, alpha, beta);

        // double[][] theta = lda.getTheta();
        // double[][] phi = lda.getPhi();

        // System.out.println();
        // System.out.println();
        // System.out.println("Document--Topic Associations, Theta[d][k] (alpha="
        // + alpha + ")");
        // System.out.print("d\\k\t");
        // for (int m = 0; m < theta[0].length; m++) {
        // System.out.print("   " + m % 10 + "    ");
        // }
        // System.out.println();
        // for (int m = 0; m < theta.length; m++) {
        // System.out.print(m + "\t");
        // for (int k = 0; k < theta[m].length; k++) {
        // System.out.print(theta[m][k] + " ");
        // // System.out.print(shadeDouble(theta[m][k], 1) + " ");
        // }
        // System.out.println();
        // }
        // System.out.println();
        // System.out.println("Topic--Term Associations, Phi[k][w] (beta=" +
        // beta
        // + ")");
        //
        // System.out.print("k\\w\t");
        // for (int w = 0; w < phi[0].length; w++) {
        // System.out.print("   " + w + "    ");
        // }
        // System.out.println();
        // for (int k = 0; k < phi.length; k++) {
        // System.out.print(k + "\t");
        // for (int w = 0; w < phi[k].length; w++) {
        // System.out.print(phi[k][w] + " ");
        // // System.out.print(shadeDouble(phi[k][w], 1) + " ");
        // }
        // System.out.println();
        // }

        lda.printFrame();
        lda.printTuple();
    }

}
