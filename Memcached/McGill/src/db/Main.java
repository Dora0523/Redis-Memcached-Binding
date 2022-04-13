package db;
import java.io.IOException;
import java.sql.*;
import java.net.InetSocketAddress;
import org.json.JSONException;
import org.json.JSONObject;
import net.spy.memcached.MemcachedClient;

public class Main {

        /*********
        //Start Memcached from Linux before running !!!!!
        //brew services start memcached
        **********/


        static int threads = 0;

	public static void main(String[] args) throws SQLException {
		int numThreads = 50;


//	    try {
//	    	cachManager = CacheManager.getInstance();
//	    	cacheManager.connect();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}





        for (int i=0; i<numThreads; i++)
        {
        	threads++;
            DatabaseAccess object = new DatabaseAccess();
            object.start();
        }
        //keep running when threads = 0 if leave it emptys
 //       while(threads > 0) {System.out.print("");}
        //cacheManager.release();

   	}
   }






