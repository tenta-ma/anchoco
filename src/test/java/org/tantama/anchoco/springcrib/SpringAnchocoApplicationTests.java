package org.tantama.anchoco.springcrib;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link SpringAnchocoApplication}のテストクラス
 */
@SpringBootTest
class SpringAnchocoApplicationTests {

    /**
     * 起動テスト
     */
    @Test
    @DisplayName("起動")
    @Disabled("aws secret manager取得など、localでは実行不能なのもあるので、実行させない")
    void contextLoads() {
        fail("実行できない");
    }

}
