package db;
import java.io.IOException;
import java.util.*;
import java.sql.*;
import redis.clients.jedis.Jedis;


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

	    //try {
	        for (int i=0; i<numThreads; i++){
	        	threads++;
	            DatabaseAccess object = new DatabaseAccess(); 
	            object.start();
	            //Thread.sleep(2000);
	        } 
	   // }catch(InterruptedException e) {
	    //		e.printStackTrace();
	    //}
        //keep running when threads = 0 if leave it emptys
        while(threads > 0) {
        	System.out.print("");
        	}
        jedisManager.release();
        
	}
}