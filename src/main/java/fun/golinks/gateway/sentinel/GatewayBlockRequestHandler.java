package fun.golinks.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public class GatewayBlockRequestHandler implements BlockRequestHandler {

    private static final String BLOCK_MSG = "尊敬的用户，您好！\n" + "\n"
            + "由于您的访问频率过高，已触发网站的流量限制保护。这是为了确保所有用户都能公平、稳定地使用我们的服务而设置的安全机制。请您稍作调整后重试。";

    private static final String BLOCK_HTML = "<!DOCTYPE html>\n" + "<html lang=\"zh-CN\">\n" + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "    <title>访问限制提示</title>\n" + "    <style>\n" + "        body {\n"
            + "            font-family: Arial, sans-serif;\n" + "            background-color: #f5f5f5;\n"
            + "            display: flex;\n" + "            justify-content: center;\n"
            + "            align-items: center;\n" + "            height: 100vh;\n" + "            margin: 0;\n"
            + "        }\n" + "        .container {\n" + "            background-color: #fff;\n"
            + "            padding: 30px;\n" + "            border-radius: 8px;\n"
            + "            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);\n" + "            max-width: 500px;\n"
            + "            text-align: center;\n" + "        }\n" + "        h1 {\n" + "            color: #333;\n"
            + "            font-size: 24px;\n" + "            margin-bottom: 20px;\n" + "        }\n" + "        p {\n"
            + "            color: #666;\n" + "            font-size: 16px;\n" + "            line-height: 1.6;\n"
            + "            margin: 10px 0;\n" + "        }\n" + "        .highlight {\n"
            + "            color: #e74c3c;\n" + "            font-weight: bold;\n" + "        }\n" + "    </style>\n"
            + "</head>\n" + "<body>\n" + "    <div class=\"container\">\n" + "        <h1>访问限制提示</h1>\n"
            + "        <p>尊敬的用户，您好！</p>\n"
            + "        <p>由于您的访问频率过高，已触发网站的<span class=\"highlight\">流量限制保护</span>。这是为了确保所有用户都能公平、稳定地使用我们的服务而设置的安全机制。</p>\n"
            + "        <p>请您稍作调整后重试。</p>\n" + "    </div>\n" + "</body>\n" + "</html>";

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        if (acceptsHtml(exchange)) {
            return htmlErrorResponse();
        }
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildErrorResult());
    }

    private Mono<ServerResponse> htmlErrorResponse() {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).header("Content-Type", "text/html;charset=UTF-8")
                .bodyValue(BLOCK_HTML);
    }

    private ErrorResult buildErrorResult() {
        return new ErrorResult(HttpStatus.TOO_MANY_REQUESTS.value(), BLOCK_MSG);
    }

    private boolean acceptsHtml(ServerWebExchange exchange) {
        try {
            List<MediaType> acceptedMediaTypes = exchange.getRequest().getHeaders().getAccept();
            acceptedMediaTypes.remove(MediaType.ALL);
            MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
            return acceptedMediaTypes.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    private static class ErrorResult {
        private final int code;
        private final String message;
    }
}
