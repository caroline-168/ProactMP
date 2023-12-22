package org.liuscraft.mydubbo.entity;

import java.io.Serializable;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

public class Request implements Serializable {

    private String from;

    private String to;

    private Boolean chunkSize;

    // the requested file or message size, unit: mb
    private int requestFileSize;

    public Request() {
    }

    public void setRequestFileSize(int requestFileSize) {
        this.requestFileSize = requestFileSize;
    }

    public Request(String from, String to, Boolean chunkSize, int requestFileSize) {
        this.from = from;
        this.to = to;
        this.chunkSize = chunkSize;
        this.requestFileSize = requestFileSize * 1024 * 1024;
    }

    public Request(String from, String to,int requestFileSize) {
        this.from = from;
        this.to = to;
        this.chunkSize = true;
        this.requestFileSize = requestFileSize * 1024 * 1024;
    }

    public void setChunkSize(Boolean chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Boolean getChunkSize() {
        return chunkSize;
    }

    public int getRequestFileSize() {
        return requestFileSize;
    }
}