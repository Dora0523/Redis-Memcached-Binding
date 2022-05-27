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
	static String urlMySQL = "jdbc:mysql://localhost:3306/venues?characterEncoding=utf8";

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
	// Randomly chooses read instruction to execute from Queries
	void executeRead() {
		int index = (int) ((Math.random() * 19)); // Generate number 0-18

		String readSQL = Queries.readQueries[index];
                // Can save in any serializable format (ArrayList, LinkedList, etc...)
		String jsonString = (String) cache.get(Integer.toString(readSQL.hashCode()));
		try {
			if (jsonString == null) {
				ArrayList<String> ans = new ArrayList<String>();
				java.sql.ResultSet rs = statement.executeQuery(readSQL);
				ArrayList<String> jsonrs = resultSetToJsonObject(rs);
				JSONObject jsonObj = new JSONObject();

				for(int i = 0 ; i < jsonrs.size();i++) {
							String add = jsonrs.get(i);
							ans.add(add);
						}

				cache.set(Integer.toString(readSQL.hashCode()), 3600, ans.toString());

				System.out.println("Not found in cache.");
				System.out.println(ans.toString());
			}else {
				System.out.println("Retrieved from cache.");
				System.out.println(jsonString);
			}
			// Execute query
			//statement.executeQuery(readSQL);
		} catch (SQLException e) {
			System.out.println(index);

			String sqlMessage = e.getMessage();
			System.out.println(sqlMessage);
		}
	}

	// Randomly chooses insert instruction to execute
	void executeInsert() {
		int oid = (int) ((Math.random() * Integer.MAX_VALUE)); // Generate random integer
		String oname = getAlphaNumericString(10);
		
		String insertSQL = "INSERT INTO organization VALUES (" + oid + ", \'" + oname + "\');";
		
		try {
			// Execute query
			statement.executeUpdate(insertSQL);
		} catch (SQLException e) {
			String sqlMessage = e.getMessage();
			System.out.println(sqlMessage);
		}
	}

	// Randomly chooses update instruction to execute
	void executeUpdate() {
		String newName = getAlphaNumericString(10);

		String updateSQL = "UPDATE person SET pname = \'" + newName + "\' WHERE pid = 69696969;";
		
		try {
			// Execute query
			statement.executeUpdate(updateSQL);
		} catch (SQLException e) {
			String sqlMessage = e.getMessage();
			System.out.println(sqlMessage);
		}
	}
	
	static String getAlphaNumericString(int n) 
    { 
  
        // chose a Character random from this String 
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                    + "0123456789"
                                    + "abcdefghijklmnopqrstuvxyz"; 
  
        // create StringBuffer size of AlphaNumericString 
        StringBuilder sb = new StringBuilder(n); 
  
        for (int i = 0; i < n; i++) { 
  
            // generate a random number between 
            // 0 to AlphaNumericString variable length 
            int index 
                = (int)(AlphaNumericString.length() 
                        * Math.random()); 
  
            // add Character one by one in end of sb 
            sb.append(AlphaNumericString 
                          .charAt(index)); 
        } 
  
        return sb.toString(); 
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
