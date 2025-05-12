package src.main;
import java.util.*;
import com.mongodb.client.MongoDatabase;

public class Main {
    public static void main(String[] args) {
        MongoDBUtil mg = new MongoDBUtil();
        MongoDatabase a = mg.getDatabase();
    }
}
