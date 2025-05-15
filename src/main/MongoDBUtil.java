
import com.mongodb.*;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class MongoDBUtil {
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final ClientSessionOptions sessionOptions = ClientSessionOptions.builder()
            .causallyConsistent(true)
            .build();

    // 初始化连接
    public MongoDBUtil() {
        String connectionString = src.main.ConfigReader.getProperty("mongo.uri");
        String dbName = src.main.ConfigReader.getProperty("mongo.database");

        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(dbName);
    }

    // 获取集合
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    // 插入单个文档
    public void insertOne(String collectionName, Document document) {
        getCollection(collectionName).insertOne(document);
    }

    // 插入多个文档
    public void insertMany(String collectionName, List<Document> documents) {
        getCollection(collectionName).insertMany(documents);
    }

    // 获取数据库
    public MongoDatabase getDatabase() {
        return this.database;
    }

    // 查询所有文档
    public List<Document> findAll(String collectionName) {
        List<Document> results = new ArrayList<>();
        getCollection(collectionName).find().into(results);
        return results;
    }

    // 条件查询
    public List<Document> find(String collectionName, Bson filter) {
        List<Document> results = new ArrayList<>();
        getCollection(collectionName).find(filter).into(results);
        return results;
    }

    // 分页查询
    public List<Document> findWithPagination(String collectionName, Bson filter,
                                             int page, int size, Bson sort) {
        List<Document> results = new ArrayList<>();
        getCollection(collectionName)
                .find(filter)
                .sort(sort)
                .skip((page - 1) * size)
                .limit(size)
                .into(results);
        return results;
    }

    // 更新单个文档
    public UpdateResult updateOne(String collectionName, Bson filter, Bson update) {
        return getCollection(collectionName).updateOne(filter, update);
    }

    // 更新多个文档
    public UpdateResult updateMany(String collectionName, Bson filter, Bson update) {
        return getCollection(collectionName).updateMany(filter, update);
    }

    // 批量写入操作
    public BulkWriteResult bulkWrite(String collectionName, List<WriteModel<Document>> operations) {
        return getCollection(collectionName).bulkWrite(operations);
    }

    // 删除单个文档
    public DeleteResult deleteOne(String collectionName, Bson filter) {
        return getCollection(collectionName).deleteOne(filter);
    }

    // 删除多个文档
    public DeleteResult deleteMany(String collectionName, Bson filter) {
        return getCollection(collectionName).deleteMany(filter);
    }

    // 聚合查询
    public List<Document> aggregate(String collectionName, List<Bson> pipeline) {
        List<Document> results = new ArrayList<>();
        getCollection(collectionName).aggregate(pipeline).into(results);
        return results;
    }

    // 创建索引
    public String createIndex(String collectionName, Bson keys, IndexOptions options) {
        return getCollection(collectionName).createIndex(keys, options);
    }

    // 获取所有索引
    public List<Document> listIndexes(String collectionName) {
        List<Document> indexes = new ArrayList<>();
        getCollection(collectionName).listIndexes().into(indexes);
        return indexes;
    }

    // 文档计数
    public long countDocuments(String collectionName, Bson filter) {
        return getCollection(collectionName).countDocuments(filter);
    }

    // 估算文档数量
    public long estimatedDocumentCount(String collectionName) {
        return getCollection(collectionName).estimatedDocumentCount();
    }

    // 执行事务
    public void executeTransaction(TransactionBody<String> transactionBody) {
        try (ClientSession session = mongoClient.startSession()) {
            session.withTransaction(transactionBody);
        }
    }

    // 检查连接是否有效
    public boolean isConnectionValid() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 关闭连接
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}