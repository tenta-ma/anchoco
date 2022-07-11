package org.tantama.anchoco.springcrib.amazonsdk;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link GetSecretManager}のテストクラス
 */
class GetSecretManagerTest {

    // tips. aws接続になるため、基本的にはテストしない

    /**
     * {@link GetSecretManager}のコンストラクタのテスト
     */
    @Test
    @DisplayName("初期処理")
    @Disabled("awsへのアクセスになるため、実施不能")
    void testGetSecretManager() {
        fail("awsへの接続になるため、テスト不能");
    }

}
