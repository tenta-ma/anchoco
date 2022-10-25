package org.tantama.anchoco.springcrib.univapay;

import com.univapay.sdk.models.common.ChargeId;
import com.univapay.sdk.models.common.RefundId;
import com.univapay.sdk.models.common.StoreId;
import com.univapay.sdk.models.response.charge.Charge;
import com.univapay.sdk.models.response.refund.Refund;
import com.univapay.sdk.types.ProcessingMode;
import com.univapay.sdk.types.RefundReason;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * {@link Univapay}のテスト
 */
@DisplayName("Univapayへの接続")
class UnivapayTest {

    /** テスト対象 */
    private Univapay target;

    /** mock server */
    private static ClientAndServer mockServer;

    /** mock server のhost名 */
    private static final String MOCK_SERVER_HOST = "localhost";

    /** mock server のport */
    private static final int MOCK_SWERVER_PORT = 6180;

    /** mock server のbase path */
    private static final String MOCK_END_POINT = "http://" + MOCK_SERVER_HOST + ":" + MOCK_SWERVER_PORT;

    /** api path */
    private class ApiPath {
        /** create transaction token */
        private static String tokens = "/tokens";
        /** charge */
        private static String charges = "/charges";
        /** store root */
        private static String stores = "/stores";
    }

    /** (univapay)アプリケーショントークン */
    private String jwt;
    /** アプリケーションシークレット */
    private String secret;

    /** 通信タイムアウト秒 */
    private final int timeout = 3;

