/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.csupomona.ir.solrapplication;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/**
 *
 * @author Xing
 */
public class Stopword {
    private static HashSet<String> stopwords;
	
    public static void init() {
        try {
            FileReader swFile = new FileReader("stopwords.txt");
            BufferedReader swReader = new BufferedReader(swFile);
            String sw;

            stopwords = new HashSet<String>();
            while((sw = swReader.readLine()) != null){
                stopwords.add(sw.replaceAll("\\s+", ""));
            }
            swFile.close();
        } catch (IOException e) {
                System.out.println("Can not open file");
        }

    }

    public static boolean isStopword(String word) {
        return stopwords.contains(word.replaceAll("\\s+", "").toLowerCase());
    }
}
