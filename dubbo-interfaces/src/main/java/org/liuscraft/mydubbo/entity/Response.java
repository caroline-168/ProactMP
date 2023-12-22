package org.liuscraft.mydubbo.entity;

import java.io.Serializable;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

public class Response implements Serializable {

    private Boolean state;

    private String msg;

    // data packet
    private Data data;

    // unit：mb
    private Integer requestFileSize;

    private long sendTime; // todo (ms)

    public void setRequestFileSize(int requestFileSize) {
        this.requestFileSize = requestFileSize;
    }

    public int getRequestFileSize() {
        return requestFileSize;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public String getMsg() {
        return msg;
    }

    public Response setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime * 1000;
    }

    public static Response err(){
        final Response response = new Response();
        response.setState(false);
        return response;
    }

    public Response() {
    }

    public static Response success(){
        final Response response = new Response();
        response.setState(true);
        return response;
    }

    @Override
    public String toString() {
        return "Response{" +
                "state=" + state +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                ", requestFileSize=" + requestFileSize +
                ", sendTime=" + sendTime +
                '}';
    }
}