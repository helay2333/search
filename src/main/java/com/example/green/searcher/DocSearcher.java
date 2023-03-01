package com.example.green.searcher;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Green写代码
 * @date 2023-01-21 23:25
 * 完成整个搜索过程
 */
public class DocSearcher {
    private static String STOP_WORD_PATH = "";
    static {
        if(Config.isOnline){
            STOP_WORD_PATH = "/home/green/stop_word.txt";
        }else{
            STOP_WORD_PATH = "D:\\Demo\\doc_search\\stop_word.txt";
        }
    }
    private HashSet<String> stopWords = new HashSet<>();
    //这里要加上索引对象的实例
    //同时要完成索引加载的工作
    private Index index = new Index();

    public DocSearcher() {
        index.load();
    }
    //参数是搜索的查询词
    //返回值是搜索结果的集合
    public List<Result> search(String query) {
        //1. 针对query 这个查询词进行分词
        List<Term>oldTerms = ToAnalysis.parse(query).getTerms();
        List<Term> terms = new ArrayList<>();
        for(Term term : oldTerms){
            //过滤无用分词
            if(stopWords.contains(term.getName())){
                continue;
            }
            terms.add(term);
        }
        //2. 针对分词结果查倒排
//        List<List<Weight>>
        List<Weight> allTermResult = new ArrayList<>();
        for(Term term : terms) {
            String word = term.getName();
            List<Weight> invertedList = index.getInverted(word);
            if(invertedList == null) {
                //说明这个词在所有文档中都不存在
                continue;
            }
            allTermResult.addAll(invertedList);
        }
        allTermResult.sort(new Comparator<Weight>() {
            @Override
            public int compare(Weight o1, Weight o2) {
                return o2.getWeight() - o1.getWeight();
            }
        });
        //3. 针对倒排结果排序
        //4. 查正排
        List<Result> results = new ArrayList<>();
        for(Weight weight : allTermResult) {
            DocInfo docInfo = index.getDocInfo(weight.getDocId());
            Result result = new Result();
            result.setTitle(docInfo.getTitle());
            result.setUrl(docInfo.getUrl());
            result.setDesc(GenDesc(docInfo.getContent().toLowerCase(Locale.ROOT), terms));
            results.add(result);
        }
        return results;
    }

    private String GenDesc(String content, List<Term> terms) {
        int firstPos = -1;
        for(Term term : terms) {
            String word = term.getName();
            //独立成词
            content = content.toLowerCase().replaceAll("\\b"+word + "\\b", " " + word + " ");
            firstPos = content.toLowerCase().indexOf(" " + word + " ");
            if(firstPos >= 0) {
                //找到了位置
                break;
            }
        }
        if(firstPos == -1) {
            //比较极端的情况, 分词结果不存在文章中
            if(content.length() > 160)
                return content.substring(0, 160) + "...";
            return content;
        }
        String desc = "";
        int descBeg = firstPos < 60 ?  0 : firstPos -60;
        if(descBeg + 160 > content.length()) {
            desc = content.substring(descBeg);
        }else{
            desc = content.substring(descBeg, descBeg + 160) + "...";
        }
        //在这里添加i标签
        //将描述中的和分词结果相同的部分, 给加上一层i标签, 就可以通过replace实现
        for(Term term : terms) {
            String word = term.getName();
            //此处是全字匹配, 当前查询词为List的时候, 不能把ArrayList中的List单独标红
            desc = desc.replaceAll("(?i) " + word, "<i> "+word+"</i>");
        }
        return desc;
    }
    public void loadStopWords() {
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(STOP_WORD_PATH))) {
            while(true) {
                String line = bufferedReader.readLine();
                if(line == null) {
                    break;
                }
                stopWords.add(line);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DocSearcher docSearcher = new DocSearcher();
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("->");
            String query = scanner.next();
            List<Result> results = docSearcher.search(query);
            for(Result result : results) {
                System.out.println("================");
                System.out.println(result);
                System.out.println("================");

            }
        }
    }

}
