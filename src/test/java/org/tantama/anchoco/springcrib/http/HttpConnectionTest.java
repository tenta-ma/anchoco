package org.tantama.anchoco.springcrib.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.tantama.anchoco.springcrib.helper.JsonHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link HttpConnection}のテストクラス
 */
class HttpConnectionTest {

    /** テスト来小クラス */
    private HttpConnection target;

    // mock server を利用したテストを実行したときにdebugでのstack traceが出力される
    // 問題はないが、気になる場合はシステムプロパティで'-Dcom.couchbase.client.core.deps.io.netty.noUnsafe=true'を設定する
    // https://forums.couchbase.com/t/running-under-jdk-11-prints-stack-trace/22793
    // https://stackoverflow.com/questions/60241857/java-lang-unsupportedoperationexception-reflective-setaccessibletrue-disabled

    /** mock server */
    private static ClientAndServer mockServer;

    /** mock server のhost名 */
    private static final String MOCK_SERVER_HOST = "localhost";

    /** mock server のport */
    private static final int MOCK_SWERVER_PORT = 1080;

    /** time out */
    private final long timeout = 5;

    /**
     * テストクラス初期処理
     *
     * @throws Exception 処理時例外
     */
    @BeforeAll
    public static void beforeClass() throws Exception {
        // mock server start.
        mockServer = ClientAndServer.startClientAndServer(MOCK_SWERVER_PORT);
    }

    /**
     * テスト初期設定
     */
    @BeforeEach
    public void beforeMethod() {

        // create instance
        target = new HttpConnection(timeout);
        // set field parameter
        ReflectionTestUtils.setField(target, "readTimeout", timeout);

        // mock server re-start.
        mockServer.reset();
    }

    /**
     * {@link HttpConnection#postJson(String, Object, Class)}のテスト
     */
    @Test
    @SuppressWarnings("resource")
    @DisplayName("mock serverの利用サンプル")
    void testPostJsonURIRequestDtoClassOfResponseDto() {

        // set mock server.
        TestHttpResponseDto response = new TestHttpResponseDto();
        response.setResultCode("01");

        // tips. use SuppressWarnings resource
        new MockServerClient(MOCK_SERVER_HOST, MOCK_SWERVER_PORT)
                .when(
                        org.mockserver.model.HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/hogehoge"))
                .respond(
                        org.mockserver.model.HttpResponse.response(JsonHelper.toJson(response))
                                // withDelay : time out settings
                                // .withDelay(TimeUnit.SECONDS, timeout + 1)
                                .withStatusCode(HttpStatus.OK.value()));

        // setting test method parameter.
        final String url = "http://" + MOCK_SERVER_HOST + ":" + MOCK_SWERVER_PORT + "/hogehoge";

        TestHttpRequestDto request = new TestHttpRequestDto();
        request.setId(1);
        request.setName("hoge");

        TestHttpResponseDto actualResponse = target.postJson(url, request, TestHttpResponseDto.class);

        // assertion
        // mock server からの返却値が利用される
        assertNotNull(actualResponse);
        assertEquals(response.getResultCode(), actualResponse.getResultCode());
    }

    /**
     * {@link HttpConnection#postJson(java.net.URI, Object, Class)}のテスト
     */
    @Test
    @DisplayName("利用サンプルのオーバーロードメソッド")
    @Disabled("string to uri のオーバーロードなので、サンプルとしては同じになるため実装しない")
    void testPostJsonStringRequestDtoClassOfResponseDto() {
        fail("Not yet implemented");
    }

}
