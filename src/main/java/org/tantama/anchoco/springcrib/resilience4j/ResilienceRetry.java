package org.tantama.anchoco.springcrib.resilience4j;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

/**
 * リトライ処理の設定などの提供
 */
@Slf4j
@Component
public class ResilienceRetry {

    /** メモリ上に保存するリトライ設定のキー値 */
    private static final String RETRY_REG_KEY = "reg_key";

    /** 最大実行回数(リトライなしの場合1) */
    @Value("${retry.max-attempts:1}")
    private int maxAttempts;

    /** リトライ時のインターバル(s) */
    @Value("${retry.wait-duration:1}")
    private long retryInterval;

    /** {@link Retry} */
    private Retry retry;

    /**
     * 初期化処理<br>
     * リトライ設定を行う
     */
    @PostConstruct
    public void init() {

        log.debug("retry setteings start.");

        // リトライ設定
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofSeconds(retryInterval))
                // 通信タイムアウトの場合にリトライを行う
                // tips. ここの定義は通信部品(今回の場合はspring rest template)が投げるexceptionなどに依存する
                // 実行時にthrowするエラーを定義することで、独自exceptionでも対応可能
                .retryExceptions(HttpServerErrorException.class)
                .build();

        retry = RetryRegistry.of(config).retry(RETRY_REG_KEY);

        // リトライ時にログ出力
        Retry.EventPublisher publisher = retry.getEventPublisher();
        publisher.onRetry(event -> log.warn(event.toString()));
    }

    /**
     * リトライ設定に基づいた処理の実行を行う
     *
     * @param <T> 実行処理のレスポンス
     * @param sup 実行処理
     * @return レスポンス
     */
    public <T> T excuteRetry(Supplier<T> sup) {
        return retry.executeSupplier(sup);
    }
}