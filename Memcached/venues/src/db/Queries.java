package db;

public class Queries {
	static String[] readQueries = {
			"SELECT DISTINCT vid, capacity \r\n" + 
			"FROM venue \r\n" + 
			"WHERE (area='B' OR area='C') AND capacity > 100 \r\n" + 
			"ORDER by capacity DESC, vid;",
			
			"SELECT DISTINCT v.vid, capacity \r\n" + 
			"FROM calendar c \r\n" + 
			"JOIN venue v ON c.vid = v.vid \r\n" + 
			"WHERE area='C' AND date='2020-01-16' AND price < 100.00 \r\n" + 
			"ORDER BY v.vid;",
			
			"SELECT DISTINCT vid, capacity \r\n" + 
			"FROM venue v \r\n" + 
			"WHERE v.vid in \r\n" + 
			"(\r\n" + 
			"	SELECT c.vid \r\n" + 
			"	FROM calendar c \r\n" + 
			"	WHERE date='2020-01-16' AND price < 100.00\r\n" + 
			")\r\n" + 
			"AND area='C' \r\n" + 
			"ORDER BY vid;",
		
			"SELECT DISTINCT vid, capacity \r\n" + 
			"FROM venue v \r\n" + 
			"WHERE EXISTS \r\n" + 
			"(\r\n" + 
			"	SELECT *\r\n" + 
			"	FROM calendar c \r\n" + 
			"	WHERE date='2020-01-16' AND price < 100.00 AND c.vid=v.vid\r\n" + 
			")\r\n" + 
			"AND area='C' \r\n" + 
			"ORDER BY vid;",
			
			"SELECT DISTINCT part.pid, pname \r\n" + 
			"FROM participate part JOIN person p \r\n" + 
			"ON part.pid=p.pid \r\n" + 
			"WHERE eid=5 \r\n" + 
			"ORDER BY part.pid;",
			
			"SELECT DISTINCT o.oid, oname\r\n" + 
			"FROM organization o, schedule s, host h\r\n" + 
			"WHERE o.oid=h.oid AND h.eid=s.eid AND date='2020-01-16'\r\n" + 
			"ORDER BY o.oid;",
			
			"SELECT DISTINCT vid \r\n" + 
			"FROM venue\r\n" + 
			"WHERE vid NOT IN \r\n" + 
			"(\r\n" + 
			"	SELECT vid \r\n" + 
			"	FROM schedule\r\n" + 
			"	WHERE date='2020-01-17'\r\n" + 
			")\r\n" + 
			"AND area='A'\r\n" + 
			"ORDER BY vid;",
			
			"SELECT DISTINCT eid, date \r\n" + 
			"FROM schedule s \r\n" + 
			"WHERE s.eid IN \r\n" + 
			"(\r\n" + 
			"	SELECT e.eid \r\n" + 
			"	FROM event e \r\n" + 
			"	WHERE e.eid in \r\n" + 
			"	(\r\n" + 
			"		SELECT DISTINCT p.eid\r\n" + 
			"		FROM participate p, participate p1\r\n" + 
			"		WHERE p.eid=p1.eid AND p.pid='12345678' AND p1.pid='12345679'\r\n" + 
			"	)\r\n" + 
			"	AND e.type='fundraising'\r\n" + 
			")\r\n" + 
			"ORDER BY eid DESC, date;",
			
			"SELECT DISTINCT eid, date \r\n" + 
			"FROM schedule s \r\n" + 
			"WHERE s.eid IN \r\n" + 
			"(\r\n" + 
			"	SELECT e.eid\r\n" + 
			"	FROM event e\r\n" + 
			"	WHERE e.eid IN\r\n" + 
			"	(\r\n" + 
			"		SELECT DISTINCT p.eid\r\n" + 
			"		FROM participate p\r\n" + 
			"		WHERE p.pid='12345678' )\r\n" + 
			"	AND e.eid NOT IN\r\n" + 
			"	(\r\n" + 
			"		SELECT DISTINCT p.eid\r\n" + 
			"		FROM participate p\r\n" + 
			"		WHERE p.pid='12345679'\r\n" + 
			"	)\r\n" + 
			"	AND e.type='music'\r\n" + 
			")\r\n" + 
			"ORDER BY eid DESC, date;",
			
			"SELECT DISTINCT v.vid, area, capacity\r\n" + 
			"FROM venue v, schedule s\r\n" + 
			"WHERE EXISTS\r\n" + 
			"(\r\n" + 
			"	SELECT eid\r\n" + 
			"	FROM host h		\r\n" + 
			"	WHERE oid=6 AND h.eid=s.eid\r\n" + 
			")\r\n" + 
			"AND v.vid=s.vid\r\n" + 
			"ORDER BY vid, capacity;",
			
			"SELECT DISTINCT pid, pname\r\n" + 
			"FROM person\r\n" + 
			"WHERE pid IN\r\n" + 
			"(\r\n" + 
			"	SELECT pid\r\n" + 
			"	FROM participate p\r\n" + 
			"	WHERE p.eid IN\r\n" + 
			"	(\r\n" + 
			"		SELECT eid\r\n" + 
			"		FROM participate p1\r\n" + 
			"		WHERE p1.pid='12345678'\r\n" + 
			"	)\r\n" + 
			"	AND p.pid<>'12345678'\r\n" + 
			");",
			
			"SELECT COUNT(*) AS numorganizations \r\n" + 
			"FROM organization;",
			
			"SELECT COUNT(DISTINCT pid) AS numpeople\r\n" + 
			"FROM participate p\r\n" + 
			"WHERE p.eid IN\r\n" + 
			"(\r\n" + 
			"	SELECT eid\r\n" + 
			"	FROM host\r\n" + 
			"	WHERE oid=1\r\n" + 
			");",
			
			"SELECT DISTINCT eid, SUM(price) as totalamount\r\n" + 
			"FROM calendar c, schedule s\r\n" + 
			"WHERE c.date=s.date AND s.vid=c.vid\r\n" + 
			"GROUP BY eid\r\n" + 
			"ORDER BY eid DESC;",
			
			"SELECT DISTINCT oid, oname\r\n" + 
			"FROM organization\r\n" + 
			"WHERE oid IN\r\n" + 
			"(\r\n" + 
			"	SELECT DISTINCT oid\r\n" + 
			"	FROM host h\r\n" + 
			"	WHERE h.eid IN\r\n" + 
			"	(\r\n" + 
			"		SELECT DISTINCT s1.eid \r\n" + 
			"		FROM schedule s1, schedule s2, schedule s3\r\n" + 
			"		WHERE s1.eid=s2.eid AND s2.eid=s3.eid AND s1.date<>s2.date AND s1.date <> s3.date AND s2.date <> s3.date\r\n" + 
			"	)\r\n" + 
			")\r\n" + 
			"ORDER BY oid;",
			
			"SELECT DISTINCT v.vid, COALESCE((SELECT COUNT(DISTINCT s.eid) FROM schedule s WHERE s.vid=v.vid),0) AS numevents\r\n" + 
			"FROM venue v\r\n" + 
			"ORDER BY v.vid;",
			
			"SELECT DISTINCT v.vid, COALESCE(COUNT(DISTINCT eid),0) as numevents\r\n" + 
			"FROM venue v LEFT OUTER JOIN schedule s\r\n" + 
			"ON v.vid=s.vid\r\n" + 
			"GROUP BY v.vid\r\n" + 
			"ORDER BY v.vid;",
			
			"SELECT oid\r\n" + 
			"FROM \r\n" + 
			"(\r\n" + 
			"	SELECT o.oid, COUNT(DISTINCT v.vid) AS count\r\n" + 
			"	FROM organization o, host h, event e, schedule s, venue v\r\n" + 
			"	WHERE o.oid=h.oid AND h.eid=e.eid AND e.eid=s.eid AND s.vid=v.vid AND area='C'\r\n" + 
			"	GROUP BY o.oid\r\n" + 
			") x\r\n" + 
			"WHERE count=\r\n" + 
			"(\r\n" + 
			"	SELECT count\r\n" + 
			"	FROM\r\n" + 
			"	(\r\n" + 
			"		SELECT COUNT(DISTINCT vid) as count\r\n" + 
			"		FROM venue\r\n" + 
			"		WHERE area='C'\r\n" + 
			"	) y\r\n" + 
			")\r\n" + 
			"ORDER BY oid;",
			
			"SELECT DISTINCT e.type, AVG(price) as averageamount\r\n" + 
			"FROM calendar c, schedule s, event e\r\n" + 
			"WHERE e.eid=s.eid AND s.vid=c.vid AND s.date=c.date\r\n" + 
			"GROUP BY e.type\r\n" + 
			"ORDER BY e.type;"
	};
}