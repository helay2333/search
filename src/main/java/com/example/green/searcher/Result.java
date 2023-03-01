package com.example.green.searcher;

/**
 * @author Green写代码
 * @date 2023-01-21 23:27
 */
public class Result {

    private String title;
    private String url;
    private String desc;//摘要

    @Override
    public String toString() {
        return "search.Result{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
}
