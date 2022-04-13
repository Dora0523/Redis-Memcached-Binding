package db;
import java.io.IOException;
import java.io.IOException;
import java.sql.*;
import java.net.InetSocketAddress;
import org.json.JSONException;
import org.json.JSONObject;
import net.spy.memcached.MemcachedClient;

public class Main {
	static RedisManager jedisManager;
	static int threads = 0;
	
	public static void main(String[] args) throws SQLException {		
		int numThreads = 50; 
		
	    try {
	    	jedisManager = RedisManager.getInstance();
	    	jedisManager.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}  

        for (int i=0; i<numThreads; i++)
        {
        	threads++;
            DatabaseAccess object = new DatabaseAccess(); 
            object.start(); 
        }

	}
}