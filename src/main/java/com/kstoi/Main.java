package com.kstoi;

import com.kstoi.trees.DecTree;
import com.kstoi.trees.DecisionTree;
import com.kstoi.trees.PrunedDecisionTree;
import com.kstoi.trees.TreeNode;
import com.kstoi.utils.ArffReader;
import com.kstoi.utils.Dataset;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;

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
            log.info(" Decision Tree results");
            resultNotPrunedHalfDataset.forEach((string,object)->{
                log.info("{} -> {}",string,object.toString());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static <Node extends TreeNode> Map<String,Object> testTree(DecTree<Node> tree, Dataset testDataset) {
            var node = tree.build();

        try {
            getTreeImage(node,tree.toString()+"_data_"+testDataset.getData().size());
        } catch (IOException e) {
           e.printStackTrace();
        }

        double truePositives=0,trueNegatives=0,falsePositives=0,falseNegatives=0;
            for (Dataset.Line line : testDataset.getData()){
                String prediction = tree.predict(node,line);
                System.out.println(line);
                log.info("line is labeled as {} -> predicted {}",line.get("match"),prediction);
                if(prediction!=null)
                    if(Objects.equals(line.get("match"), prediction) && prediction.equals("1")){
                       truePositives++;
                    } else if (Objects.equals(line.get("match"), prediction) && prediction.equals("0")) {
                        trueNegatives++;
                    }else if(!Objects.equals(line.get("match"), prediction) && prediction.equals("1")){
                        falsePositives++;
                    }else if (!Objects.equals(line.get("match"), prediction) && prediction.equals("1")) {
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

    public static void getTreeImage(TreeNode node,String filename) throws IOException {
        Image image = new BufferedImage(17000,2160,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,17000,2160);
        g.setFont(new Font("Arial Black",Font.PLAIN,12));
        node.forEach(treeNode->{
            g.setColor(Color.BLACK);
            var point = (Point)treeNode.get("point");
            var previous = (Point) treeNode.get("parent");
            if (previous != null){
                g.drawLine(previous.x, previous.y, point.x, point.y);
            }
            if((boolean) treeNode.get("isLeaf")){
                g.drawString((String) treeNode.get("label"),point.x,point.y);
            }else{
                g.drawString(((String) treeNode.get("attr")), point.x, point.y);
            }
        },8500,100,0,0);
        g.dispose();
        ImageIO.write((RenderedImage) image,"png",new File("src/main/resources/"+filename+".png"));
    }
}