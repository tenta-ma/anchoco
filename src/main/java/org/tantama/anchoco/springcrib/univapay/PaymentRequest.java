package org.tantama.anchoco.springcrib.univapay;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * univapay決済のための情報<br>
 * notice. swagger-codegen-cli から出力したクラス
 */
public class PaymentRequest {

    @JsonProperty("nonceStr")
    private String nonceStr = null;

    @JsonProperty("package")
    private String _package = null;

    @JsonProperty("timeStamp")
    private String timeStamp = null;

    @JsonProperty("signType")
    private String signType = null;

    @JsonProperty("paySign")
    private String paySign = null;

    public PaymentRequest nonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
        return this;
    }

    /**
     * 32 ビット以下のランダムな文字列
     * 
     * @return nonceStr
     **/
    public String getNonceStr() {
        return nonceStr;
    }

    public void setNonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
    }

    public PaymentRequest _package(String _package) {
        this._package = _package;
        return this;
    }

    /**
     * アプレットの注文インターフェイスによって返される prepay_id パラメータ値
     * 
     * @return _package
     **/
    public String getPackage() {
        return _package;
    }

    public void setPackage(String _package) {
        this._package = _package;
    }

    public PaymentRequest timeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    /**
     * 中国標準時のエポック秒(1970.01.01 00:00:00からの秒数)
     * 
     * @return timeStamp
     **/
    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public PaymentRequest signType(String signType) {
        this.signType = signType;
        return this;
    }

    /**
     * 署名方法
     * 
     * @return signType
     **/
    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public PaymentRequest paySign(String paySign) {
        this.paySign = paySign;
        return this;
    }

    /**
     * デジタル署名
     * 
     * @return paySign
     **/
    public String getPaySign() {
        return paySign;
    }

    public void setPaySign(String paySign) {
        this.paySign = paySign;
    }
}
