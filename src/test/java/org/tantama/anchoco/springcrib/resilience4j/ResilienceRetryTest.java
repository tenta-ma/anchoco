package org.tantama.anchoco.springcrib.resilience4j;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link ResilienceRetry}のテストクラス
 */
class ResilienceRetryTest {

    /** テスト対象クラス */
    private ResilienceRetry target;

    /** 最大実行回数(リトライなしの場合1) */
    private static final int MAX_ATTEMPTS = 2;

    /** リトライ時のインターバル(s) */
    private static final long RETRY_INTERVAL = 1;

    /**
     * 初期設定
     */
    @BeforeEach
    public void beforeMethod() {
        target = new ResilienceRetry();
        ReflectionTestUtils.setField(target, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(target, "retryInterval", RETRY_INTERVAL);
        target.init();
    }

    /**
     * {@link ResilienceRetry#init()}のテストクラス
     */
    @Test
    @DisplayName("初期設定")
    @Disabled("before eachで実施するため、独自テストは行わない")
    void testInit() {
        fail("個別のテストはしない");
    }

    /**
     * {@link ResilienceRetry#excuteRetry(Supplier)}のテスト<br>
     * 正常に動作終了する場合、エラーは発生しない
     */
    @Test
    @DisplayName("リトライを行わない正常動作")
    void testExcuteRetrySuccss() {

        TestSupplier testClass = new TestSupplier();
        Supplier<String> sup = () -> testClass.testSupplier();

        final String expected = "hoge";
        assertEquals(expected, target.excuteRetry(sup));
    }

    /**
     * {@link ResilienceRetry#excuteRetry(Supplier)}のテスト<br>
     * 規定の回数の規定のエラーまではリトライを行い処理を実施する
     */
    @Test
    @DisplayName("規定回数までのリトライを行う正常動作")
    void testExcuteRetryOneRetry() {

        final Exception retryError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        TestSupplier mockClass = Mockito.mock(TestSupplier.class);
        Mockito
                // 規定回数までの規定エラー
                .doThrow(retryError)
                // 最終的には正常
                .doReturn("hoge")
                .when(mockClass).testSupplier();

        Supplier<String> sup = () -> mockClass.testSupplier();

        final String expected = "hoge";
        assertEquals(expected, target.excuteRetry(sup));
    }

    /**
     * {@link ResilienceRetry#excuteRetry(Supplier)}のテスト<br>
     * 規定の回数の規定のエラーを超えた場合、その発生したエラーを投げる
     */
    @Test
    @DisplayName("規定回数のリトライを超えるエラー")
    void testExcuteRetryOverRetry() {

        final Exception retryError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        TestSupplier mockClass = Mockito.mock(TestSupplier.class);
        Mockito
                // 規定回数をこえる規定エラー
                .doThrow(retryError)
                .doThrow(retryError)
                // 最終的には正常
                .doReturn("hoge")
                .when(mockClass).testSupplier();

        Supplier<String> sup = () -> mockClass.testSupplier();

        // 正常にならず、発生したエラーになる
        assertThrows(HttpServerErrorException.class, () -> target.excuteRetry(sup));
    }

    /**
     * {@link ResilienceRetry#excuteRetry(Supplier)}のテスト<br>
     * リトライ対象のエラーでない場合、その発生したエラーをリトライせずに投げる
     */
    @Test
    @DisplayName("リトライ対象でないエラー")
    void testExcuteRetryNoRetryError() {

        final Exception noRetryError = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        TestSupplier mockClass = Mockito.mock(TestSupplier.class);
        Mockito
                // 規定回数のエラー
                .doThrow(noRetryError)
                // 最終的には正常
                .doReturn("hoge")
                .when(mockClass).testSupplier();

        Supplier<String> sup = () -> mockClass.testSupplier();

        // 正常にならず、発生したエラーになる
        assertThrows(HttpClientErrorException.class, () -> target.excuteRetry(sup));
    }

}
