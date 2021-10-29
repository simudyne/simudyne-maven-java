package org.example.models.tokyo;

import java.util.Arrays;

public class SFGenerator {

    int[][] adjacencyMatrix;

    public void generateSF(int smallBanks, int largeBanks) {

        int totalBanks = smallBanks + largeBanks;
        adjacencyMatrix = new int[totalBanks][totalBanks];

        // Step 1
        // fully connect the Large Banks
        for (int i = 0; i < largeBanks; i++) {
            for (int j = 0; j < largeBanks; j++) {
                if (i != j) makeEdge(i, j, 1);
            }
        }

        // Step 2
        // iteratively add the Smaller Banks
        double p;
        int links = 10;

        for (int i = 0; i < largeBanks; i++) {
            for (int j = largeBanks; j < totalBanks; j++) {
                int edgeSum = sumEdges(totalBanks, totalBanks, adjacencyMatrix);
                p = (double) links / (2 * edgeSum);
//                if (getContext().getPrng().uniform(0.0, 1.0).sample() < p) {
//                    makeEdge(i, j, 1);
                }
            }
        }

//        System.out.println(Arrays.deepToString(adjacencyMatrix));
//    }

    public void makeEdge(int from, int to, int edge) {
        adjacencyMatrix[from][to] = edge;
    }

    public int sumEdges(int x, int y, int[][] adjacencyMatrix) {
        int sum = 0;
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                if (adjacencyMatrix[i][j] == 1) {
                    sum += 1;
                }
            }
        }
        return sum;
    }
}