package xyz.zhaokai.middleware.sdk;

import org.eclipse.jgit.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {
        System.out.println("运行测试");

        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        Process process = processBuilder.start();
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
            String line;
            while (!StringUtils.isEmptyOrNull(line = bufferedReader.readLine())) {
                stringBuilder.append(line);
            }
        }
        process.waitFor();
        System.out.println(stringBuilder);
    }
}
