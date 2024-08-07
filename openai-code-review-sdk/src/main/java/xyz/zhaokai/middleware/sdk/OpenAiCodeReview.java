package xyz.zhaokai.middleware.sdk;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {
        System.out.println("运行测试");

        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        Process process = processBuilder.start();
        StringBuilder diffCode = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
            String line;
            while (!StringUtils.isEmptyOrNull(line = bufferedReader.readLine())) {
                diffCode.append(line);
            }
        }
        process.waitFor();
        System.out.println(diffCode);
//        testInvoke(diffCode.toString());
        writeLog();
    }

    private static void testInvoke(String diffCode) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), String.format("你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:%s", diffCode));
        messages.add(chatMessage);
        String requestId = String.format("zhaokai-%d", System.currentTimeMillis());
        ClientV4 client = new ClientV4.Builder("2a75f5756d9a7fa11fa9623459c1bbae.agZET2kIyUq897Mw").build();
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("glm-4-flash")
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        log.info("response:{}", invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent());
    }

    @SuppressWarnings("all")
    private static void writeLog() throws Exception {
        String token = System.getenv("GITHUB_TOKEN");
        Git git = Git.cloneRepository()
                .setURI("https://github.com/alle2k/openai-code-review-log.git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();
        File folder = new File(String.format("%s%s", "repo/", DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, String.format("review-%d%s", System.currentTimeMillis(), ".md"));
        try (FileWriter fileWriter = new FileWriter(file);) {
            fileWriter.write("根据提供的`git diff`记录，我们可以看到以下变更：\n" +
                    "\n" +
                    "```\n" +
                    "diff --git a/openai-code-review-sdk/src/main/java/xyz/zhaokai/middleware/sdk/OpenAiCodeReview.java b/openai-code-review-sdk/src/main/java/xyz/zhaokai/middleware/sdk/OpenAiCodeReview.java\n" +
                    "index c6c60e1..9b6af9b 100644\n" +
                    "--- a/openai-code-review-sdk/src/main/java/xyz/zhaokai/middleware/sdk/OpenAiCodeReview.java\n" +
                    "+++ b/openai-code-review-sdk/src/main/java/xyz/zhaokai/middleware/sdk/OpenAiCodeReview.java\n" +
                    "@@ -6,7 +6,7 @@\n" +
                    " import java.io.BufferedReader;\n" +
                    " import java.io.InputStreamReader;\n" +
                    " \n" +
                    "-public class OpenAiCodeReview {\n" +
                    "+    public static void main(String[] args) throws Exception {\n" +
                    "         System.out.println(\"运行测试\");\n" +
                    " }\n" +
                    "```\n" +
                    "\n" +
                    "以下是针对这个变更的代码评审：\n" +
                    "\n" +
                    "1. **代码风格和命名**：\n" +
                    "   - 文件名和类名之间的空格是不必要的。通常，文件名和类名应该紧挨在一起，没有空格。因此，`OpenAiCodeReview.java` 应该是 `OpenAiCodeReview.java`。\n" +
                    "\n" +
                    "2. **类定义和主方法**：\n" +
                    "   - 在原始代码中，`OpenAiCodeReview` 类没有明确的主方法（`main` 方法）。然而，在更改后的代码中，`main` 方法被添加到类定义中，这是正确的做法，因为 `main` 方法是应用程序的入口点。\n" +
                    "   - 在 `main` 方法中，只有一个打印语句 `System.out.println(\"运行测试\");`。这看起来像是一个测试或示例打印，没有实际的功能实现。如果这个类是为了测试而设计的，那么这是可以接受的。但是，如果这是一个生产环境中的代码，那么它应该有一些实际的功能或逻辑。\n" +
                    "\n" +
                    "3. **异常处理**：\n" +
                    "   - `main` 方法中使用了 `throws Exception`，这是一个非常宽泛的异常处理方式。在实际的应用程序中，最好捕获可能发生的具体异常，并相应地处理它们。这样做可以提高代码的健壮性和可读性。\n" +
                    "\n" +
                    "4. **代码注释**：\n" +
                    "   - 在 `main` 方法之前，应该有一个类注释来描述 `OpenAiCodeReview` 类的目的。同时，添加一个方法注释来描述 `main` 方法的功能也是好的实践。\n" +
                    "\n" +
                    "总结：\n" +
                    "- 代码风格需要调整，移除不必要的空格。\n" +
                    "- `main` 方法应该包含实际的功能或逻辑，或者至少应该有一个清晰的注释说明它的目的。\n" +
                    "- 异常处理应该更加具体。\n" +
                    "- 增加必要的注释以提高代码的可读性和可维护性。");
        }
        git.add().addFilepattern(String.format("%s%s%s", file.getParentFile().getName(), "/", file.getName())).call();
        git.commit().setMessage("upload review code log").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();
    }
}
