package fun.golinks.gateway.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResult {
    private final int code;
    private final String message;
}