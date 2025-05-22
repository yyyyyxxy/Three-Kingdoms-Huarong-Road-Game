public  class MailRecord {
    private String from;
    private String status;
    public MailRecord(String from, String status) {
        this.from = from;
        this.status = status;
    }
    public String getFrom() { return from; }
    public String getStatus() { return status; }

}