    /**
     * 初期処理
     */
    @BeforeEach
    public void init() {
        final int strLen = 64;
        secret = RandomStringUtils.randomAlphanumeric(strLen);
        jwt = RandomStringUtils.randomAlphanumeric(strLen);
        target = new Univapay(true, MOCK_END_POINT, timeout, "店舗名称", jwt, secret);
        // mock server re-start.
        mockServer.reset();

    }

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
     * {@link Univapay#charge(String, BigDecimal, String)}のテスト<br>
     * 課金成功時に課金オブジェクトが返却される
     */
    @Test
    @DisplayName("課金")
    void testCharge() {

        final int amount = RandomUtils.nextInt();

        final UUID tokenId = UUID.randomUUID();
        final UUID storeId = UUID.randomUUID();
        final UUID chargeId = UUID.randomUUID();
        final String tokenJson = """
                 {
                  "id": "%s",
                  "payment_type": "card",
                  "mode": "test",
                  "type": "one_time",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                                """.formatted(tokenId.toString());

        final String pendingChargeJson = """
                {
                  "id": "%s",
                  "store_id": "%s",
                  "transaction_token_id": "%s",
                  "requested_amount": %d,
                  "requested_currency": "JPY",
                  "requested_amount_formatted": %d,
                  "status": "pending",
                  "error": null,
                  "mode": "test",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                """.formatted(
            chargeId.toString(),
            storeId.toString(),
            tokenId.toString(),
            amount, amount);

        final String awaitChargeJson = """
                {
                  "id": "%s",
                  "store_id": "%s",
                  "transaction_token_id": "%s",
                  "requested_amount": %d,
                  "requested_currency": "JPY",
                  "requested_amount_formatted": %d,
                  "status": "awaiting",
                  "error": null,
                  "mode": "test",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                """.formatted(
            chargeId.toString(),
            storeId.toString(),
            tokenId.toString(),
            amount, amount);

        // create token
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.tokens))
                .respond(response(tokenJson)
                        .withStatusCode(HttpStatus.OK.value()));
        // charge
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.charges))
                .respond(response(pendingChargeJson)
                        .withStatusCode(HttpStatus.OK.value()));

        // polling
        // path is /stores/{store_id}/charges/{charge_id}
        final String pollingPath = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString();
        mockServer.when(request()
                .withMethod("GET")
                .withPath(pollingPath))
                .respond(response(awaitChargeJson)
                        .withStatusCode(HttpStatus.OK.value()));

        Charge chargesResult = target.charge("open-id", BigDecimal.valueOf(amount), "order-no");
        assertAll("charge", () -> {
            assertEquals(tokenId, chargesResult.getTransactionTokenId().toUUID(), "transaction id");
            assertEquals(storeId, chargesResult.getStoreId().toUUID(), "store id");
            assertEquals(chargeId, chargesResult.getId().toUUID(), "charge id");
            assertEquals(amount, chargesResult.getRequestedAmount().intValue(), "request amount");
        });

    }

    /**
     * {@link Univapay#charge(String, BigDecimal, String)}のテスト<br>
     * 課金結果で、ステータスが不正（処理待ちでない）時にエラーを投げる
     */
    @Test
    @DisplayName("課金:結果のステータスが不正")
    void testChargeInvalidStatus() {

        final int amount = RandomUtils.nextInt();

        final UUID tokenId = UUID.randomUUID();
        final UUID storeId = UUID.randomUUID();
        final UUID chargeId = UUID.randomUUID();
        final String tokenJson = """
                 {
                  "id": "%s",
                  "payment_type": "card",
                  "mode": "test",
                  "type": "one_time",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                                """.formatted(tokenId.toString());

        final String pendingChargeJson = """
                {
                  "id": "%s",
                  "store_id": "%s",
                  "transaction_token_id": "%s",
                  "requested_amount": %d,
                  "requested_currency": "JPY",
                  "requested_amount_formatted": %d,
                  "status": "pending",
                  "error": null,
                  "mode": "test",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                """.formatted(
            chargeId.toString(),
            storeId.toString(),
            tokenId.toString(),
            amount, amount);

        final String awaitChargeJson = """
                {
                  "id": "%s",
                  "store_id": "%s",
                  "transaction_token_id": "%s",
                  "requested_amount": %d,
                  "requested_currency": "JPY",
                  "requested_amount_formatted": %d,
                  "status": "failed",
                  "error": null,
                  "mode": "test",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                """.formatted(
            chargeId.toString(),
            storeId.toString(),
            tokenId.toString(),
            amount, amount);

        // create token
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.tokens))
                .respond(response(tokenJson)
                        .withStatusCode(HttpStatus.OK.value()));
        // charge
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.charges))
                .respond(response(pendingChargeJson)
                        .withStatusCode(HttpStatus.OK.value()));

        // polling
        // path is /stores/{store_id}/charges/{charge_id}
        final String pollingPath = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString();
        mockServer.when(request()
                .withMethod("GET")
                .withPath(pollingPath))
                .respond(response(awaitChargeJson)
                        .withStatusCode(HttpStatus.OK.value()));

        final String orderNo = UUID.randomUUID().toString();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> target.charge("open-id", BigDecimal.valueOf(amount), orderNo));
        assertTrue(exception.getMessage().contains(orderNo), "message has orderNo");
    }

    /**
     * {@link Univapay#charge(String, BigDecimal, String)}のテスト<br>
     * 通信タイムアウト時にエラーが返却される
     */
    @Test
    @DisplayName("課金:タイムアウト")
    @Disabled("最終的なタイムアウトのExceptionが発行されるまで、時間がかかるため、CIとしてはSKIP")
    void testChargeTimeout() {

        final int amount = RandomUtils.nextInt();

        final UUID tokenId = UUID.randomUUID();
        final UUID storeId = UUID.randomUUID();
        final UUID chargeId = UUID.randomUUID();
        final String tokenJson = """
                 {
                  "id": "%s",
                  "payment_type": "card",
                  "mode": "test",
                  "type": "one_time",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                                """.formatted(tokenId.toString());

        final String pendingChargeJson = """
                {
                  "id": "%s",
                  "store_id": "%s",
                  "transaction_token_id": "%s",
                  "requested_amount": %d,
                  "requested_currency": "JPY",
                  "requested_amount_formatted": %d,
                  "status": "pending",
                  "error": null,
                  "mode": "test",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                """.formatted(
            chargeId.toString(),
            storeId.toString(),
            tokenId.toString(),
            amount, amount);

        final String awaitChargeJson = """
                {
                  "id": "%s",
                  "store_id": "%s",
                  "transaction_token_id": "%s",
                  "requested_amount": %d,
                  "requested_currency": "JPY",
                  "requested_amount_formatted": %d,
                  "status": "awaiting",
                  "error": null,
                  "mode": "test",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                """.formatted(
            chargeId.toString(),
            storeId.toString(),
            tokenId.toString(),
            amount, amount);

        // create token
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.tokens))
                .respond(response(tokenJson)
                        .withStatusCode(HttpStatus.OK.value()));
        // charge
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.charges))
                .respond(response(pendingChargeJson)
                        .withStatusCode(HttpStatus.OK.value()));

        // polling
        // path is /stores/{store_id}/charges/{charge_id}
        final String pollingPath = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString();
        mockServer.when(request()
                .withMethod("GET")
                .withPath(pollingPath))
                .respond(response(awaitChargeJson)
                        // タイムアウト設定
                        .withDelay(TimeUnit.SECONDS, timeout + 1)
                        .withStatusCode(HttpStatus.OK.value()));

        assertThrows(RuntimeException.class,
            () -> target.charge("open-id", BigDecimal.valueOf(amount), "order-no"));
    }

    /**
     * {@link Univapay#charge(String, BigDecimal, String)}のテスト<br>
     * トランザクショントークン作成エラー時にエラーが返却される
     */
    @Test
    @DisplayName("課金:トークン作成失敗")
    void testChargeCreateTokenFailure() {

        final HttpStatus errorStatus = HttpStatus.UNAUTHORIZED;

        final int amount = RandomUtils.nextInt();
        final String errorJson = """
                {
                  status: "error",
                  code: "ERROR_HAPPEN"
                  errors: []
                }
                """;

        // create token error
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.tokens))
                .respond(response(errorJson)
                        .withStatusCode(errorStatus.value()));

        assertThrows(RuntimeException.class, () -> target.charge("open-id", BigDecimal.valueOf(amount), "order-no"));
    }

    /**
     * {@link Univapay#charge(String, BigDecimal, String)}のテスト<br>
     * 課金エラー時にエラーが返却される
     */
    @Test
    @DisplayName("課金:課金失敗")
    void testChargeFailure() {

        final HttpStatus errorStatus = HttpStatus.BAD_REQUEST;

        final int amount = RandomUtils.nextInt();

        final UUID tokenId = UUID.randomUUID();
        final String tokenJson = """
                 {
                  "id": "%s",
                  "payment_type": "card",
                  "mode": "test",
                  "type": "one_time",
                  "created_on": "2022-06-27T03:27:40.928378Z"
                }
                                """.formatted(tokenId.toString());
        final String errorJson = """
                {
                  status: "error",
                  code: "ERROR_HAPPEN"
                  errors: []
                }
                """;

        // create token
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.tokens))
                .respond(response(tokenJson)
                        .withStatusCode(HttpStatus.OK.value()));
        // charge
        mockServer.when(request()
                .withMethod("POST")
                .withPath(ApiPath.charges))
                .respond(response(errorJson)
                        .withStatusCode(errorStatus.value()));

        assertThrows(RuntimeException.class, () -> target.charge("open-id", BigDecimal.valueOf(amount), "order-no"));

    }

    /**
     * {@link Univapay#getIssuerToken(StoreId, ChargeId)}のテスト<br>
     * テストモードの場合、空が返却される。
     */
    @Test
    @DisplayName("イシュアトークン取得:テストモード")
    void testGetIssuerTokenModeTest() {

        final ProcessingMode mode = ProcessingMode.TEST;

        final StoreId storeId = new StoreId(UUID.randomUUID());
        final ChargeId chargeId = new ChargeId(UUID.randomUUID());

        final String issuerToken = """
                {
                  "issuer_token": "test-issuer-token",
                  "call_method": "http_get"
                }
                """;

        // getIssuer
        // path is /stores/{store_id}/charges/{charge_id}/issuerToken
        final String url = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString() + "/issuerToken";
        mockServer.when(request()
                .withMethod("GET")
                .withPath(url))
                .respond(response(issuerToken)
                        .withStatusCode(HttpStatus.OK.value()));

        assertTrue(target.getIssuerToken(storeId, chargeId, mode).isEmpty(), "issuer_token");
    }

    /**
     * {@link Univapay#getIssuerToken(StoreId, ChargeId)}のテスト<br>
     * 本番モードの場合、イシュアトークンが返却される。
     */
    @Test
    @DisplayName("イシュアトークン取得:本番モード")
    void testGetIssuerTokenModeLive() {

        final ProcessingMode mode = ProcessingMode.LIVE;

        final StoreId storeId = new StoreId(UUID.randomUUID());
        final ChargeId chargeId = new ChargeId(UUID.randomUUID());

        final int strLen = 32;
        PaymentRequest mockPayment = new PaymentRequest();
        mockPayment.setNonceStr(RandomStringUtils.randomAlphanumeric(strLen));
        mockPayment.setPackage(RandomStringUtils.randomAlphanumeric(strLen));
        mockPayment.setTimeStamp(RandomStringUtils.randomAlphanumeric(strLen));
        mockPayment.setSignType(RandomStringUtils.randomAlphanumeric(strLen));
        mockPayment.setPaySign(RandomStringUtils.randomAlphanumeric(strLen));

        final String issuerToken = """
                {
                    "issuer_token":
                        "{
                        \\"nonceStr\\":\\"%s\\",
                        \\"package\\":\\"%s\\",
                        \\"timeStamp\\":\\"%s\\",
                        \\"signType\\":\\"%s\\",
                        \\"paySign\\":\\"%s\\"}"
                    ,
                    "call_method": "http_get"
                }
                """
                .formatted(mockPayment.getNonceStr(),
                    mockPayment.getPackage(),
                    mockPayment.getTimeStamp(),
                    mockPayment.getSignType(),
                    mockPayment.getPaySign());

        // getIssuer
        // path is /stores/{store_id}/charges/{charge_id}/issuerToken
        final String url = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString() + "/issuerToken";
        mockServer.when(request()
                .withMethod("GET")
                .withPath(url))
                .respond(response(issuerToken)
                        .withStatusCode(HttpStatus.OK.value()));

        assertAll("issuerToken", () -> {
            Optional<PaymentRequest> opt = target.getIssuerToken(storeId, chargeId, mode);
            assertTrue(opt.isPresent(), "issuer_token valid");
            PaymentRequest payment = opt.get();

            assertEquals(mockPayment.getNonceStr(), payment.getNonceStr(), "nonceStr");
            assertEquals(mockPayment.getPackage(), payment.getPackage(), "package");
            assertEquals(mockPayment.getTimeStamp(), payment.getTimeStamp(), "timeStamp");
            assertEquals(mockPayment.getSignType(), payment.getSignType(), "signType");
            assertEquals(mockPayment.getPaySign(), payment.getPaySign(), "paySign");
        });
    }

    /**
     * {@link Univapay#getIssuerToken(StoreId, ChargeId)}のテスト<br>
     * 取得失敗時、エラーが返却される
     */
    @Test
    @DisplayName("イシュアトークン取得:取得失敗")
    void testGetIssuerTokenFailure() {

        final HttpStatus errorStatus = HttpStatus.BAD_REQUEST;

        final StoreId storeId = new StoreId(UUID.randomUUID());
        final ChargeId chargeId = new ChargeId(UUID.randomUUID());

        final String errorResponse = """
                {
                  code:INVALID_CHARGE_STATUS,
                  status:error, details:[]}
                }
                """;

        // getIssuer
        // path is /stores/{store_id}/charges/{charge_id}/issuerToken
        final String url = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString() + "/issuerToken";
        mockServer.when(request()
                .withMethod("GET")
                .withPath(url))
                .respond(response(errorResponse)
                        .withStatusCode(errorStatus.value()));

        assertThrows(RuntimeException.class, () -> target.getIssuerToken(storeId, chargeId, ProcessingMode.TEST));
    }

    /**
     * {@link Univapay#refunds(ChargeId, StoreId, BigDecimal, String, RefundReason, String)}のテスト<br>
     * 返金成功時、返金オブジェクトが返却される
     */
    @Test
    @DisplayName("全額返金")
    void testFullRefunds() {

        final RefundId refundId = new RefundId(UUID.randomUUID());
        final StoreId storeId = new StoreId(UUID.randomUUID());
        final ChargeId chargeId = new ChargeId(UUID.randomUUID());
        final int amount = RandomUtils.nextInt();

        final String refundsJson = """
                {
                  "id": "%s",
                  "charge_id": "%s",
                  "status": "pending",
                  "amount": %d,
                  "currency": "JPY",
                  "amount_formatted": %d,
                  "reason": "customer_request",
                  "error": null,
                  "mode": "test",
                  "created_on": "2018-07-13T02:55:00.07367Z"
                }
                """.formatted(
            refundId.toString(),
            chargeId.toString(),
            amount, amount);

        // getIssuer
        // path is /stores/{storeId}/charges/{chargeId}/refunds
        final String url = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString() + "/refunds";
        mockServer.when(request()
                .withMethod("POST")
                .withPath(url))
                .respond(response(refundsJson)
                        .withStatusCode(HttpStatus.OK.value()));

        Refund refund = target.refunds(chargeId, storeId, BigDecimal.valueOf(amount), refundsJson,
            RefundReason.CUSTOMER_REQUEST, "返金の理由");

        assertAll("refund", () -> {
            assertEquals(refundId.toUUID(), refund.getId().toUUID(), "refund id");
            assertEquals(chargeId.toUUID(), refund.getChargeId().toUUID(), "chargeId id");
            assertEquals(amount, refund.getAmount().intValue(), "refund amount");
        });

    }

    /**
     * {@link Univapay#refunds(ChargeId, StoreId, BigDecimal, String, RefundReason, String)}のテスト<br>
     * 返金失敗時、エラーが返却される
     */
    @Test
    @DisplayName("全額返金:返金失敗")
    void testFullRefundsFailure() {

        final StoreId storeId = new StoreId(UUID.randomUUID());
        final ChargeId chargeId = new ChargeId(UUID.randomUUID());
        final int amount = RandomUtils.nextInt();

        final HttpStatus errorStatus = HttpStatus.BAD_REQUEST;

        final String refundsJson = """
                {
                  code:REFUND_EXCEEDS_CHARGE_AMOUNT,
                  status:error, details:[]
                }
                """;

        // getIssuer
        // path is /stores/{storeId}/charges/{chargeId}/refunds
        final String url = ApiPath.stores + "/" + storeId.toString()
                + ApiPath.charges + "/" + chargeId.toString() + "/refunds";
        mockServer.when(request()
                .withMethod("POST")
                .withPath(url))
                .respond(response(refundsJson)
                        .withStatusCode(errorStatus.value()));

        assertThrows(RuntimeException.class,
            () -> target.refunds(chargeId, storeId, BigDecimal.valueOf(amount), refundsJson, RefundReason.CUSTOMER_REQUEST, "返金の理由"));
    }

    /**
     * {@link Univapay#partialRefunds(ChargeId, StoreId, BigDecimal, String, RefundReason, String)}のテスト<br>
     * 返金成功時、返金オブジェクトが返却される
     */
    @Test
    @DisplayName("一部返金")
    @Disabled("全額返金とテスト処理が重複(mockのレスポンス依存)するため、割愛")
    void testPartialRefunds() {
        fail("まだ実装されていません");
    }

    /**
     * {@link Univapay#partialRefunds(ChargeId, StoreId, BigDecimal, String, RefundReason, String)}のテスト<br>
     * 返金失敗時、エラーが返却される
     */
    @Test
    @DisplayName("一部返金:返金失敗")
    @Disabled("全額返金とテスト処理が重複(mockのレスポンス依存)するため、割愛")
    void testPartialRefundsFailure() {
        fail("まだ実装されていません");
    }
}
