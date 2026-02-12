package com.base.seqflow.core;


import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;


public class SeqFlow {

    private static IdSegmentManager segmentManager;


    @Autowired
    public void setSegmentManager(IdSegmentManager segmentManager) {
        SeqFlow.segmentManager = segmentManager;
    }


    public static long nextId(String bizTag) {
        checkBean();
        return segmentManager.nextId(bizTag);
    }


    public static String nextId(String bizTag, Integer formatLen) {
        checkBean();
        long l = segmentManager.nextId(bizTag);
        if (formatLen == null) {
            return String.valueOf(l);
        }
        return String.format("%0" + formatLen + "d", l);
    }

    public static String nextPureBizNo(String bizTag, Integer formatLen) {
        checkBean();
        long l = segmentManager.nextId(bizTag);
        if (formatLen == null) {
            return String.valueOf(l);
        }
        return bizTag + String.format("%0" + formatLen + "d", l);
    }

    public static String nextBizNo(String bizTag, Integer formatLen) {
        checkBean();
        String yyyyMMdd = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String dailyBizTag = bizTag + yyyyMMdd;
        long l = segmentManager.nextId(dailyBizTag);
        if (formatLen == null) {
            return String.valueOf(l);
        }
        return dailyBizTag + String.format("%0" + formatLen + "d", l);
    }

    public static String nextBizNo(String bizTag) {
        checkBean();
        String yyyyMMdd = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String dailyBizTag = bizTag + yyyyMMdd;
        long l = segmentManager.nextId(dailyBizTag);
        return dailyBizTag+ String.format("%04d", l);
    }

    public static String nextBizNo(String bizTag, String dateFormat, Integer formatLen) {
        checkBean();
        String yyyyMMdd = new SimpleDateFormat(dateFormat).format(new Date());
        String dailyBizTag = bizTag + yyyyMMdd;
        long l = segmentManager.nextId(dailyBizTag);
        if (formatLen == null) {
            return String.valueOf(l);
        }
        return dailyBizTag + String.format("%0" + formatLen + "d", l);
    }


    private static void checkBean() {
        if (segmentManager == null) {
            throw new IllegalStateException("IdGenerator not initialized by Spring");
        }
    }
}