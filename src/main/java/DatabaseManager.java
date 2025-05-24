
import com.mongodb.client.*;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.bson.Document;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;


public class DatabaseManager implements AutoCloseable {
    private static volatile DatabaseManager INSTANCE;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private static final Object lock = new Object();

    private DatabaseManager() {
        initialize();
    }

    public static DatabaseManager getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseManager();
                }
            }
        }
        return INSTANCE;
    }

    private void initialize() {
        try {
            // 使用 ConfigReader 读取配置文件中的连接字符串
            String connectionUri = ConfigReader.getProperty("mongo.uri");
            String dbName = ConfigReader.getProperty("mongo.database");

            if (connectionUri == null || connectionUri.trim().isEmpty()) {
                throw new IllegalArgumentException("MongoDB连接字符串未配置");
            }

            if (dbName == null || dbName.trim().isEmpty()) {
                throw new IllegalArgumentException("数据库名称未配置");
            }

            ConnectionString connectionString = new ConnectionString(connectionUri);

            // 添加 ServerApi 配置以支持云端 MongoDB
            ServerApi serverApi = ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build();

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .serverApi(serverApi)
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(dbName);

            // 测试连接
            this.database.runCommand(new Document("ping", 1));
            System.out.println("数据库连接成功: " + dbName);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        if (database == null) {
            throw new IllegalStateException("数据库未初始化");
        }
        return database.getCollection(collectionName);
    }

    public Document getUserByUsername(String username) {
        try {
            return getCollection("users").find(new Document("username", username)).first();
        } catch (Exception e) {
            throw new RuntimeException("查询用户失败: " + username, e);
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public static void shutdown() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}