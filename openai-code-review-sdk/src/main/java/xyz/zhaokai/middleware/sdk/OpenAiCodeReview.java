package xyz.zhaokai.middleware.sdk;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OpenAiCodeReview {

    private static String AUTHOR;
    private static String MESSAGE;
    private static final String PROJECT;
    private static final String BRANCH;
    private static final String GPT_MODEL = "glm-4-flash";

    public static void main(String[] args) throws Exception {
        String diffCode = getDiffCode();
        String reviewContent = invoke(diffCode);
        writeLog(reviewContent);
        sendWechatTempMsg();
    }

    private static void test0(){
        if(1==1){
            int i =1/0   ;
        }
        System.out.println("这里不会运行");
        for(;;){
        }
    }

    private static String getDiffCode() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%h");
        Process process = processBuilder.start();
        String shortCommitId;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            shortCommitId = buffer.readLine();
        }
        process.waitFor();

        processBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%an <%ae>", shortCommitId);
        process = processBuilder.start();
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            AUTHOR = buffer.readLine();
        }
        process.waitFor();

        processBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%s", shortCommitId);
        process = processBuilder.start();
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            MESSAGE = buffer.readLine();
        }
        process.waitFor();

        processBuilder = new ProcessBuilder("git", "diff", shortCommitId + "^", shortCommitId);
        process = processBuilder.start();
        StringBuilder diffCode = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while (!StringUtils.isEmptyOrNull(line = bufferedReader.readLine())) {
                diffCode.append(line);
            }
        }
        process.waitFor();
        return diffCode.toString();
    }

    private static String invoke(String diffCode) {
        // 2a75f5756d9a7fa11fa9623459c1bbae.agZET2kIyUq897Mw
        String apiSecretKey = System.getenv("CHATGLM_APIKEYSECRET");
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), String.format("你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:%s", diffCode));
        String requestId = String.format("zhaokai-%d", System.currentTimeMillis());
        ClientV4 client = new ClientV4.Builder(apiSecretKey).build();
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(GPT_MODEL)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(Collections.singletonList(chatMessage))
                .requestId(requestId)
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        return (String) invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent();
    }

    @SuppressWarnings("all")
    private static void writeLog(String reviewContent) throws Exception {
        String token = System.getenv("GITHUB_TOKEN");
        // "https://github.com/alle2k/openai-code-review-log.git"
        String githubReviewLogUri = System.getenv("GITHUB_REVIEW_LOG_URI");
        Git git = Git.cloneRepository()
                .setURI(githubReviewLogUri)
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();
        File folder = new File(String.format("%s%s", "repo/", DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, String.format("%s-%s-%s-%d%s", PROJECT.replace(".git", ""), BRANCH, AUTHOR.split(" ")[0], System.currentTimeMillis(), ".md"));
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(reviewContent);
        }
        git.add().addFilepattern(String.format("%s%s%s", file.getParentFile().getName(), "/", file.getName())).call();
        git.commit().setMessage("upload review code log").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();
    }

    private static void sendWechatTempMsg() {
        // appid:wx0bc0db26091b6a12
        String wechatAppid = System.getenv("WECHAT_APPID");
        // secret:7adb2d9c491c8242590f8166ae392f3f
        String wechatSecret = System.getenv("WECHAT_SECRET");
        String tokenResponseStr = HttpUtil.get(String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", wechatAppid, wechatSecret));
        JSONObject jsonObject = JSON.parseObject(tokenResponseStr);
        String accessToken = jsonObject.getString("access_token");

        // ohfu_6eB47rBvK-QnIDs8nOzvF3U
        String wechatToUser = System.getenv("WECHAT_TO_USER");
        // AQ4TNpItfp35b_1AjThSXrTqAf-hVogTX043mlKjbY0
        String wechatTempId = System.getenv("WECHAT_TEMP_ID");
        Map<String, Object> param = new HashMap<>();
        param.put("touser", wechatToUser);
        param.put("template_id", wechatTempId);
        Map<String, Model> data;
        param.put("data", data = new HashMap<>());
        data.put("repo_name", new Model(PROJECT));
        data.put("branch_name", new Model(BRANCH));
        data.put("commit_author", new Model(AUTHOR));
        data.put("commit_message", new Model(MESSAGE));
        HttpUtil.post(String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken), JSON.toJSONString(param));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Model {
        String value;
        String color = "#173177";

        public Model(String value) {
            this.value = value;
        }
    }

    static {
        PROJECT = System.getenv("COMMIT_PROJECT");
        BRANCH = System.getenv("COMMIT_BRANCH");
    }
}
