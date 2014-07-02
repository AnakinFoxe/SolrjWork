/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.csupomona.ir.solrapplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author xing
 */
public class Searcher {
    
    private final Pattern ptnEnglish = Pattern.compile("^[a-zA-Z]{1,20}$");
    
    private HashMap<String, Integer> dict;  // record unigrams and counts
    
    public Searcher() {
        dict = new HashMap<String, Integer>();
        
        Stopword.init();
    }

    /*
    * Update count for the word in hashmap
    * @param map The word:count mapping HashMap
    * @param word The word for updating
    * @return Nothing
    */
    private void updateHashMap(HashMap<String, Integer> map, String word) {
        if (map.containsKey(word)) 
            map.put(word, map.get(word)+1);
        else
            map.put(word, 1);
    }
    
    /*
    * Trim the hashmap dict by removing less count words
    * @param Nothing
    * @return Nothing
    */
    private void trimDict() {
        // put dict into a treemap and sort the treemap
        Map<String, Integer> tmp = new TreeMap<String, Integer>();
        tmp.putAll(dict);
        tmp = MapUtil.sortByValue(tmp);
        
        // remove everything after "num" counts.
        Integer num = 40;
        for (String key : tmp.keySet()) {
            System.out.println(key + ":" + tmp.get(key).toString());
            num--;
            if (num <= 0)
                dict.remove(key);
        }
    }
    
    /*
    * This method will parse the text by breaking text into words 
    * and remove stopword, non-English word. Finally it will return 
    * a word:count mapping as term-frequency recording for the text.
    * @param text  The input string text
    * @return HashMap<String, Integer> the word:count mapping
    */
    public HashMap<String, Integer> parseText(String text) {
        HashMap<String, Integer> term_frequency = new HashMap<String, Integer>();

        // update hashmap for each word
        String[] words = text.split(" ");
        for (String word : words) {
            word = word.toLowerCase();  // unify to lowercase

            // skip obvious non-English words and stopword
            Matcher mthEnglish = ptnEnglish.matcher(word);
            if (!mthEnglish.find() || Stopword.isStopword(word)) 
                continue;

            updateHashMap(dict, word);
            updateHashMap(term_frequency, word);
        }
        
        return term_frequency;
    }
    
    public void write(List tf_list, List title_list, String filename) 
            throws IOException{        
        // trim the dict to remove words with too few counts
        trimDict();
        
        // generate sparse results of term frequency for each document
        FileWriter fw = new FileWriter(filename, false);
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator<HashMap<String, Integer>> it_tf = tf_list.iterator();
        Iterator<String> it_title = title_list.iterator();
        while (it_tf.hasNext()) {
            // for each document
            HashMap<String, Integer> tf = it_tf.next();
            for (String key : dict.keySet()) {
                // get the value for the key
                String value = "0";
                if (tf.containsKey(key))
                    value = tf.get(key).toString();
                
                // write the value and separate with a comma
                bw.write(value + ",");
            }
            bw.write(it_title.next() + "\n");
        }
        bw.close();
    }

    
    public static void main(String[] args) 
            throws IOException, MalformedURLException, SolrServerException {
        HttpSolrServer solr = new HttpSolrServer("http://localhost:8983/solr");

        SolrQuery param = new SolrQuery();
        param.set("q", "tech");
        param.set("rows", "1000");
    //    query.addFilterQuery("cat:electronics","store:amazon.com");
    //    query.setFields("id","price","merchant","cat","store");
    //    param.setStart(0);    
    //    param.set("defType", "edismax");
        
        Searcher sch = new Searcher();

        QueryResponse response = solr.query(param);
        SolrDocumentList results = response.getResults();
        
        // parse the result text and obtain term-frequency mapping
        List tf_list = new ArrayList();
        List title_list = new ArrayList();
        for (int i = 0; i < results.size(); ++i) {
//            System.out.println(results.get(i).get("content").toString());
            tf_list.add(sch.parseText(results.get(i).get("content").toString()));
            title_list.add(results.get(i).get("title").toString());
        }
        
        // write the most significant term-frequency into file
        sch.write(tf_list, title_list, "data.txt");
        
        // load the file into Dataset
        Dataset data = FileHandler.loadDataset(new File("data.txt"), 39, ",");
        
        // create an instance of kMeans algorithm
        // 5 clusters, 50 iterations
        Clusterer km = new KMeans(5, 100);
        
        // cluster the data, it will be returned as an array of data sets, 
        // with each dataset representing a cluster
        Dataset[] clusters = km.cluster(data);
        
        System.out.println("Cluster count: " + clusters.length);
        for (Dataset cluster : clusters) {
            System.out.println("cluster size: " + cluster.size());
            for (Instance item :cluster) {
                System.out.println(item.toString());
            }
        }
        
        
        
        System.out.println("total number of results: " + results.size());
        
        
    }
    
}
