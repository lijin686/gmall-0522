package com.atguigu.gmall.scheduling.job;


import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

@Component
public class MyJobHandler {

    @XxlJob("myJobHandler")
    public ReturnT<String> executor(String param){
        XxlJobLogger.log("使用 XxlJobLogger打印日志");
        System.out.println("我的任务执行了：" + param + "，线程：" + Thread.currentThread().getName());
        return ReturnT.SUCCESS;
    }
}
