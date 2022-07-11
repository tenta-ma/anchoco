package org.tantama.anchoco.springcrib.helper;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JsonHelper}のテストクラス
 */
class JsonHelperTest {

    /**
     * 初期処理
     */
    @BeforeEach
    public void setUp() {

    }

    /**
     * {@link JsonHelper#toJson(Object)}のテスト
     *
     * <pre>
     * objectをjsonに変換できること
     * </pre>
     *
     * @throws JsonException json変換に失敗
     */
    @Test
    @DisplayName("objectをjsonに変換")
    public void testToJson() {
        TestPojo obj = new TestPojo();
        obj.setId(1);
        obj.setName("hoge");

        assertEquals("{\"id\":1,\"name\":\"hoge\"}", JsonHelper.toJson(obj));
    }

    /**
     * {@link JsonHelper#toJson(Object)}のテスト
     *
     * <pre>
     * 空objectを空jsonに変換できること
     * </pre>
     *
     * @throws JsonException json変換に失敗
     */
    @Test
    @DisplayName("空objectを空jsonに変換")
    public void testToJsonByEmptyObject() {
        TestPojo obj = new TestPojo();

        assertEquals("{}", JsonHelper.toJson(obj));

    }

    /**
     * {@link JsonHelper#toDto(String, Class)}のテスト
     *
     * <pre>
     * jsonをobjectに変換できること
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("jsonをobjectに変換")
    public void testToDto() {
        String json = "{\"id\":2,\"name\":\"fuga\"}";
        TestPojo pojo = JsonHelper.toDto(json, TestPojo.class);

        assertEquals(2, pojo.getId());
        assertEquals("fuga", pojo.getName());

    }

    /**
     * {@link JsonHelper#toDto(String, Class)}のテスト
     *
     * <pre>
     * null入力の場合、null pointer exceptionが発生する
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("nullはobjectに変換できない")
    public void testToDtoByNull() {

        assertThrows(NullPointerException.class, () -> JsonHelper.toDto(null, TestPojo.class));

    }

    /**
     * {@link JsonHelper#toDto(String, Class)}のテスト
     *
     * <pre>
     * 空jsonを空objectに変換できること
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("空jsonを空objectに変換")
    public void testToDtoByEmpty() {
        String json = "{}";

        TestPojo pojo = JsonHelper.toDto(json, TestPojo.class);

        assertNull(pojo.getId());
        assertNull(pojo.getName());

    }

    /**
     * {@link JsonHelper#toMap(String)}のテスト
     *
     * <pre>
     * jsonをmapに変換できること
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("jsonをmapに変換")
    public void testToMap() {
        String json = "{\"id\":3,\"name\":\"moge\"}";
        Map<String, String> map = JsonHelper.toMap(json);

        assertFalse(map.isEmpty());
        assertEquals(2, map.keySet().size());
        assertTrue(map.containsKey("id"));
        assertEquals("3", map.get("id"));
        assertTrue(map.containsKey("name"));
        assertEquals("moge", map.get("name"));
    }

    /**
     * {@link JsonHelper#toMap(String)}のテスト
     *
     * <pre>
     * emptyを空mapに変換できること
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("emptyを空mapに変換")
    public void testToMapByEmpty() {
        String json = "{}";

        Map<String, String> map = JsonHelper.toMap(json);
        assertTrue(map.isEmpty());
    }

    /**
     * {@link JsonHelper#toList(String, Class)}のテスト
     *
     * <pre>
     * jsonをlistに変換できること
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("jsonをlistに変換")
    public void testToList() {

        String listJson = "[{\"id\":1,\"name\":\"hoge\"},{\"id\":2,\"name\":\"fuga\"}]";

        List<TestPojo> listPojo = JsonHelper.toList(listJson, TestPojo.class);

        assertNotNull(listPojo);
        assertEquals(2, listPojo.size());

        TestPojo hoge = listPojo.get(0);
        assertEquals(1, hoge.getId());
        assertEquals("hoge", hoge.getName());

        TestPojo fuga = listPojo.get(1);
        assertEquals(2, fuga.getId());
        assertEquals("fuga", fuga.getName());
    }

    /**
     * {@link JsonHelper#toList(String, Class)}のテスト
     *
     * <pre>
     * 空json listを空listに変換できること
     * </pre>
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("空json listを空listに変換")
    public void testToListByEmpty() {

        String listJson = "[]";

        List<TestPojo> listPojo = JsonHelper.toList(listJson, TestPojo.class);

        assertNotNull(listPojo);
        assertEquals(0, listPojo.size());
    }

    /**
     * {@link JsonHelper#toNode(String)}のテスト
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("Node変換")
    public void testToNode() {

        String json = "{\"id\":2,\"name\":\"fuga\"}";

        JsonNode node = JsonHelper.toNode(json);

        assertEquals(2, node.get("id").asInt());
        assertEquals("fuga", node.get("name").asText());
    }

    /**
     * {@link JsonHelper#toNode(String)}のテスト
     *
     * @throws JsonException オブジェクト変換に失敗
     */
    @Test
    @DisplayName("Node変換:null")
    public void testToNodeNull() {

        String json = null;

        assertNull(JsonHelper.toNode(json));
    }
}
