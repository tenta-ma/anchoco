package org.tantama.anchoco.springcrib.http;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * test用のリクエストdto
 */
@Getter
@Setter
class TestHttpRequestDto implements Serializable {

    /** シリアルID */
    private static final long serialVersionUID = 1L;

    /** id */
    private Integer id;

    /** name */
    private String name;

}
