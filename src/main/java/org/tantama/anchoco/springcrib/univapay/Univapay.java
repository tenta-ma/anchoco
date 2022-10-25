package org.tantama.anchoco.springcrib.univapay;

import com.univapay.sdk.UnivapaySDK;
import com.univapay.sdk.models.common.CallMethod;
import com.univapay.sdk.models.common.ChargeId;
import com.univapay.sdk.models.common.OnlinePayment;
import com.univapay.sdk.models.common.StoreId;
import com.univapay.sdk.models.common.TransactionTokenId;
import com.univapay.sdk.models.common.auth.AppJWTStrategy;
import com.univapay.sdk.models.errors.UnivapayException;
import com.univapay.sdk.models.response.IssuerToken;
import com.univapay.sdk.models.response.charge.Charge;
import com.univapay.sdk.models.response.refund.Refund;
import com.univapay.sdk.models.response.transactiontoken.TransactionTokenWithData;
import com.univapay.sdk.settings.UnivapaySettings;
import com.univapay.sdk.types.ChargeStatus;
import com.univapay.sdk.types.MetadataMap;
import com.univapay.sdk.types.ProcessingMode;
import com.univapay.sdk.types.RefundReason;
import com.univapay.sdk.types.TransactionTokenType;
import com.univapay.sdk.types.brand.OnlineBrand;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tantama.anchoco.springcrib.helper.JsonHelper;

/**
 * Univapayからwechatpayを利用する処理
 *
 * @see <a href="https://github.com/univapay/univapay-java-sdk">github</a>
 */
@Slf4j
@Component
public class Univapay {

    /** univapay 接続SDK */
    private UnivapaySDK univapay;

    /** MetadataMapの注文番号キー */
    private static final String ORDER_NO_KEY = "order_no";

    /** 店名 */
    private final String shopName;

    /** Univapay接続先のホスト */
    private final String endPoint;

    /** Univapayの接続タイムアウト(秒) */
    private final int timeoutSeconds;

    /** MetadataMapの店舗名 */
    private static final String SHOP_NAME_KEY = "shop_name";

    /**
     * 通貨単位<br>
     * 日本円でのみ取り扱う
     */
    private static final String CURRENCY = Currency.getInstance(Locale.JAPAN).getCurrencyCode();

    /**
     * コンストラクタ
     *
     * @param enabledSecretsManager secret managerの利用可否、ローカルなどawsを利用できない場合はfalse
     * @param endPoint              Univapay接続先のホスト。defaultは{@literal https://api.univapay.com}
     * @param timeoutSeconds        接続タイムアウト(秒)
     * @param awsSecretManager      シークレットマネージャ
     */
    public Univapay(
            @Value("${aws.secretsmanager.enabled:false}") boolean enabledSecretsManager,
            @Value("${univapay.endpoint:https://api.univapay.com}") String endPoint,
            @Value("${univapay.timeout-second:5}") int timeoutSeconds,
            @Value("${univapay.shop-name}") String shopName,
            @Value("${univapay.jwt}") String jwt,
            @Value("${univapay.secret}") String secret) {
        this.endPoint = endPoint;
        this.timeoutSeconds = timeoutSeconds;
        this.shopName = shopName;
        AppJWTStrategy authStrategy = new AppJWTStrategy(jwt, secret);
        UnivapaySettings settings = new UnivapaySettings()
                .withEndpoint(this.endPoint)
                .withTimeoutSeconds(this.timeoutSeconds);
        this.univapay = UnivapaySDK.create(authStrategy, settings);
    }

