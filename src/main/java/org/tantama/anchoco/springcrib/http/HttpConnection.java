package org.tantama.anchoco.springcrib.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.tantama.anchoco.springcrib.helper.JsonHelper;

/**
 * {@link java.net.http.HttpClient}を利用した
 * Http接続サンプル
 */
@Slf4j
public class HttpConnection {

    // notice. 本質はテストのスタブサーバーの利用方法

    // tips. 通信にrest templateではなくhttp clientを利用する理由の一つとして
    // rest templateはbad requestの時にbodyが取得できないというバグがあり
    // 解決策を見ても解決できなかったため、http clientを利用するという案件があった
    /** http client */
    private final HttpClient httpClient;

    /** 読み取りタイムアウト(秒) */
    @Value("${rest.read.timeout-second:5}")
    private long readTimeout;

    /**
     * コンストラクタ<br>
     * パラメータの初期設定を行う
     *
     * @param connectionTimeout 接続タイムアウト値(秒)
     */
    @Autowired
    public HttpConnection(@Value("${rest.connection.timeout-second:5}") long connectionTimeout) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.connectTimeout(Duration.ofSeconds(connectionTimeout));
        this.httpClient = builder.build();
    }

    /**
     * postによるjson接続を行う
     *
     * @param <RequestDto>  リクエストの型
     * @param <ResponseDto> レスポンスの型
     * @param url           接続先url
     * @param requestDto    リクエスト情報
     * @param responseClass レスポンスの型
     * @return レスポンス情報
     */
    public <RequestDto, ResponseDto> ResponseDto postJson(URI url, RequestDto requestDto, Class<ResponseDto> responseClass) {

        // http requestの作成
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(readTimeout))
                .uri(url)
                .headers("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(BodyPublishers.ofString(JsonHelper.toJson(requestDto)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());

            // httpstatus handling.
            switch (HttpStatus.resolve(response.statusCode())) {
                case OK:
                    log.info("ok.");
                    break;
                case CREATED:
                    // ok
                    log.info("created.");
                    break;
                case CONFLICT:
                    // user already exists.
                    log.error("keycloak user already exists, exist username is {}.");
                    // throw new InternalServerException("user already exists.");
                    break;
                case UNAUTHORIZED:
                    // 認証トークンの有効切れ
                    // time out error.
                    // throw new ConnectionTimeoutException("unauthorized.");
                    break;
                default:
                    // それ以外の想定外のステータス
                    // server error
                    // throw new InternalServerException("response status is illegal " +
                    // response.statusCode() + ". ");
                    break;
            }
            return JsonHelper.toDto(response.body(), responseClass);
        } catch (IOException | InterruptedException e) {
            // tips. 実際に利用する場合は適当なクラスにwrapする
            throw new RuntimeException(e);
        }
    }

    /**
     * postによるjson接続を行う
     *
     * @param <RequestDto>  リクエストの型
     * @param <ResponseDto> レスポンスの型
     * @param url           接続先url
     * @param requestDto    リクエスト情報
     * @param responseClass レスポンスの型
     * @return レスポンス情報
     */
    public <RequestDto, ResponseDto> ResponseDto postJson(String url, RequestDto requestDto, Class<ResponseDto> responseClass) {
        return postJson(URI.create(url), requestDto, responseClass);
    }

}
