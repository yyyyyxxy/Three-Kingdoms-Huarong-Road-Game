package src.main;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class MongoDBUtil {
    private MongoClient mongoClient;
    private MongoDatabase database;

    // 初始化连接
    public MongoDBUtil() {
        String connectionString = ConfigReader.getProperty("mongo.uri");
        String dbName = ConfigReader.getProperty("mongo.database");

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
    //get database
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

    // 更新单个文档
    public UpdateResult updateOne(String collectionName, Bson filter, Bson update) {
        return getCollection(collectionName).updateOne(filter, update);
    }

    // 更新多个文档
    public UpdateResult updateMany(String collectionName, Bson filter, Bson update) {
        return getCollection(collectionName).updateMany(filter, update);
    }

    // 删除单个文档
    public DeleteResult deleteOne(String collectionName, Bson filter) {
        return getCollection(collectionName).deleteOne(filter);
    }

    // 删除多个文档
    public DeleteResult deleteMany(String collectionName, Bson filter) {
        return getCollection(collectionName).deleteMany(filter);
    }

    // 关闭连接
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}