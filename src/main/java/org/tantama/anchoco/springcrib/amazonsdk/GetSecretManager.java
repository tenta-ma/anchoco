package org.tantama.anchoco.springcrib.amazonsdk;

import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tantama.anchoco.springcrib.helper.JsonHelper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * amazon secret managerからgetする感じ
 */
@Slf4j
@Component
public class GetSecretManager {

    // notice. このクラスはamazon secret managerの「その他のシークレット」に対して
    // 値の取得を行う実装である

    // また、これだけではなく aws 側の設定として
    // IAMに対して、このsecret managerが取得可能である権限を設定する必要がある

    // また、amazon secret managerは呼び出し毎にcost(money)がかかる(恐れがある)ため
    // コンストラクタで初回読み込みを行い、後はメモリ保持などでcostを抑える実装が必要となる

    /** x-api-key */
    @Getter
    private final String xApiKey;

    /**
     * 初期化処理<br>
     * secret managerから値を取得する
     *
     * @param xApiKeySecretName {@link #xApiKey}のsecret name
     * @param region            aws での利用region
     * @see software.amazon.awssdk.regions.Region
     */
    public GetSecretManager(@Value("${secret.manager.secretname.xapikey}") String xApiKeySecretName,
            @Value("${secret.manager.region:ap-northeast-1}") String region) {

        log.debug("get from aws secret service start...");

        try (SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(Region.of(region)).build()) {
            this.xApiKey = getValue(secretsClient, xApiKeySecretName);
            log.trace("api key is {}", this.xApiKey);
        }
    }

    /**
     * awsからsecret managerの値を取得する
     *
     * @param secretsClient secret manager client
     * @param secretName    secret name
     * @return nameに対しての値
     */
    private String getValue(SecretsManagerClient secretsClient, String secretName) {

        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
        GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);

        // tips. GetSecretValueResponse#secretString()
        // ではsecretname:valueのjson文字列が取得できるようである
        // f.e. {"x-api-key":"xxxxxxxx"} というstring

        // awsの設定が悪い可能性があるが、調べてみてがvalueのみ取得する方法が不明だったため、jsonをMapに変換して取得することにする
        String secretString = valueResponse.secretString();
        if (secretString == null) {
            return null;
        }

        Map<String, String> secretNameValue = JsonHelper.toMap(secretString);
        return secretNameValue.get(secretName);
    }
}
