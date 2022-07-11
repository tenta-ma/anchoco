package org.tantama.anchoco.springcrib.http;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * test用のレスポンスdto
 */
@Getter
@Setter
class TestHttpResponseDto implements Serializable {

    /** シリアルID */
    private static final long serialVersionUID = 1L;

    /** 結果コード */
    private String resultCode;

}
