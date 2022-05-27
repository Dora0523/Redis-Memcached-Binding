package db;

import java.sql.*;
import java.util.ArrayList;
import org.json.JSONObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import net.spy.memcached.MemcachedClient;
import java.security.cert.CertificateFactory;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import java.io.FileInputStream;
import java.io.InputStream;

public class DatabaseAccess extends Thread {
    Statement statement;
    MemcachedClient cache;

    static int numRequests = 100;
    static int percentRead = 100;
    static int percentInsert = 0;
    // percentUpdate is simply remaining amount

    // Credentials
    static String username = "root";
    static String password = "104479hxc";

    // JDBC URLs
    static String urlMySQL = "jdbc:mysql://localhost:3306/netflix?characterEncoding=utf8";

    public void run() {
        try {
            // Initialize connection to db of your choice
            Connection con = DriverManager.getConnection(urlMySQL, username, password);
            statement = con.createStatement();
            /*----------*****************************------*/

            //cache = Main.CacheManager.getCache();

            try {
                // 11211 is default Memcached port
                InputStream inputStream = new FileInputStream("bin/resources/server.crt");
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null);
                ks.setCertificateEntry("cert", CertificateFactory.getInstance("X.509").generateCertificate(inputStream));
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                //connection builder
                ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder();

                // Build SSLContext
                connectionFactoryBuilder.setSSLContext(sslContext);
                connectionFactoryBuilder.setSkipTlsHostnameVerification(true);

                cache = new MemcachedClient(connectionFactoryBuilder.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                        .build(), AddrUtil.getAddresses("127.0.0.1:11211"));
                //cache = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
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
                //JSONObject jsonObj = new JSONObject();
                int count=0;
                ArrayList<String> medias = new ArrayList<String>();

                java.sql.ResultSet rs = statement.executeQuery(querySQL);

                while (rs.next()) {
                    //medias.put(Integer.toString(count), rs.getString(1));
                    medias.add(rs.getString(1));
                    count++;
                }
                // Put in the cache
                cache.set(Integer.toString(querySQL.hashCode()), 3600, medias.toString());

                System.out.println("Not found in cache.");
                System.out.println(medias.toString());
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
