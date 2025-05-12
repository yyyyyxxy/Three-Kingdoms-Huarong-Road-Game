package src.test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.*;
import src.main.MongoDBUtil;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class MongoDBUtilTest {

    private static final String TEST_DB = "test_db";
    private static final String TEST_COLLECTION = "test_collection";
    private static MongoDBUtil mongoDBUtil;

    // 测试用的连接配置 - 建议从环境变量获取或使用测试专用配置
    private static final String MONGO_URI = "mongodb+srv://username:password@cluster0.example.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

    @BeforeAll
    static void setUp() {
        try {
            mongoDBUtil = new MongoDBUtil();

            // 测试连接是否成功
            mongoDBUtil.getCollection(TEST_COLLECTION).insertOne(new Document("test", "connection"));
        } catch (MongoException e) {
            System.out.println("wufalianjie");
        }
    }

    @AfterAll
    static void tearDown() {
        if (mongoDBUtil != null) {
            // 清理测试数据
            mongoDBUtil.getCollection(TEST_COLLECTION).deleteMany(new Document());
            mongoDBUtil.close();
        }
    }

    @Test
    @DisplayName("测试数据库连接是否成功")
    void testConnection() {
        assertDoesNotThrow(() -> {
            // 执行一个简单的命令测试连接
            mongoDBUtil.getDatabase().runCommand(new Document("ping", 1));
        }, "数据库连接失败");
    }

    @Test
    @DisplayName("测试插入和查询文档")
    void testInsertAndFind() {
        // 准备测试数据
        Document testDoc = new Document("name", "测试用户")
                .append("age", 30)
                .append("email", "test@example.com");

        // 插入文档
        mongoDBUtil.insertOne(TEST_COLLECTION, testDoc);

        // 查询文档
        Document foundDoc = mongoDBUtil.getCollection(TEST_COLLECTION)
                .find(new Document("email", "test@example.com"))
                .first();

        // 验证结果
        assertThat(foundDoc)
                .isNotNull()
                .containsEntry("name", "测试用户")
                .containsEntry("age", 30);
    }

    @Test
    @DisplayName("测试更新文档")
    void testUpdate() {
        // 插入测试数据
        Document originalDoc = new Document("username", "old_user")
                .append("status", "active");
        mongoDBUtil.insertOne(TEST_COLLECTION, originalDoc);

        // 更新文档
        long modifiedCount = mongoDBUtil.updateOne(
                TEST_COLLECTION,
                new Document("username", "old_user"),
                new Document("$set", new Document("username", "new_user"))
        ).getModifiedCount();

        // 验证更新结果
        assertEquals(1, modifiedCount, "应该更新1个文档");

        Document updatedDoc = mongoDBUtil.getCollection(TEST_COLLECTION)
                .find(new Document("username", "new_user"))
                .first();

        assertThat(updatedDoc)
                .isNotNull()
                .containsEntry("status", "active");
    }

    @Test
    @DisplayName("测试删除文档")
    void testDelete() {
        // 插入测试数据
        Document docToDelete = new Document("temp", true);
        mongoDBUtil.insertOne(TEST_COLLECTION, docToDelete);

        // 删除文档
        DeleteResult result = mongoDBUtil.deleteOne(
                TEST_COLLECTION,
                new Document("temp", true)
        );

        // 验证删除结果
        assertEquals(1, result.getDeletedCount(), "应该删除1个文档");

        // 确认文档已删除
        Document deletedDoc = mongoDBUtil.getCollection(TEST_COLLECTION)
                .find(new Document("temp", true))
                .first();

        assertNull(deletedDoc, "文档应该已被删除");
    }

    @Test
    @DisplayName("测试集合操作")
    void testCollectionOperations() {
        String tempCollection = "temp_collection_" + System.currentTimeMillis();

        // 测试创建集合
        MongoCollection<Document> collection = mongoDBUtil.getCollection(tempCollection);
        assertNotNull(collection, "应该能获取集合");

        // 插入测试数据
        collection.insertOne(new Document("test", "data"));

        // 验证数据存在
        assertThat(collection.countDocuments())
                .isEqualTo(1);

        // 清理
        collection.drop();
    }
}