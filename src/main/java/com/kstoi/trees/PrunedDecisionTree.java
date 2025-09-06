package com.kstoi.trees;

import com.kstoi.utils.Dataset;
import com.kstoi.utils.TreeMath;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@AllArgsConstructor
@Slf4j
public class PrunedDecisionTree implements DecTree<com.kstoi.trees.PrunedDecisionTree.Node> {

    private Dataset dataset;
    private int maxDepth;
    private int minSamplesSplit;
    private double minGain;

    @Override
    public String toString() {
        return "PrunedDecisionTree{" +
                "maxDepth=" + maxDepth +
                ", minSamplesSplit=" + minSamplesSplit +
                ", minGain=" + minGain +
                '}';
    }

    private List<String> getColumn(List<Dataset.Line> data, String attribute) {
        List<String> column = new ArrayList<>();
        for (Dataset.Line line : data) {
            column.add(line.get(attribute));
        }
        return column;
    }

    private Double entropy(List<String> labels) {
        double entropy = 0d;
        Map<String, Integer> labelFrequency = new HashMap<>();
        for (var label : labels) {
            labelFrequency.put(label, labelFrequency.getOrDefault(label, 0) + 1);
        }
        for (var count : labelFrequency.values()) {
            double probability = (double) count / labels.size();
            entropy += TreeMath.entropy(probability);
        }
        return entropy;
    }

    private Map<String, List<Dataset.Line>> splitDataBasedOnCategory(List<Dataset.Line> data, String attribute) {
        var result = new HashMap<String, List<Dataset.Line>>();
        for (var line : data) {
            String val = line.get(attribute);
            result.computeIfAbsent(val, k -> new ArrayList<>()).add(line);
        }
        return result;
    }

