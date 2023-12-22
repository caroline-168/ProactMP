package org.liuscraft.mydubbo.entity;

import java.io.Serializable;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

public class Data implements Serializable {


    // data
    byte[] data;

    // true - data transfer completes
    Boolean state;

    Long curOffset;

    Integer length;

    public long getCurOffset() {
        return curOffset;
    }

    public void setCurOffset(long curOffset) {
        this.curOffset = curOffset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Data(byte[] data) {
        this.data = data;

    }

    public boolean getState(){
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public Data() {
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Data{" +
                ", state=" + state +
                ", curOffset=" + curOffset +
                ", length=" + length +
                '}';
    }
}