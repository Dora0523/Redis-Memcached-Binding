package db;
import java.sql.*;
import java.util.ArrayList;

import redis.clients.jedis.Jedis;

public class DatabaseAccess extends Thread {
	Statement statement;
	Jedis jedis;
	
	static int numRequests = 100;
	
	static int percentRead = 50;
	static int percentInsert = 50;
	// percentUpdate is simply remaining amount

	// Credentials
	static String username = "root";
	static String password = "104479hxc";

	// JDBC URLs
	static String urlMySQL = "jdbc:mysql://localhost:3306/venues?characterEncoding=utf8";

	public void run() {
		try {
			Connection con = DriverManager.getConnection(urlMySQL, username, password);
			statement = con.createStatement();

			jedis = Main.jedisManager.getJedis();


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
			Main.jedisManager.returnJedis(jedis);
			Main.threads --;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Randomly chooses read instruction to execute from Queries
	void executeRead() {
		int index = (int) ((Math.random() * 19)); // Generate number 0-18
		String readSQL = Queries.readQueries[index];

		try {
			if (jedis.lrange(Integer.toString(readSQL.hashCode()), 0, -1).size() == 0) {
				ArrayList<String> ans = new ArrayList<String>();
				java.sql.ResultSet rs = statement.executeQuery(readSQL);
				int numcolum = rs.getMetaData().getColumnCount();
				while (rs.next()) {
					for(int i = 1; i <= numcolum ;i++) {
						String add = rs.getString(i);
						if(add != null) {
							ans.add(add);
							jedis.rpush(Integer.toString(readSQL.hashCode()), add);
						}else {
							ans.add("null");
							jedis.rpush(Integer.toString(readSQL.hashCode()), "null");
						}
					}
				}

				System.out.println("Not found in cache.");
				System.out.println(ans.toString());
			}else {
				ArrayList<String> ans = (ArrayList<String>)jedis.lrange(Integer.toString(readSQL.hashCode()), 0, -1);
				System.out.println("Retrieved from cache.");
				System.out.println(ans.toString());
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
}