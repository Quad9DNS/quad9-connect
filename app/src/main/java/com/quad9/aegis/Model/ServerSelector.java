package com.quad9.aegis.Model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerSelector {

    private static final String TAG = "ServerSelector";
    // TODO Add back to ConnectStatus
    private static List<String> blockingPool = Arrays.asList("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::fe:9");
    private static List<String> noBlockingPool = Arrays.asList("9.9.9.10", "149.112.112.10", "2620:fe::10", "2620:fe::fe:10");
    private static List<String> ECSPool = Arrays.asList("9.9.9.11", "149.112.112.11", "2620:fe::11", "2620:fe::fe:11");
    private static List<String> ECSnoBlockingPool = Arrays.asList("9.9.9.12", "149.112.112.12", "2620:fe::12", "2620:fe::fe:12");

    private static List<String> candidateList;

    ServerSelector() {
    }

    ;

    public static List<String> getCandidate(VpnSeekerService service) {
        List<MyThread> runnableList = new ArrayList<>();
        candidateList = new ArrayList<>();
        int size = 1;
        if (DnsSeeker.getStatus().isUsingECS()) {
            if (DnsSeeker.getStatus().isUsingBlock()) {
                size = ECSPool.size();
                for (int i = 0; i < size; i++) {
                    runnableList.add(new MyThread(ECSPool.get(i), service));
                    runnableList.get(i).start();
                }
            } else {
                size = ECSnoBlockingPool.size();
                for (int i = 0; i < size; i++) {
                    runnableList.add(new MyThread(ECSnoBlockingPool.get(i), service));
                    runnableList.get(i).start();
                }
            }
        } else {
            if (DnsSeeker.getStatus().isUsingBlock()) {
                size = blockingPool.size();
                for (int i = 0; i < size; i++) {
                    runnableList.add(new MyThread(blockingPool.get(i), service));
                    runnableList.get(i).start();
                }
            } else {
                size = noBlockingPool.size();
                for (int i = 0; i < size; i++) {
                    runnableList.add(new MyThread(noBlockingPool.get(i), service));
                    runnableList.get(i).start();
                }
            }
        }


        for (int i = 0; i < size; i++) {
            try {
                runnableList.get(i).join();
            } catch (Exception e) {
            }
        }
        Log.i(TAG, "getCandidate done");
        return candidateList;

    }

    public static void setBlockingPool(List<String> blockingPool) {
        ServerSelector.blockingPool = blockingPool;
        DnsSeeker.getStatus().setServerName(blockingPool);
    }

    private static void addRunnableList(String s) {
        Lock lock = new ReentrantLock();

        lock.lock();
        try {
            if (!s.equals("null")) {
                candidateList.add(s);
            }
        } catch (Exception e) {

        } finally {
            lock.unlock();
        }
    }

    static class MyThread extends Thread {
        String ip;
        VpnSeekerService service;

        MyThread(String ip, VpnSeekerService service) {
            this.ip = ip;
            this.service = service;
        }

        public void run() {

            if (SSLConnector.testSocket(this.ip, service)) {
                addRunnableList(this.ip);
            }
        }
    }

}
