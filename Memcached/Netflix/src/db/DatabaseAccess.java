package db;

import java.sql.*;
import java.util.ArrayList;
import org.json.JSONObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import net.spy.memcached.MemcachedClient;

public class DatabaseAccess extends Thread {
    Statement statement;
    MemcachedClient cache;

    static int numRequests = 100;
    static int percentRead = 50;
    static int percentInsert = 20;
    // percentUpdate is simply remaining amount

    // Credentials
// Credentials
    static String username = "root";
    static String password = "";

    // JDBC URLs
    static String urlMySQL = "jdbc:mysql://localhost:3306/netflix?characterEncoding=utf8";
    static String urlPSQL = "jdbc:postgresql://192.168.2.24:5432/netflix";
    static String urlMonetDB = "jdbc:monetdb://192.168.0.108:1337/netflix";
    static String urlDB2 = "jdbc:db2://192.168.0.108:6969/netflix";

    public void run() {
        try {
            // Initialize connection to db of your choice
            Connection con = DriverManager.getConnection(urlMySQL, username, password);
            statement = con.createStatement();

            /*----------*****************************------*/

            //cache = Main.CacheManager.getCache();
            try {
                // 11211 is default Memcached port
                cache = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
            } catch (IOException e) {
                e.printStackTrace();
            }

            //cache.flush(); //clear cache

            for (int i = 0; i < numRequests; i++) {
                // Randomly choose an operation based on % probabilities
                int choice = (int) (Math.random() * 100); // Generate random number 0-99
                if (choice < percentRead) {
                    executeRead();
                } else if (choice < percentRead + percentInsert) {
                    executeInsert();
                } else {
                    executeUpdate();
                }
            }

            statement.close();
            con.close();

            //Main.CacheManager.returnCache(cache);
            Main.threads --;
            System.out.println("thread:"+Main.threads);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Randomly chooses read instruction to execute
    void executeRead() {
        int accid = (int) ((Math.random() * 2)); // Generate random number 0-1

        switch (accid) {
            case 0:
                getAvailableMedia();
                break;
            case 1:
                getNumEpisodes();
                break;
        }
    }

    // Randomly chooses insert instruction to execute
    void executeInsert() {
        int accid = (int) ((Math.random() * 2)); // Generate random number 0-1
        switch (accid) {
            case 0:
                createNewPayment();
                break;
            case 1:
                createAccount();
                break;
        }
    }

    // Randomly chooses update instruction to execute
    void executeUpdate() {
        int accid = (int) ((Math.random() * 1)); // Generate number 0-0 (Add more options for other projects)

        switch (accid) {
            case 0:
                increaseRockRatings();
                break;
        }
    }

    @SuppressWarnings("unchecked")
    void getAvailableMedia() {
        //System.out.println("___Get Available Media");
        int accid = (int) ((Math.random() * 10) + 1);

        try {
            // Create query
            String querySQL = "SELECT title FROM AccountUser au, available_in avail, Media med\r\n"
                    + "WHERE au.accid=" + accid
                    + " AND au.username=\'User\' AND avail.regname=au.regname AND med.mid=avail.mid;";

            String jsonString = (String) cache.get(Integer.toString(querySQL.hashCode()));
            // If not cached
            if (jsonString == null) {
                JSONObject jsonObj = new JSONObject();
                int count=0;

                java.sql.ResultSet rs = statement.executeQuery(querySQL);

                while (rs.next()) {
                    jsonObj.put(Integer.toString(count), rs.getString(1));
                    count++;
                }
                // Put in the cache
                cache.set(Integer.toString(querySQL.hashCode()), 3600, jsonObj.toString());

                System.out.println("Not found in cache.");
                System.out.println(jsonObj.toString());
            }

            // Else found in cache
            else {
                System.out.println("Retrieved from cache.");
                System.out.println(jsonString);
            }
        } catch (Exception e) {
            String sqlMessage = e.getMessage();
            System.out.println(sqlMessage);
        }
    }

    void getNumEpisodes() {
        //System.out.println("---Get Num Episodes");
        int seasonNum = (int) ((Math.random() * 10) + 1);
        int numEpisodes;

        try {
            String querySQL = "SELECT COUNT(*) \r\n" + "FROM Episode e, Media m\r\n"
                    + "WHERE e.mid=m.mid AND m.title=\'Pokemon\' AND e.seasonnum=" + seasonNum + ";";


            // Check to see if answer already cached
            // Can save in any serializable format (ArrayList, LinkedList, etc...)
            String jsonString = (String) cache.get(Integer.toString(querySQL.hashCode()));
            // If not cached
            if (jsonString == null) {
                java.sql.ResultSet rs = statement.executeQuery(querySQL);
                rs.next();
                numEpisodes = rs.getInt(1);
                String numString = Integer.toString(numEpisodes);

                cache.set(Integer.toString(querySQL.hashCode()),3600,numString);
                System.out.println("Not found in cache.");
                System.out.println(numEpisodes);
            }
            // Else found in cache
            else {
                System.out.println("Retrieved from cache.");
                System.out.println(jsonString);
            }
        } catch (SQLException e) {
            String sqlMessage = e.getMessage();
            System.out.println(sqlMessage);
        }

    }

    void createNewPayment() {
        //System.out.println("---Create New Payment");
        int accid = (int) ((Math.random() * 10) + 1);
        String date = "2020-01-01";
        Double amount = (double) ((int) (Math.random() * 100) + 1);

        try {
            // Create insert string
            String insertSQL = "INSERT INTO Payment VALUES (DEFAULT, \'" + date + "\', " + amount + ", " + accid + ");";

            // Execute insert
            statement.executeUpdate(insertSQL);
        } catch (SQLException e) {
            String sqlMessage = e.getMessage();
            System.out.println(sqlMessage);
        }
    }

    void createAccount() {
        //System.out.println("---Create Account");

        try {
            // Create insert string
            String insertSQL = "INSERT INTO Account VALUES (DEFAULT);";
            // Execute insert
            System.out.println(insertSQL);
            statement.executeUpdate(insertSQL);
        } catch (SQLException e) {
            String sqlMessage = e.getMessage();
            System.out.println(sqlMessage);
        }
    }

    void increaseRockRatings() {
        //System.out.println("---Increase Rock Ratings");

        try {
            // Create update string
            String updateSQL = "UPDATE Rating\r\n" + "SET value = value + 1\r\n" + "WHERE value <= 4 AND mid IN \r\n"
                    + "(\r\n" + "	SELECT mid\r\n" + "	FROM describes d, Tag t \r\n"
                    + "	WHERE d.tid=t.tid AND t.tagname='The Rock'\r\n" + ");";

            // Execute insert
            statement.executeUpdate(updateSQL);
        } catch (SQLException e) {
            String sqlMessage = e.getMessage();
            System.out.println(sqlMessage);
        }
    }
    public static ArrayList<String> resultSetToJsonObject(ResultSet rs) throws SQLException{
        // json Obj
        ArrayList<String> ans = new ArrayList<String>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();


        while (rs.next()) {
            JSONObject jsonObj = new JSONObject();
            for (int i = 1; i <= columnCount; i++) {
                String columnName =metaData.getColumnLabel(i);
                String value = rs.getString(columnName);
                jsonObj.put(columnName, value);
            }
            String str = jsonObj.toString();
            ans.add(str);
        }
        return ans;
    }
}