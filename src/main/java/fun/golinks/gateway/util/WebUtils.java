package fun.golinks.gateway.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;

public class WebUtils {

    public static boolean acceptsHtml(ServerWebExchange exchange) {
        try {
            List<MediaType> acceptedMediaTypes = exchange.getRequest().getHeaders().getAccept();
            acceptedMediaTypes.remove(MediaType.ALL);
            MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
            return acceptedMediaTypes.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    public static String getClientIp(ServerWebExchange exchange) {
        // 从请求头中获取 X-Forwarded-For 或 X-Real-IP
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");

        // 优先使用 X-Forwarded-For，如果存在则取第一个 IP
        if (StringUtils.isNotBlank(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        // 如果 X-Forwarded-For 不存在，则使用 X-Real-IP
        if (StringUtils.isNotBlank(xRealIp)) {
            return xRealIp;
        }

        // 如果以上头部都不存在，则直接获取远程地址
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        // 如果无法获取 IP，返回默认值
        return "";
    }
}