    private String majorityLabel(List<String> labels) {
        Map<String, Integer> counts = new HashMap<>();
        for (String label : labels) {
            counts.put(label, counts.getOrDefault(label, 0) + 1);
        }
        return Collections.max(counts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private Double informationGain(List<Dataset.Line> data, String attribute, String target) {
        double baseEntropy = entropy(getColumn(data, target));
        Map<String, List<Dataset.Line>> subsets = splitDataBasedOnCategory(data, attribute);
        double newEntropy = 0d;
        for (List<Dataset.Line> subset : subsets.values()) {
            double p = (double) subset.size() / data.size();
            newEntropy += p * entropy(getColumn(subset, target));
        }
        return baseEntropy - newEntropy;
    }

    private double[] bestThresholdNumeric(List<Dataset.Line> data, String attribute, String target) {
        List<Dataset.Line> sorted = new ArrayList<>(data);
        sorted.sort(Comparator.comparing(m -> Double.parseDouble(m.get(attribute))));

        double bestGain = 0d;
        double bestThresh = 0d;
        double baseEntropy = entropy(getColumn(data, target));

        for (int i = 1; i < sorted.size(); i++) {
            double prev = Double.parseDouble(sorted.get(i - 1).get(attribute));
            double curr = Double.parseDouble(sorted.get(i).get(attribute));
            if (prev == curr) continue;

            double thresh = (prev + curr) / 2.0;

            List<Dataset.Line> left = new ArrayList<>();
            List<Dataset.Line> right = new ArrayList<>();

            for (Dataset.Line line : sorted) {
                double val = Double.parseDouble(line.get(attribute));
                if (val <= thresh) left.add(line);
                else right.add(line);
            }

            double newEntropy = (left.size() * entropy(getColumn(left, target))
                    + right.size() * entropy(getColumn(right, target))) / data.size();

            double gain = baseEntropy - newEntropy;
            if (gain > bestGain) {
                bestGain = gain;
                bestThresh = thresh;
            }
        }

        return new double[]{bestGain, bestThresh};
    }

    private Node buildTree(List<Dataset.Line> data,
                           List<String> attributes,
                           String targetAttr,
                           int depth,
                           Map<String, Boolean> isNumeric) {
        log.info("Depth {}", depth);

        Node node = new Node();
        List<String> labels = getColumn(data, targetAttr);

        node.label=majorityLabel(labels);

        if (new HashSet<>(labels).size() == 1) {
            node.isLeaf = true;
            node.label = labels.get(0);
            return node;
        }

        if (attributes.isEmpty() || depth >= maxDepth || data.size() < minSamplesSplit) {
            node.isLeaf = true;
            node.label = majorityLabel(labels);
            return node;
        }

        double bestGain = 0.0;
        String bestAttr = null;
        Double bestThreshold = null;
        boolean numeric = false;

        for (String attr : attributes) {
            double gain;
            double thresh = 0;
            if (isNumeric.getOrDefault(attr, false)) {
                double[] res = bestThresholdNumeric(data, attr, targetAttr);
                gain = res[0];
                thresh = res[1];
            } else {
                gain = informationGain(data, attr, targetAttr);
            }

            if (gain > bestGain) {
                bestGain = gain;
                bestAttr = attr;
                if (isNumeric.getOrDefault(attr, false)) {
                    bestThreshold = thresh;
                    numeric = true;
                } else {
                    bestThreshold = null;
                    numeric = false;
                }
            }
        }

        if (bestGain < minGain || bestAttr == null) {
            node.isLeaf = true;
            node.label = majorityLabel(labels);
            return node;
        }

        node.attribute = bestAttr;
        node.threshold = bestThreshold;

        List<String> newAttributes = new ArrayList<>(attributes);
        newAttributes.remove(bestAttr);

        if (numeric) {
            List<Dataset.Line> left = new ArrayList<>();
            List<Dataset.Line> right = new ArrayList<>();
            for (Dataset.Line row : data) {
                double val = Double.parseDouble(row.get(bestAttr));
                if (val <= bestThreshold) left.add(row);
                else right.add(row);
            }
            node.left = buildTree(left, newAttributes, targetAttr, depth + 1, isNumeric);
            node.right = buildTree(right, newAttributes, targetAttr, depth + 1, isNumeric);
        } else {
            Map<String, List<Dataset.Line>> subsets = splitDataBasedOnCategory(data, bestAttr);
            for (Map.Entry<String, List<Dataset.Line>> entry : subsets.entrySet()) {
                node.children.put(entry.getKey(),
                        buildTree(entry.getValue(), newAttributes, targetAttr, depth + 1, isNumeric));
            }
        }

//        log.info("Built node: {}", node);
        return node;
    }

    public Node build() {
        log.info("Building tree");

        Map<String, Boolean> isNumeric = new HashMap<>();
        var firstLine = dataset.getData().get(0);
        for (var attribute : firstLine.getLine().keySet()) {
            try {
                Double.parseDouble(firstLine.get(attribute));
                isNumeric.put(attribute, true);
            } catch (NumberFormatException e) {
                isNumeric.put(attribute, false);
            }
        }

        String targetAttr = dataset.getAttributes().get(dataset.getAttributes().size() - 1);
        var attributes  = new ArrayList<String>(dataset.getAttributes());
        attributes.remove(dataset.getAttributes().size()-1);
        log.info("Target attribute is {}",targetAttr);
        return buildTree(dataset.getData(), attributes, targetAttr, 0, isNumeric);
    }
    public String predict(Node root, Dataset.Line instance) {
        if (root.isLeaf) return root.label;

        if (root.threshold != null) {
            double val = Double.parseDouble(instance.get(root.attribute));
            if (val <= root.threshold && root.left != null) {
                return predict(root.left, instance);
            } else if (root.right != null) {
                return predict(root.right, instance);
            } else {
                return root.label;
            }
        }

        String val = instance.get(root.attribute);
        Node child = root.children.get(val);
        if (child != null) {
            return predict(child, instance);
        } else {
            return root.label;
        }
    }

    @ToString
    public static class Node {
        String attribute;
        Double threshold;
        String label;
        boolean isLeaf = false;
        Map<String, Node> children = new HashMap<>();
        Node left = null, right = null;
    }
}
