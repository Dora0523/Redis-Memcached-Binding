package db;
import java.io.FileInputStream;
import java.io.InputStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.File;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {
    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile, final String password) throws Exception {
        InputStream caInputStream = null;
        InputStream crtInputStream = null;
        InputStream keyInputStream = null;

        try {
            Security.addProvider(new BouncyCastleProvider());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // load CA certificate
            caInputStream = new ClassPathResource(caCrtFile).getInputStream();
            X509Certificate caCert = null;
            while (caInputStream.available() > 0) {
                caCert = (X509Certificate) cf.generateCertificate(caInputStream);

            }

            // load client certificate
            crtInputStream = new ClassPathResource(crtFile).getInputStream();
            X509Certificate cert = null;
            while (crtInputStream.available() > 0) {
                cert = (X509Certificate) cf.generateCertificate(crtInputStream);

            }

            // load client private key
            keyInputStream = new ClassPathResource(keyFile).getInputStream();
            if(keyInputStream.available()>0){
            }

            PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream));
            Object object = pemParser.readObject();

            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            KeyPair key;
            if (object instanceof PEMEncryptedKeyPair) {
                System.out.println("Encrypted key - we will use provided password");
                key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
            } else {
                System.out.println("Unencrypted key - no password needed");
                key = converter.getKeyPair((PEMKeyPair) object);

            }
            pemParser.close();

            // CA certificate is used to authenticate server
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(caKs);

            // client key and certificates are sent to server so it can authenticate
            // us

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certificate", cert);
            ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());

            // finally, create SSL socket factory

            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return context.getSocketFactory();

        }
        finally {
            if (null != caInputStream) {
                caInputStream.close();
            }
            if (null != crtInputStream) {
                crtInputStream.close();
            }
            if (null != keyInputStream) {
                keyInputStream.close();
            }
        }
    }

    /****************/
    private static final RedisManager instance = new RedisManager();
    private static JedisPool pool;
    private RedisManager() {}
    public final static RedisManager getInstance() {
        return instance;
    }
    public void connect() throws Exception {
        //String p = System.getProperty("user.dir")+"/src/main/resources/";

        String caCrtFile= "/resources/ca.crt";
        String crtFile= "/resources/redis.crt";
        String keyFile= "/resources/redis.key";

        try {
            SSLSocketFactory socketFactory = getSocketFactory(caCrtFile, crtFile, keyFile,"");
            System.out.println("Connecting SSLSocketFactory");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SSLSocketFactory socketFactory = getSocketFactory(caCrtFile, crtFile, keyFile, "");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        //poolConfig.setMaxActive(60);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setMaxIdle(1);
        poolConfig.setMaxTotal(60);
        poolConfig.setTestWhileIdle(true);

        pool = new JedisPool(poolConfig, "127.0.0.1", 6379, 0, true, socketFactory, null, null);


    }
    public void release() {
        pool.destroy();
    }
    public Jedis getJedis() {
        return pool.getResource();
    }
    public void returnJedis(Jedis jedis) {
        pool.returnResource(jedis);
    }
}