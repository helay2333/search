package com.example.green.searcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Green写代码
 * @date 2023-01-19 15:28
 */
public class Index {
    public static String PATH = "";

    static {
        if(Config.isOnline){
            PATH = "/home/green/";
        }else{
            PATH = "D:\\Demo\\doc_search\\";
        }
    }
    private ObjectMapper objectMapper = new ObjectMapper();
    //使用数组下标表示docId
    private ArrayList<DocInfo> forwardIndex = new ArrayList<>();

    //使用哈希表表示倒排索引
    //key 词
    //value 一组和这个词关联的文章 使用
    private HashMap<String, ArrayList<Weight>> invertedIndex = new HashMap<>();
    //1. 给定一个docId, 在正排索引中, 查询文档的详细信息
    public DocInfo getDocInfo(int docId){
        //O(1)
        return forwardIndex.get(docId);
    }
    //2. 给定一个词, 在倒排索引中, 查哪些文档和这个词有关联
    public List<Weight> getInverted(String term) {
        //O(1)
        return invertedIndex.get(term);
    }

    //3. 往索引中新增一个文档
    public void addDoc(String title, String url, String content) {
        //新增文档操作 同时给正排索引和倒排索引新增信息
        //构建正排索引
        DocInfo docInfo = buildForward(title, url, content);
        //构建倒排索引
        buildInverted(docInfo);

    }

    //倒排索引是统计词在哪些文档中出现的
    private void buildInverted(DocInfo docInfo) {
        class WordCnt{
            public int titleCount;//表示这个词在标题中出现的次数
            public int contentCount;//表示这个词在正文中出现的次数
        }
        //这个数据结构用来统计词频
        HashMap<String, WordCnt> wordCntHashMap = new HashMap<>();
        //1. 针对文档标题进行分词
        List<Term>terms = ToAnalysis.parse(docInfo.getTitle()).getTerms();
        //2. 遍历分词结果, 统计每个词出现的次数
        for(Term term : terms) {
            String word = term.getName();
            WordCnt wordCnt = wordCntHashMap.get(word);
            if(wordCnt == null) {
                //如果不存在, 就创建一个新的键值对, 插入进去, titleCount设置成1
                WordCnt newWordCnt = new WordCnt();
                newWordCnt.titleCount = 1;
                newWordCnt.contentCount = 0;
                wordCntHashMap.put(word, newWordCnt);

            }else{
                //如果存在, 就找到之前的值, 然后把对应的titleCount + 1
                wordCnt.titleCount += 1;
            }
        }
        //3. 针对正文页艰辛分词
        terms = ToAnalysis.parse(docInfo.getContent()).getTerms();
        for(Term term : terms) {
            String word = term.getName();
            WordCnt wordCnt = wordCntHashMap.get(word);
            if(wordCnt == null) {
                WordCnt newWordCnt = new WordCnt();
                newWordCnt.titleCount = 0;
                newWordCnt.contentCount = 1;
                wordCntHashMap.put(word, newWordCnt);
            }else{
                wordCnt.contentCount += 1;
            }
        }
        //4. 遍历分词结果, 统计每个词出现的次数
        //5. 把上面的结果汇总到一个HashMap里面, 最终文档的权重设定成: 标题中出现的次数*10 + 正文中出现的次数

        //6. 遍历刚才的HashMap,依次来更新倒排索引中的结构
        for(Map.Entry<String, WordCnt> entry : wordCntHashMap.entrySet()) {
            synchronized (invertedIndex){//加锁的就是invertedIndex, 倒排索引和正排索引是两个锁, 各加各的
                List<Weight> invertedList = invertedIndex.get(entry.getKey());
                if(invertedList == null) {
                    ArrayList<Weight> newInvertedList = new ArrayList<>();
                    Weight weight = new Weight();
                    weight.setDocId(docInfo.getDocId());
                    weight.setWeight(entry.getValue().titleCount * 10 + entry.getValue().contentCount);
                    newInvertedList.add(weight);
                    invertedIndex.put(entry.getKey(), newInvertedList);

                } else{
                    Weight weight = new Weight();
                    weight.setDocId(docInfo.getDocId());

                    weight.setWeight(entry.getValue().titleCount * 10 + entry.getValue().contentCount);
                    invertedList.add(weight);
                }
            }
        }
    }

    private DocInfo buildForward(String title, String url, String content) {
        DocInfo docInfo = new DocInfo();
        docInfo.setTitle(title);
        docInfo.setUrl(url);
        docInfo.setContent(content);
        synchronized (forwardIndex){
            docInfo.setDocId(forwardIndex.size());
            forwardIndex.add(docInfo);
        }
        return docInfo;
    }

    //4. 把内存中的索引结构保存到磁盘中
    public void save() {
        long beg = System.currentTimeMillis();

        System.out.println("保存索引开始");
        File indexPathFile = new File(PATH);
        if(!indexPathFile.exists()) {
            indexPathFile.mkdirs();
        }
        File forwardIndexFile = new File(PATH+"forward.txt");
        File invertedIndexFile = new File(PATH+"inverted.txt");
        try{
            objectMapper.writeValue(forwardIndexFile, forwardIndex);//把forwardIndex序列化成字母串写入到文件中
            objectMapper.writeValue(invertedIndexFile, invertedIndex);//把invertedIndex序列化成字母串写入到文件中
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("保存索引结束");
        long end = System.currentTimeMillis();

        System.out.println("消耗时间" + (end - beg));

    }
    //5. 把磁盘中的索引数据加载到内存中
    public void load() {
        long beg = System.currentTimeMillis();
        System.out.println("加载索引开始");
        File forwardIndexFile = new File(PATH + "forward.txt");
        File invertedIndexFile = new File(PATH+"inverted.txt");
        try{
            forwardIndex = objectMapper.readValue(forwardIndexFile, new TypeReference<ArrayList<DocInfo>>() {
            });
            invertedIndex = objectMapper.readValue(invertedIndexFile, new TypeReference<HashMap<String, ArrayList<Weight>>>() {
            });
        }catch (IOException e){

        }
        System.out.println("加载索引结束");
        long end = System.currentTimeMillis();
        System.out.println("消耗时间" + (end - beg));

    }
}
