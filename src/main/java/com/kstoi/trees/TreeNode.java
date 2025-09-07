package com.kstoi.trees;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TreeNode {
    void forEach( Consumer<Map<String,Object>> consumer,int x,int y,int dx,int dy);
}
