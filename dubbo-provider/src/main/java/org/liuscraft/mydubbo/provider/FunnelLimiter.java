package org.liuscraft.mydubbo.provider;
import java.lang.*;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 * @Function: Overcommitment of ProactMP
 * @Explaination: Decide how many credits to allocate
 */

public class FunnelLimiter   {

    // set requestFileSize to OWDbytes (can be changed according to downlink bandwidth * one-way delay)
    static Integer OWDbytes = 5; // requestFileSize
    static Integer rate_credits = 1; // downlink bandwidth (unit: mb/ms)

    static class Funnel {

        /**
         * Coupled congestion control of ProactMP (at the receiver)
         */
        // Implement ProactMP's overcommitment with funnel algorithm

        // water rate
        private float waterRate; // requestFileSize

        // funnel capacity
        private Integer capacity;

        // remaining funnel capacity
        private Integer leftCapacity;

        // allocated credits of the last response
        private Integer lastCapacity;

        // @param T_last (the time for the receiver to allocate credits last time)
        private long lastAccess;

        // @param T_next (the time for the receiver to allocate credits this time && current time)
        private long curLastTime;

        private static Funnel funnel;

        public Integer getCapacity() {
            return capacity;
        }

        /**
         * @param waterRate is the rate of allocating credits
         * @param capacity is the total credits (total available capacity at the receiver)
         */

        private Funnel(Integer waterRate, Integer capacity) {
            // unit: mb
            this.waterRate = waterRate;
            // unit: mb
            this.capacity = capacity;
            this.leftCapacity = capacity;
            this.lastAccess = System.currentTimeMillis();
        }

        // unit: mb (default) ("byte" will cause the variables to overflow)
        public static Funnel getInstance(int waterRate,int capacity) {
            funnel = new Funnel(waterRate, capacity);
            return funnel;
        }

        /**
         * Allocating credits
         */
        public void runningWater() {
            long nowTime = System.currentTimeMillis();
            curLastTime = System.currentTimeMillis();
            long deltaTime = nowTime - lastAccess;

            // base one-way delay (unit: ms)
            int OWD_base = OWDbytes / rate_credits;

            if (deltaTime > OWD_base) {
                deltaTime = OWD_base;
            }
            // calculate the generated credits in the process of waiting for responses
            // F(T_a,T_b)
            int deltaCapacity = Float.valueOf(deltaTime * waterRate).intValue();
            int credits_all = 0;

            // running water + idle capacity = current remaining capacity
            // the receiver still has spare credits
            if (capacity >= 0) {
                credits_all = capacity + deltaCapacity;
                capacity -= credits_all;
            }
            // there is no available credits, but can overcommit
            else if (0 > capacity && capacity >= -OWDbytes) {
                credits_all = deltaCapacity;
                capacity -= credits_all;
            }
            // stop overcommitting
            else {
                try {
                    // set the waiting time to a multiple of RTT
                    Thread.sleep(4*OWD_base);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // renew T_last
            this.lastAccess = nowTime;

            // calculate a negative number, for the last request taking too long, need to be rechecked
            if (deltaCapacity < 0) {
                if (capacity >= 0) {
                    this.leftCapacity = capacity;
                }
                else {
                    this.leftCapacity = 0;
                }
                return;
            }
            if (deltaCapacity < 1) {
                return;
            }

            this.leftCapacity = credits_all;
            // remaining credits
            this.capacity = capacity;

            if (this.leftCapacity < 0) {
                this.leftCapacity = 0;
            }
        }

        /**
         * Idle credits generated in the process of waiting for responses
         * @param allCapacity (allocated credits in the last round)
         * @return leftCapacity (allocated credits in the next round)
         */

        public int watering(Integer allCapacity) {
            /* curLastTime = System.currentTimeMillis(); */
            // Calculate the remaining credits first
            runningWater();
            /* if (leftCapacity >= allCapacity) {
                this.leftCapacity -= allCapacity;
            } */
            return leftCapacity;
        }

    }

    public static void main(String[] args) throws InterruptedException {
        final Funnel funnel = Funnel.getInstance(1,OWDbytes);

        // used to test only (can be deleted)
        /* System.out.println(funnel.watering(OWDbytes));
        System.out.println(funnel.watering(OWDbytes));
        System.out.println(funnel.watering(OWDbytes));
        System.out.println(funnel.watering(OWDbytes));
        Thread.sleep(1000L);
        System.out.println(funnel.watering(OWDbytes));
        System.out.println(funnel.watering(OWDbytes)); */
    }
}