import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class MongoDBUtil {
    private final MongoClient mongoClient;
    private final MongoDatabase database;

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

    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    // 查询单个文档
    public Document getDocument(String collectionName, Bson filter) {
        return getCollection(collectionName).find(filter).first();
    }

    // 根据用户名获取用户文档
    public Document getUserByUsername(String username) {
        return getDocument("users", new Document("username", username));
    }

    // 用户名是否已存在
    public boolean userExists(String username) {
        return getUserByUsername(username) != null;
    }

    // 校验用户名和密码
    public boolean checkPassword(String username, String password) {
        Document doc = getUserByUsername(username);
        if (doc == null) return false;
        return password.equals(doc.getString("password"));
    }

    // 注册新用户（返回true表示注册成功，false表示用户名已存在）
    public boolean registerUser(String username, String password) {
        if (userExists(username)) return false;
        Document newUser = new Document("username", username)
                .append("password", password);
        getCollection("users").insertOne(newUser);
        return true;
    }

    // 直接插入文档（用于注册界面）
    public void insertOne(String collectionName, Document doc) {
        getCollection(collectionName).insertOne(doc);
    }

    // 查询所有用户
    public List<Document> findAllUsers() {
        List<Document> results = new ArrayList<>();
        getCollection("users").find().into(results);
        return results;
    }

    // 关闭连接
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}