package com.example.green.searcher;

/**
 * @author Green写代码
 * @date 2023-01-20 13:45
 * 表示文档id和文档与词的相关性的权重 的包裹
 */
public class Weight {
    private int docId;
    //weight表示文档和词之间的相关性 : 这个值越大, 相关性越强
    private int weight;// 权重

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