    /**
     * 課金を行う<br>
     * メタデータとして注文番号を設定する
     *
     * @param openid       user識別子のopenid
     * @param chargeAmount 課金金額
     * @param orderNo      注文番号
     * @return 課金結果
     * @throws UncheckedIOException       http接続時のIOエラー
     * @throws UncheckedUnivapayException 課金失敗
     * @throws UnivapayFailureException   ポーリング結果でawait(処理待ち)にならない場合
     * @throws RequestTimeOutException    ポーリング中のタイムアウト
     */
    public Charge charge(String openid, BigDecimal chargeAmount, String orderNo) {
        log.debug("charge start.");

        OnlinePayment opay = new OnlinePayment(OnlineBrand.WE_CHAT);
        opay.withUserIdentifier(openid).withCallMethod(CallMethod.SDK);

        try {
            TransactionTokenWithData transactionToken = univapay.createTransactionToken(opay, TransactionTokenType.ONE_TIME)
                    .build()
                    .dispatch();
            TransactionTokenId transactionId = transactionToken.getId();
            log.debug("transaction id is {}", transactionId);
            MetadataMap metadata = new MetadataMap();
            metadata.put(ORDER_NO_KEY, orderNo);
            metadata.put(SHOP_NAME_KEY, shopName);
            Charge charge = univapay.createCharge(transactionId, chargeAmount.toBigInteger(), CURRENCY)
                    .withMetadata(metadata)
                    .build()
                    .dispatch();

            log.debug("charge id is {}", charge.getId());

            // 課金状態の未確定解決までポーリング
            Charge polling = univapay.chargeCompletionMonitor(charge.getStoreId(), charge.getId()).await();

            if (polling.getStatus() != ChargeStatus.AWAITING) {
                // AWAITINGでない場合を想定していないため、エラーにする
                // TOOD: 本番モードで試していないため(机上での考慮のみ)、もしかしたら何かあるかもしれない。要確認。
                throw new RuntimeException("order no : " + orderNo + " の課金結果ステータスが不正 : " + polling.getStatus());
            }

            return polling;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (UnivapayException e) {
            log.error("charge faild.  {} ", e.toString());
            throw new RuntimeException(e);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            log.debug("charge finished.");

        }
    }

    /**
     * イシュアトークンの取得
     * 
     * @param storeId  店舗ID
     * @param chargeId 課金ID
     * @param mode     モード、テストか本番か
     * @return イシュアトークン。テストモードの場合null
     */
    public Optional<PaymentRequest> getIssuerToken(StoreId storeId, ChargeId chargeId, ProcessingMode mode) {

        // notice. テストモードでは test-issuer-token というjsonでない文字列が設定される
        // 本番モードの場合 wx.requestPaymentで必要なパラメータ(e.g. nonce)がjson形式で返却される

        log.debug("get IssuerToken start.");

        try {

            IssuerToken issuer = univapay.getIssuerToken(storeId, chargeId).dispatch();

            if (mode == ProcessingMode.TEST) {
                return Optional.empty();
            }

            final String paymentJson = issuer.getIssuerToken();
            return Optional.ofNullable(JsonHelper.toDto(paymentJson, PaymentRequest.class));

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (UnivapayException e) {
            log.error("get IssuerToken faild.  {} ", e.toString());
            throw new RuntimeException(e);
        } finally {
            log.debug("get IssuerToken end.");
        }

    }

    /**
     * 返金額に対して全額返金を行う
     *
     * @param chargeId 課金ID
     * @param storeId  店舗ID
     * @param amount   課金キャンセル額
     * @param orderNo  注文No
     * @param reason   返金理由
     * @param message  返金を行う理由の詳細内容
     * @return 返金結果
     */
    public Refund refunds(ChargeId chargeId, StoreId storeId, BigDecimal amount,
            String orderNo, RefundReason reason, String message) {

        log.debug("refunds start.");

        try {

            MetadataMap metadata = new MetadataMap();
            metadata.put(ORDER_NO_KEY, orderNo);
            metadata.put(SHOP_NAME_KEY, shopName);

            Refund ref = univapay.createRefund(storeId, chargeId, amount.toBigInteger(), CURRENCY, reason)
                    .withMetadata(metadata)
                    .withMessage(message)
                    .build()
                    .dispatch();

            log.debug("Refund id is {}", ref.getId());

            return ref;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (UnivapayException e) {
            log.error("refunds faild.  {} ", e.toString());
            throw new RuntimeException(e);
        } finally {
            log.debug("refunds finished.");
        }
    }

}
