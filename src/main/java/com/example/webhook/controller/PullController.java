package com.example.webhook.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


@RestController
@RequestMapping("webhook")
@Slf4j
public class PullController {

    @Value("${shell.startFileName:start.sh}")
    private String fileName;

    @Value("${shell.directory}")
    private String directory;

    @Value("${shell.token:zhima123}")
    private String token;


    @Autowired
    private ResourceLoader resourceLoader;


    /**
     * 请求
     *
     * @param userAgent
     * @param giteeToken
     * @param giteeEvent
     * @return
     * @throws IOException
     */
    @PostMapping("autoUpdate")
    public String auto(@RequestHeader("User-Agent") String userAgent,
                       @RequestHeader("X-Gitee-Token") String giteeToken,
                       @RequestHeader("X-Gitee-Event") String giteeEvent) throws IOException {
/*        //git-oschina-hook
        log.info("User-Agent:{}", userAgent);
        //zhimaxxx
        log.info("X-Gitee-Token:{}", giteeToken);
        //Push Hook
        log.info("Gitee-Event:{}", giteeEvent);*/
        //鉴权先没做
        //if ("git-oschina-hook".equals(userAgent) && "PUSH HOOK".equals(giteeEvent) && token.equals(giteeToken)) {
        executeShell();
        //}
        return "success";
    }


    /**
     * 执行脚本
     *
     * @throws IOException
     */
    public void executeShell() throws IOException {
        //有则返回无则生成文件
        generateFile(fileName);

        //赋予755权限并调用
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/chmod", "755", getFullName(fileName));
        processBuilder.directory(new File(directory));
        Process process = processBuilder.start();
        //记日志
        String input;
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((input = stdInput.readLine()) != null) {
            log.info(input);
        }
        while ((input = stdError.readLine()) != null) {
            log.error(input);
        }

        int runningStatus = 0;
        try {
            runningStatus = process.waitFor();
        } catch (InterruptedException e) {
            log.error("shell", e);
        }

        if (runningStatus != 0) {
            log.error("failed.");
        } else {
            log.info("success.");
        }
    }


    private void generateFile(String fileName) throws IOException {
        String fileFullName = getFullName(fileName);
        File file = new File(fileFullName);
        if (file.exists()) {
            return;
        }
        // 不存在先删除再新建
        FileWriter fileWriter = new FileWriter(fileFullName);
        Resource resource = resourceLoader.getResource("classpath:" + fileName);
        InputStream inputStream = resource.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String data;
        while ((data = bufferedReader.readLine()) != null) {
            fileWriter.write(data + "\n");
        }
        bufferedReader.close();
        inputStreamReader.close();
        inputStream.close();
        fileWriter.close();
        // 设置权限，否则会报 Permission denied
        file.setReadable(true);
        file.setWritable(true);
        file.setExecutable(true);
    }


    /**
     * 文件调用全路径
     *
     * @param fileName
     * @return
     */
    private String getFullName(String fileName) {
        return directory + File.separator + fileName;
    }

}