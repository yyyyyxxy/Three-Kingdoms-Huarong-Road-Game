// 聊天列表记录类 - 增强版
public  class ChatListRecord {
    private String friendName;
    private String lastMessage;
    private int unreadCount;
    private long lastMessageTime; // 新增时间戳

    public ChatListRecord(String friendName, String lastMessage, int unreadCount) {
        this.friendName = friendName;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.lastMessageTime = System.currentTimeMillis(); // 默认当前时间
    }

    public ChatListRecord(String friendName, String lastMessage, int unreadCount, long lastMessageTime) {
        this.friendName = friendName;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.lastMessageTime = lastMessageTime;
    }

    public String getFriendName() { return friendName; }
    public String getLastMessage() { return lastMessage; }
    public int getUnreadCount() { return unreadCount; }
    public long getLastMessageTime() { return lastMessageTime; }
}