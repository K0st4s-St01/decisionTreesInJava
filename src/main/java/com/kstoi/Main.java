package com.kstoi;

import com.kstoi.trees.DecTree;
import com.kstoi.trees.DecisionTree;
import com.kstoi.trees.PrunedDecisionTree;
import com.kstoi.utils.ArffReader;
import com.kstoi.utils.Dataset;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class Main {
    public static void main(String[] args){
        ArffReader reader = null;
        try {
            reader = new ArffReader(new File("src/main/resources/speeddating.arff"));
            var dataset = reader.loadData();
            var datasets = dataset.split(2);


            var resultPruned = testTree(new PrunedDecisionTree(dataset,4,5,0.01),dataset);
            var resultNotPruned = testTree(new DecisionTree(dataset),dataset);
            var resultNotPrunedHalfDataset = testTree(new DecisionTree(datasets[0]),datasets[1]);
            var resultPrunedHalfDataset = testTree(new PrunedDecisionTree(datasets[0],4,5,0.01),datasets[1]);


            log.info("2. Pruned Decision Tree results");
            resultPruned.forEach((string,object)->{
                log.info("{} -> {}",string,object.toString());
            });
            log.info("3. Decision Tree results");
            resultNotPruned.forEach((string,object)->{
                log.info("{} -> {}",string,object.toString());
            });
            log.info("5.Pruned vs NotPruned split 1/2 dataset");
            log.info("***PRUNED***");
            resultPrunedHalfDataset.forEach((string,object)->{
                log.info("{} -> {}",string,object.toString());
            });
            log.info("***REGULAR***");
            log.info("3. Decision Tree results");
            resultNotPrunedHalfDataset.forEach((string,object)->{
                log.info("{} -> {}",string,object.toString());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static <Node> Map<String,Object> testTree(DecTree<Node> tree, Dataset testDataset) {
            var node = tree.build();
            double truePositives=0,trueNegatives=0,falsePositives=0,falseNegatives=0;
            for (Dataset.Line line : testDataset.getData()){
                String prediction = tree.predict(node,line);
                log.info("line is labeled as {} -> predicted {}",line.get("match {0,1}"),prediction);
                if(prediction!=null)
                    if(Objects.equals(line.get("match {0,1}"), prediction) && prediction.equals("1")){
                       truePositives++;
                    } else if (Objects.equals(line.get("match {0,1}"), prediction) && prediction.equals("0")) {
                        trueNegatives++;
                    }else if(!Objects.equals(line.get("match {0,1}"), prediction) && prediction.equals("1")){
                        falsePositives++;
                    }else if (!Objects.equals(line.get("match {0,1}"), prediction) && prediction.equals("1")) {
                        falseNegatives++;
                    }
            }
            log.info("TP {} TN {} FP {} FN {}",truePositives,trueNegatives,falsePositives,falseNegatives);
            log.info("precision {}",precision(truePositives,falsePositives));
            log.info("recall {}",recall(truePositives,falseNegatives));
            log.info("specificity {}",specificity(trueNegatives,falsePositives));
            log.info("accuracy {}",accuracy(truePositives,trueNegatives,falsePositives,falseNegatives));
            var result = new HashMap<String,Object>();
            result.put("TP {} TN {} FP {} FN {}", truePositives+" "+trueNegatives+" "+falsePositives+" "+falseNegatives);
            result.put("precision {}",precision(truePositives,falsePositives));
            result.put("recall {}",recall(truePositives,falseNegatives));
            result.put("specificity {}",specificity(trueNegatives,falsePositives));
            result.put("accuracy {}",accuracy(truePositives,trueNegatives,falsePositives,falseNegatives));
            result.put("tree",tree.toString());
            return result;
    }
    public static double accuracy(double tp,double tn,double fp,double fn){
        return (tp+tn)/(tp + tn + fp + fn);
    }
    public static double precision(double tp,double fp){
        return tp /(tp+fp);
    }
    public static double recall(double tp,double fn){
        return tp /(tp+fn);
    }
    public static double specificity(double tn,double fp){
        return tn /(tn+fp);
    }

}