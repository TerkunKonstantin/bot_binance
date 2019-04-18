package DB_Connection;

class CRUD_config {
    private final static String USERNAME = "root";
    private final static String PASSWORD = "root";
    private final static String URL = "jdbc:mysql://localhost:3306/mysql?autoReconnect=true&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC";


    static String getUSERNAME() {
        return USERNAME;
    }

    static String getPASSWORD() {
        return PASSWORD;
    }

    static String getURL() {
        return URL;
    }


}
