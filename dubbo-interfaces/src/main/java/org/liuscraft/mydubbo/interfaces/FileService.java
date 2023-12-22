package org.liuscraft.mydubbo.interfaces;

import org.liuscraft.mydubbo.entity.Request;
import org.liuscraft.mydubbo.entity.Response;

import java.nio.file.NoSuchFileException;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

public interface FileService {
    Response requestFile(Request request);

    void rest();

    // whether ProactMP wants to select multipath working mode
    boolean slices(String filePath) throws NoSuchFileException;

    void setFunnelInfo(Integer w,Integer c);

    void setOffset(long offset);
}