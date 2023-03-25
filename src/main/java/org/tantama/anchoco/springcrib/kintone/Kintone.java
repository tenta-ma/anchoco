package org.tantama.anchoco.springcrib.kintone;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kintone.client.KintoneClient;
import com.kintone.client.KintoneClientBuilder;
import com.kintone.client.api.common.BulkRequestsRequest;
import com.kintone.client.api.record.UpdateRecordRequest;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.RecordForUpdate;
import com.kintone.client.model.record.SingleLineTextFieldValue;

import lombok.extern.slf4j.Slf4j;

/**
 * kintone cybozuの操作
 */
@Slf4j
@Component
public class Kintone {

    /** ログインID */
    private final String user = "キントーンのログインユーザー名";
    /** パスワード */
    private final String password = "キントーンのログインパスワード";

    /**
     * ドメイン
     *
     * e.g. https://「smaple」.cybozu.com
     */
    private final String domain = "smaple";

    /**
     * APP ID<br>
     * キントーンのアプリのページ行ったときにくっついているID
     */
    private final int appId = 46;

    /** kintone API のURL */
    private final String url = "https://" + domain + ".cybozu.com";

    /**
     * 取得処理のサンプル<br>
     * フィールドのイメージは、顧客サンプルのアプリ想定
     *
     * @return Dtoに整形したkintoneのリスト
     * @throws IOException kintoneへの接続エラー
     */
    public List<KintonePojo> getRecord() throws IOException {

        try (KintoneClient client = KintoneClientBuilder.create(url).authByPassword(user, password).build()) {

            // 指定条件無しで全レコード取得
            // List<Record> allRecords = client.record().getRecords(appId);

            // レコードIDを指定して取得
            // final int recordId = 32;
            // Record oneRecord = client.record().getRecord(appId, recordId);

            // 取得フィールドを制限して取得
            final int limit = 10;
            final int offset = 0;
            List<String> fields = List.of("$id", "レコード番号", "会社名", "郵便番号", "更新日時");
            List<Record> anyColumnRecords = client.record().getRecords(appId, fields, limit, offset);

            // 整形
            return anyColumnRecords.stream().map(record -> {
                KintonePojo dto = new KintonePojo();

                // キントーン側のフィールド形式に合わせてgetXXXXXメソッドで取得する
                dto.setCompanyName(record.getSingleLineTextFieldValue("会社名"));
                dto.setCompanyName(record.getSingleLineTextFieldValue("郵便番号"));
                dto.setUpdateDatetime(record.getDateTimeFieldValue("更新日時"));

                // レコードIDフィールドは指定は以下で取得(フィールド指定は「レコード番号」）できる
                // IDフィールドとは違う
                dto.setRecordId(Integer.parseInt(record.getRecordNumberFieldValue()));
                // f.g. id(フィールド指定は「$id」)
                log.info("id is {}", record.getId());

                return dto;
            }).toList();
        }
    }

    /**
     * 更新処理のサンプル
     *
     * @param dtoList 更新を行うdto
     * @throws IOException
     */
    public void updateRecords(List<KintonePojo> dtoList) throws IOException {

        try (KintoneClient client = KintoneClientBuilder.create(url).authByPassword(user, password).build()) {

            // 単一の更新
            updateOneRecord(client, dtoList.get(0));

            // 複数の更新
            updateListRecord(client, dtoList);
        }
    }

    /**
     * キントーンへ更新をする
     *
     * @param client キントーンAPI実行部品
     * @param dto    更新Dto
     */
    private void updateOneRecord(KintoneClient client, KintonePojo dto) {

        // 更新したいフィールドの設定
        Record record = new Record();
        // 取得とは逆に、Fieldを指定して作成する
        record.putField("会社名", new SingleLineTextFieldValue(dto.getCompanyName()));
        record.putField("郵便番号", new SingleLineTextFieldValue(dto.getZipCode()));
        // appId, recordIdを指定して更新
        client.record().updateRecord(appId, dto.getRecordId(), record);
    }

    /**
     * キントーンへ更新をする
     *
     * @param client キントーンAPI実行部品
     * @param dto    更新Dto
     */
    private void updateListRecord(KintoneClient client, List<KintonePojo> dtoList) {

        List<RecordForUpdate> updateList = dtoList.stream().map(dto -> {
            // 更新したいフィールドの設定
            Record record = new Record();
            // 取得とは逆に、Fieldを指定して作成する
            record.putField("会社名", new SingleLineTextFieldValue(dto.getCompanyName()));
            record.putField("郵便番号", new SingleLineTextFieldValue(dto.getZipCode()));
            // 更新内容とrecordIdを紐付けたクラスRecordForUpdateを作成する
            RecordForUpdate recordForUpdate = new RecordForUpdate(dto.getRecordId(), record);
            return recordForUpdate;
        }).toList();

        // appId, 更新リストを指定して更新
        client.record().updateRecords(appId, updateList);
    }


  /**
   * キントーンへ複数アプリに対して同時に更新する<br>
   * f.g. この実装例では2種類のアプリに対してbulk updateを行う
   *
   * @param appId01 1つ目の更新のアプリID
   * @param recordId01　1つ目の更新のレコードID
   * @param updateRecord01　1つ目の更新内容（更新内容は設定済み）
   * @param appId02 2つ目の更新のアプリID
   * @param recordId02 2つ目の更新のレコードID
   * @param updateRecord02 2つ目の更新内容（更新内容は設定済み）
   * @throws IOException キントーン接続エラー
   */
    public void updateBulkRecord(long appId01, long recordId01,  Record updateRecord01,
            long appId02, long recordId02,  Record updateRecord02) throws IOException {

         try (KintoneClient client = KintoneClientBuilder.create(url).authByPassword(user, password).build()) {

             // 更新するデータを作成
             UpdateRecordRequest updateRequest01 = new UpdateRecordRequest()
                     .setApp(appId01).setId(recordId01).setRecord(updateRecord01);
             UpdateRecordRequest updateRequest02 = new UpdateRecordRequest()
                     .setApp(appId02).setId(recordId02).setRecord(updateRecord02);

             // bulk処理用リクエストに更新データ登録
             BulkRequestsRequest bulkRequest = new BulkRequestsRequest();
             // notice. registerUpdateRecord[s]というメソッドあのため利用に注意すること
             bulkRequest.registerUpdateRecord(updateRequest01);
             bulkRequest.registerUpdateRecord(updateRequest02);

             // キントーンへ更新処理を行う
             client.bulkRequests(bulkRequest);
         }


    }
}
