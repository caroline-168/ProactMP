package org.liuscraft.mydubbo.provider;

import org.apache.dubbo.config.annotation.DubboService;
import org.liuscraft.mydubbo.entity.Data;
import org.liuscraft.mydubbo.entity.Request;
import org.liuscraft.mydubbo.entity.Response;
import org.liuscraft.mydubbo.interfaces.FileService;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

@DubboService
public class FileServiceImpl implements FileService {


    AtomicLong curOffset = new AtomicLong(0);

    // set requestFileSize to OWDbytes (can be changed according to downlink bandwidth * one-way delay)
    static Integer OWDbytes = 5; // requestFileSize
    static Integer rate_credits = 1; // downlink bandwidth (unit: mb/ms)

    FunnelLimiter.Funnel funnel = FunnelLimiter.Funnel.getInstance(rate_credits,OWDbytes);

    @Override
    public synchronized Response requestFile(Request request) {
        final Response response = new Response();

        // get binary stream
        // and send
        if (!request.getChunkSize()) {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(request.getFrom()));
                final Data data = new Data(fileContent);
                data.setState(true);
                data.setCurOffset(0);
                data.setLength(fileContent.length);
                response.setData(data);
                response.setSendTime(new Date().getTime());
                response.setState(true);
                rest();
                System.out.println("Successfully sent: " + response);
                return response;
            } catch (IOException e) {
                e.printStackTrace();
                return response.err().setMsg(e.getMessage());
            }
        }

        // read data according to the offset
        int readLen = request.getRequestFileSize();

        // GRANT -- contains the total allocated credits (credits_all)
        final int nextRequestFileSize = funnel.watering(readLen);

        try (RandomAccessFile file = new RandomAccessFile(request.getFrom(), "r")) {
            // set the reading position based on the specified offset
            file.seek(curOffset.get());

            byte[] buffer = new byte[readLen];

            // read a specified amount of bytes from a file/message
            final int read = file.read(buffer, 0, readLen);

            // read < readLen --> start over
            if (read < readLen && read != -1) {
                buffer = Arrays.copyOf(buffer, read);
            }
            final Data data = new Data(buffer);
            data.setCurOffset(curOffset.get());
            data.setLength(read);
            curOffset.set(readLen + curOffset.get());

            if (read == -1) {
                data.setState(true);
            }else {
                data.setState(false);
            }
            response.setData(data);
            response.setSendTime(new Date().getTime());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // update credits_all -- the amount of data that can be sent in the next round
        response.setRequestFileSize(nextRequestFileSize * 1024 * 1024);
        response.setState(true);
        System.out.println("Successfully sent: " + response);
        return response;
    }

    @Override
    public void rest() {
        curOffset.set(0);
    }

    @Override
    public boolean slices(String filePath) throws NoSuchFileException {
        final File file = new File(filePath);
        if (!file.exists()) {
            throw new NoSuchFileException("no such file of " +filePath + "!");
        }
        long fileSizeBytes = file.length();
        long fileSizeKB = fileSizeBytes / 1024;
        long fileSizeMB = fileSizeKB / 1024;

        // determine whether to use multiple subflows for transmission based on file size and available capacity
        return funnel.getCapacity() < fileSizeMB;
    }

    @Override
    public void setFunnelInfo(Integer w, Integer c) {
        funnel = FunnelLimiter.Funnel.getInstance(w,c);
    }

    @Override
    public void setOffset(long offset) {
        curOffset.set(offset);
    }
}