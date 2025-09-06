package com.kstoi.trees;

import com.kstoi.utils.Dataset;

public interface DecTree<Node> {
    Node build();
    String predict(Node root, Dataset.Line instance);
    String toString();
}
