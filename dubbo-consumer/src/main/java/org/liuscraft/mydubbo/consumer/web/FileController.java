package org.liuscraft.mydubbo.consumer.web;

import org.apache.dubbo.config.annotation.DubboReference;
import org.liuscraft.mydubbo.entity.Data;
import org.liuscraft.mydubbo.entity.Request;
import org.liuscraft.mydubbo.entity.Response;
import org.liuscraft.mydubbo.interfaces.FileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Objects;
import java.lang.*;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

@RequestMapping
@RestController
public class FileController {


    @DubboReference(retries = 3,timeout = 10000)
    FileService fileService;

    // set requestFileSize to OWDbytes (can be changed according to downlink bandwidth * one-way delay)
    int requestFileSize = 5;
    int OWDbytes = requestFileSize;

    @PostMapping("/setFunnelInfo")
    public String setFunnelInfo(Integer w,Integer c){

        fileService.setFunnelInfo(w,c);
        // "w" is the rate of allocating credits
        // "c" is the total credits (total available capacity at the receiver)
        return "Control Rate: " + w + "Capacity" + c;
    }

    @PostMapping("/setRQFS")
    public String setRequestFileSize(Integer requestFileSize){
        this.requestFileSize = requestFileSize;
        //"requestFileSize" is OWDbytes, which is the data amount that bursts in the first RTT
        return "Success, burst in the first RTT:" + requestFileSize;
    }

    /**
     * Coupled congestion control of ProactMP (at the sender)
     */
    @PostMapping
    public String getFile(String from,String to) throws Exception {
        if (Objects.isNull(from) || Objects.isNull(to)) {
            return "null";
        }

        //number of subflows (default: 2)
        int subflow_num = 2;
        int[] weights = new int[subflow_num];
        long[] now = new long[subflow_num];
        long[] last = new long[subflow_num];
        long[] RTT = new long[subflow_num];

        // initialization
        for (int i = 1 ; i <= subflow_num ; i++) {
            weights[i] = 0;
        }

        to = to + "//" + Paths.get(from).getFileName().toString();

        // Record the current offset
        long curOffset = 0;
        final long begin = System.currentTimeMillis();
        RandomAccessFile outputFile = new RandomAccessFile(to, "rw");
        boolean slices = false;
        try{
            fileService.rest();
            slices = fileService.slices(from);
            weights[1] ++;
            // calculate the RTT of the master subflow
            last[1] = now[1];
            now[1] = System.currentTimeMillis();
            RTT[1] = now[1]- last[1];

            if (!slices){
                final Request request = new Request(from, to, slices,requestFileSize);
                Response response = fileService.requestFile(request);
                downloadData(outputFile, response.getData());
            }else {
                int credits_all = requestFileSize;

                for (int n = 2 ; n <= subflow_num ; n++) {
                    weights[n]++;
                    if (weights[n] <= 0) {
                        weights[n] = 1; }
                    // calculate the RTTs of other subflows
                    last[n] = now[n];
                    now[n] = System.currentTimeMillis();
                    RTT[n] = now[n] - last[n];
                }

                int[] G_r_t = new int[subflow_num];
                int subflow;
                int wRTT_total = 0;

                // send this message with multiple subflow (two)
                // allocate credits_all to different subflows
                for (int k = 1; k <= subflow_num ; k++) {
                    int RTT_k = Long.valueOf(RTT[k]).intValue();
                    subflow = weights[k] / (RTT_k * RTT_k);
                    wRTT_total += subflow;
                }
                for (int m = 1; m <= subflow_num ; m++) {
                    int G_subflow = 0;
                    int RTT_m = Long.valueOf(RTT[m]).intValue();
                    G_r_t[m] = credits_all * (weights[m] / (RTT_m * RTT_m)) / wRTT_total;

                }
                // int p1 = requestFileSize / 2;
                // int p2 = requestFileSize - p1;

                final Request request1 = new Request(from, to, slices,G_r_t[1]);
                final Request request2 = new Request(from, to, slices,G_r_t[2]);

                Response response1 = fileService.requestFile(request1);
                Response response2 = fileService.requestFile(request2);

                if (!response1.getState() || !response2.getState()) {
                    return "Something got wrong: " +response1.getMsg() + response2.getMsg();
                } else {
                    while (true) {
                        final int requestFileSize = response1.getRequestFileSize();
                         // p1 = requestFileSize / 2;
                         // p2 = requestFileSize - p1;

                        final Data data1 = response1.getData();
                        final Data data2 = response2.getData();
                        curOffset = data2.getCurOffset();

                        // is data2 on subflow2? - if data2 -1
                        if (data2.getState()) {
                            if (!data1.getState()){
                                downloadData(outputFile, data1);
                            }
                            break;
                        }
                        downloadData(outputFile, data1);
                        downloadData(outputFile, data2);
                        request1.setRequestFileSize(G_r_t[1]);
                        request2.setRequestFileSize(G_r_t[2]);
                        response1 = fileService.requestFile(request1);
                        response2 = fileService.requestFile(request2);
                    }
                }
            }

        } catch (Exception e) {
            final boolean b = retriesEvent(3, curOffset, from, to, slices, outputFile,10000);
            if (!b){
                // the sender does not respond
                return "Timeout occurred 3 times, the sender may be down.";
            }
            e.printStackTrace();
        }finally {
            outputFile.close();
        }
        long end = System.currentTimeMillis();

        // return flow completion time (FCT)
        return "Message completes. It takes：" + (end - begin) + "ms";
    }

    /**
     * Loss Recovery of ProactMP (at the receiver)
     * @Explaination: Use no explicit acknowledgement for lost packets.
     * The receiver requests the missing bytes with current offsets.
     */

    // timeout and retry (default: 3 times )
    public boolean retriesEvent(int retries,long curOffset,String from,String to,boolean slices,RandomAccessFile outputFile,long sleep) throws IOException, InterruptedException {
        Thread.sleep(sleep);
        if (retries == 0){
            return false;
        }
        try {
            final Request request = new Request(from, to, slices,requestFileSize);
            fileService.setOffset(curOffset);
            Response response = fileService.requestFile(request);

            if (!response.getState() ) {
                return false;
            } else {
                while (true) {
                    final int requestFileSize = response.getRequestFileSize();

                    final Data data = response.getData();
                    curOffset = data.getCurOffset();
                    if (data.getState()) {
                        break;
                    }
                    // send RESEND packet, retransmit the specified bytes
                    request.setRequestFileSize(requestFileSize);
                    downloadData(outputFile, data);
                    response = fileService.requestFile(request);
                }
            }
        }catch (Exception rex){
            retries--;
            final boolean b = retriesEvent(retries, curOffset, from, to, slices, outputFile,sleep * 2);
            return b;
        }

        return true;
    }
    private void downloadData(RandomAccessFile outputFile, Data data) throws IOException {

        outputFile.seek(data.getCurOffset());
        outputFile.write(data.getData());
    }
}