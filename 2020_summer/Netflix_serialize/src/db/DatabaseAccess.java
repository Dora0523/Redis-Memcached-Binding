package db;
import java.sql.*;
import java.util.ArrayList;
import org.json.JSONObject;
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
	static String urlPSQL = "jdbc:postgresql://192.168.2.24:5432/netflix";
	static String urlMySQL = "jdbc:mysql://localhost:3306/netflix?characterEncoding=utf8";
	static String urlMonetDB = "jdbc:monetdb://192.168.0.108:1337/netflix";
	static String urlDB2 = "jdbc:db2://192.168.0.108:6969/netflix";

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
				Thread.sleep(1500);



			}

			statement.close();
			con.close();
			Main.jedisManager.returnJedis(jedis);
			Main.threads --;
			//System.out.println("thread:"+Main.threads);
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
		//System.out.println("getAvaialbe");
		int accid = (int) ((Math.random() * 10) + 1);

		//ArrayList<String> movies;

		try {
			// Create query
			String querySQL = "SELECT title FROM AccountUser au, available_in avail, Media med\r\n"
					+ "WHERE au.accid=" + accid
					+ " AND au.username=\'User\' AND avail.regname=au.regname AND med.mid=avail.mid;";


			if (jedis.lrange(Integer.toString(querySQL.hashCode()), 0, -1).size() == 0) {
				java.sql.ResultSet rs = statement.executeQuery(querySQL);
				ArrayList<String> jsonrs = resultSetToJsonObject(rs);

				System.out.println("Not found in cache.");
				System.out.println(jsonrs.toString());
				for(int i = 0 ; i < jsonrs.size();i++) {
					jedis.rpush(Integer.toString(querySQL.hashCode()),jsonrs.get(i) );
				}

			} else {
				ArrayList<String> ans = (ArrayList<String>)jedis.lrange(Integer.toString(querySQL.hashCode()), 0, -1);
				//System.out.println("Retrieved from cache.");
				//System.out.println(ans);
			}

			// Execute query
			//statement.executeQuery(querySQL);
		} catch (Exception e) {
			String sqlMessage = e.getMessage();
			System.out.println(sqlMessage);
		}
	}

	void getNumEpisodes() {
		//System.out.println("getNumEpisodes");
		int seasonNum = (int) ((Math.random() * 10) + 1);
		int numEpisodes;

		try {
			// Create query
			String querySQL = "SELECT COUNT(*) \r\n" + "FROM Episode e, Media m\r\n"
					+ "WHERE e.mid=m.mid AND m.title=\'Pokemon\' AND e.seasonnum=" + seasonNum + ";";


			if (jedis.get(Integer.toString(querySQL.hashCode()))== null) {
				java.sql.ResultSet rs = statement.executeQuery(querySQL);
				rs.next();
				numEpisodes = rs.getInt(1);
				String numString = Integer.toString(numEpisodes);
				System.out.println("Not found in cache.");
				System.out.println(numEpisodes);

				jedis.set(Integer.toString(querySQL.hashCode()),numString);

			} else {
				String numString =jedis.get(Integer.toString(querySQL.hashCode()));
				numEpisodes = Integer.parseInt(numString);
				System.out.println("Retrieved from cache.");
				System.out.println(numEpisodes);
			}

			// Execute query
			statement.executeQuery(querySQL);
		} catch (SQLException e) {
			String sqlMessage = e.getMessage();
			System.out.println(sqlMessage);
		}
	}

	void createNewPayment() {
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
		try {
			// Create insert string
			String insertSQL = "INSERT INTO Account VALUES (DEFAULT);";
			// Execute insert
			statement.executeUpdate(insertSQL);
		} catch (SQLException e) {
			String sqlMessage = e.getMessage();
			System.out.println(sqlMessage);
		}
	}

	void increaseRockRatings() {
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
		if (rs.next()) {
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