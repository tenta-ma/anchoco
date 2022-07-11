package org.tantama.anchoco.springcrib.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * json変換Helper
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonHelper {

    /** オブジェクトMapper */
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        // null フィールドは出力させない
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * オブジェクトをjsonに変換する
     *
     * @param <T> jsonに変換するクラス
     * @param obj jsonに変換するオブジェクト
     * @return json文字列
     */
    public static <T> String toJson(T obj) {

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // tips. 実際に利用する場合は適当なクラスにwrapする
            throw new UncheckedIOException(e);
        }
    }

    /**
     * jsonをオブジェクトに変換<br>
     * e.g. {@link java.util.Collection}では利用しないこと
     *
     * @param <T>  変換先クラス
     * @param json json文字列
     * @param clz  変換先クラス
     * @return オブジェクト
     */
    public static <T> T toDto(@NonNull String json, @NonNull Class<T> clz) {

        // tips. null safeの実装にしてもいいかもしれない

        try {
            return objectMapper.readValue(json, clz);
        } catch (JsonProcessingException e) {
            // tips. 実際に利用する場合は適当なクラスにwrapする
            throw new UncheckedIOException(e);

        }
    }

    /**
     * jsonをMapに変換
     *
     * @param json json文字列
     * @return オブジェクト
     */
    public static Map<String, String> toMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            // tips. 実際に利用する場合は適当なクラスにwrapする
            throw new UncheckedIOException(e);
        }
    }

    /**
     * jsonを{@link java.util.ArrayList}オブジェクトに変換
     *
     * @param <T>  変換先クラス
     * @param json json文字列
     * @param clz  変換先クラス
     * @return リストオブジェクト
     */
    public static <T> List<T> toList(String json, @NonNull Class<T> clz) {

        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clz);
        try {
            return objectMapper.readValue(json, listType);
        } catch (JsonProcessingException e) {
            // tips. 実際に利用する場合は適当なクラスにwrapする
            throw new UncheckedIOException(e);
        }
    }

    /**
     * jsonを{@link JsonNode}に変換する
     *
     * @param json json文字列
     * @return {@link JsonNode}
     */
    public static JsonNode toNode(String json) {

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            // tips. 実際に利用する場合は適当なクラスにwrapする
            throw new UncheckedIOException(e);
        }

    }
}
