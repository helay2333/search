package com.example.green.searcher;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Green写代码
 * @date 2023-01-17 19:38
 */
public class Parser {
//    public final static String INPUT_PATH = "/home/green/jdk-17.0.5_doc-all/docs/api";

  final String INPUT_PATH = "D:/Demo/doc_search/jdk-17.0.5_doc-all/docs/api";

    //创建一个Index实例
    private Index index = new Index();
    private void run() {
        long beg = System.currentTimeMillis();
        //整个Parse类的入口
        //1. 根据指定的路径, 枚举出该路径中的所有文件(html) 需要获取到所有子目录中的文件
        //2. 针对上面罗列的文件路径,打开文件,读取内容, 解析, 并构建索引
        //3. 把内存中构造好的索引数据结构保存到指定文件中
        ArrayList<File> fileList = new ArrayList<>();
        enumFile(INPUT_PATH, fileList);
//        System.out.println(Arrays.toString(new ArrayList[]{fileList}));
        for(File f : fileList) {
            System.out.println("开始解析:" + f.getAbsolutePath());
            parseHTML(f);
        }
        //3. 把内存中构造好的索引数据结构, 保存到指定文件中
        index.save();
        long end = System.currentTimeMillis();
        System.out.println("索引制作时间"+(end - beg));
    }
    //通过这个方法实现多线程制作索引
    public void runByThread() {
        long beg = System.currentTimeMillis();
        System.out.println("索引制作开始");
        //1. 枚举出所有文件
        ArrayList<File> files = new ArrayList<>();
        enumFile(INPUT_PATH, files);
        //2. 循环遍历文件, 此处为了能够通过多线程制作索引, 直接引入线程池
        CountDownLatch latch = new CountDownLatch(files.size());

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for(File f : files) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("解析" + f.getAbsolutePath());
                    parseHTML(f);
                    latch.countDown();
                }
            });
        }
        /**
         * 在2 3步骤之间我们还需要做一些事情, 需要等待所有线程把所有文档任务处理完, 才能够进行4步骤
         * submit只是提交任务给阻塞队列执行这些任务, 并不是任务立马就执行完了
         */
        try {
            latch.await(); //await也可以传递参数, 参数是等待的最大时间和时间的单位  -> 这样就可以进行精确控制
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //3. 保存索引
        index.save();

        long end = System.currentTimeMillis();
    }
    private void parseHTML(File f) {
        //1. 解析出HTML的标题
        String title = parseTitle(f);
        //2. 解析出HTML的URL
        String url = parseUrl(f);
        //3. 解析出HTML对应的正文
        String content = parseContentByRegex(f);
        //4. 把解析出来的这些信息, 加入到索引当中
        index.addDoc(title, url, content);
    }

    private String parseContent(File f) {
        //先一个字符一个字符读取,使用<>控制拷贝数据开关
        try {

            FileReader fileReader = new FileReader(f);
            BufferedReader bufferedReader = new BufferedReader(fileReader, 1024 * 1024);//手动设置缓冲区为1M
            boolean isCopy = true;

            StringBuilder content = new StringBuilder();
            while(true) {
                //此处的read返回值是int而不是char
                // 此处使用int返回是为了表示一些非法的情况, 比如读到了文件末尾的时候, 继续读就会返回-1
                    int ret = bufferedReader.read();
                    if(ret == -1) {
                        break;
                    }
                    char c = (char) ret;
                    if(isCopy){
                        //开关打开, 遇到的是普通字符串, 所以拷贝到StringBuilder中
                        if(c == '<') {
                            isCopy = false;
                            continue;
                        }
                        if(c=='\n') {
                            //去掉换行成空格
                            c = ' ';
                        }
                        content.append(c);
                    }
                    else{
                        //开关关闭,就不拷贝
                        if(c == '>') {
                            isCopy = true;
                        }
                    }
                }
                fileReader.close();
                return content.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return "";

    }

    private String readFile(File f){
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(f))) {
            StringBuilder content = new StringBuilder();
            while(true){
                int ret = bufferedReader.read();
                if(ret == -1) {
                    break;
                }
                char c = (char) ret;
                if(c == '\n' || c== '\r'){
                    c = ' ';
                }
                content.append(c);
            }
            return content.toString();
        }catch (IOException e){
            e.printStackTrace();
        }
        return "";

    }
    //这个方法内部是基于正则表达式,实现去标签, 以及去除script
    public String parseContentByRegex(File f) {
        //1. 把整个文件读取到String中
        String content = readFile(f);
        content = content.replaceAll("<script.*?>(.*?)</script>", " ");

        content = content.replaceAll("<.*?>", " ");

        content = content.replaceAll("\\s+"," ");//去除空格
        return content;
    }

    private String parseUrl(File f) {
        String part1 = "https://docs.oracle.com/javase/8/docs/api";
        String part2 = f.getAbsolutePath().substring(INPUT_PATH.length());
        return part1 + part2;
    }

    private String parseTitle(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".html".length());
    }

    public void enumFile(String inputPath, ArrayList<File> fileList){
        File rootPath = new File(inputPath);
        //listFiles()能够获取得到rootPath路径下所有的目录/文件, 只能看到一级, 所以我们还需要进行递归获取得到子目录的内容
        File[] files = rootPath.listFiles();
        for(File f : files){
            //如果f是一个文件, 就加入到fileList中
            //如果f是一个目录, 就递归调用enumFile这个方法
            if(f.isDirectory()){
                enumFile(f.getAbsolutePath(), fileList);
            }else{
                if(f.getAbsolutePath().endsWith(".html")){
                    fileList.add(f);
                }
            }
        }

    }
    //main方法实现的是制作整个索引的过程
    public static void main(String[] args) {
        Parser parser = new Parser();
//        parser.run();
        parser.runByThread();
    }
}
