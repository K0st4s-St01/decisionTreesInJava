package com.kstoi.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
public class ArffReader {
    private File file;

    public ArffReader(File file) throws FileNotFoundException {
        this.file = file;
    }
    public Dataset loadData() throws IOException {
        log.info("reading data ...");
        var bf = new BufferedReader(new FileReader(file));
        var lines = bf.lines().toList();
        var dataset = new Dataset(lines.stream().filter(s -> s.contains("@relation")).collect(Collectors.toList()).toString());
        var attributes = new ArrayList<String>();
        lines.stream().filter(line -> line.contains("@attribute")).forEach(line -> {
               attributes.add(line.split("@attribute")[1].trim());
        });
        var data = lines.stream().filter(line -> {
            return !line.contains("@attribute") || !line.contains("@relation") || line.contains(",");
        });
        int previous_size = attributes.size();
        attributes.removeIf( string -> string.contains("decision_o") || string.contains("decision"));
        data.forEach(datum -> {
            dataset.load(attributes,datum.split(","));
        });
//        for(String attribute : attributes)
//            log.info("{}" , attribute);
        log.info("attributes {} reduced to {} by removing decision decision_o",previous_size,attributes.size());
        log.info("data loading complete");
        dataset.setAttributes(attributes);
        return dataset;
    }
}
