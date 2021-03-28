package com.github.cclient.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author cclie
 * @date 2016/7/1
 */
@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class EsHost {
    private String hostname;
    private int port;
    private String scheme;
}
