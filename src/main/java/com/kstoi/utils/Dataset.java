package com.kstoi.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
@Slf4j
@Setter
@Getter
public class Dataset {
    private String relation;
    private List<Line> data = new ArrayList<>();
    private List<String> attributes;

    public Dataset(String relation) {
        this.relation  = relation;
    }
    public void load(List<String> attributes,String[] values){
        try {
            data.add(new Line(attributes,values));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
    public Dataset[] split(int divided){
        var datasets = new Dataset[divided];
        int size = this.getData().size();

        for (int i=0; i<divided;i++){
            datasets[i]=new Dataset("{"+i+"} "+this.relation+" 1/"+divided);
            int newSize = size/divided;
            try {
                datasets[i].getData().addAll(data.subList(i*newSize,i*newSize+newSize));
                datasets[i].setAttributes(this.attributes);
            } catch (Exception e) {
                log.error("{} out of bounds",i);
            }
        }
        for(int i=0; i<divided;i++){
            log.info("{} -> size {}",datasets[i].getRelation(),datasets[i].getData().size());
        }
        return datasets;
    }

    @Slf4j
    @Setter
    @Getter
    public static class Line{
        private Map<String,String> line = new HashMap<>();

        @Override
        public String toString() {
            return "Line{" +
                    "line=" + line +
                    '}';
        }

        public Line(List<String> attributes, String[] values) throws Exception {
            var sb = new StringBuilder();
            if (values.length < attributes.size()){
                throw new Exception("values = "+values.length+" but attributes = "+attributes.size());
            }
            for (int i = 0; i < attributes.size(); i++) {
                var attribute = attributes.get(i) ;
                var value =  values[i];
                if(!attribute.equals("decision") && !attribute.equals("decision_o")) {

                    line.put(attribute, value.equals("?")?"-1":value);
                    log.info("{} -->  {}", attribute, value);
                    sb.append(attribute);
                    sb.append(" = ");
                    sb.append(value);
                    if (i != attributes.size() - 1) {
                        sb.append(" , ");
                    }
                }
            }
            log.info(sb.toString());
        }
        public String get(String attribute) {
            return this.line.get(attribute);
        }
    }

}
