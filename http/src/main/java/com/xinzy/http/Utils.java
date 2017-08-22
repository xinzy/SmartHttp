package com.xinzy.http;

import android.content.Context;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.WebSettings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cookie;
import okhttp3.Headers;

/**
 * Created by Xinzy on 2017/5/19.
 */

class Utils {

    static void debug(String msg) {
        if (SmartHttp.isDebug) {
            Log.i(SmartHttp.TAG, msg);
        }
    }

    static void e(String msg, Exception e) {
        if (SmartHttp.isDebug) {
            Log.e(SmartHttp.TAG, msg, e);
        }
    }

    static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    static boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    static String splitParam(HttpParam param) {
        StringBuffer sb = new StringBuffer();
        if (param.params.size() > 0) {
            Set<String> keys = param.params.keySet();
            for (String key : keys) {
                HttpParam.Entry entry = param.params.get(key);
                sb.append(entry.key).append("=").append(entry.value).append('&');
            }
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    static String getUserAgent(Context context) {
        String userAgent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                userAgent = WebSettings.getDefaultUserAgent(context);
            } catch (Exception e) {
                userAgent = System.getProperty("http.agent");
            }
        } else {
            userAgent = System.getProperty("http.agent");
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = userAgent.length(); i < length; i++) {
            char c = userAgent.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static boolean write(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static String read(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    static Map<String, String> parseHeader(Headers headers) {
        Map<String, String> header = new ArrayMap<>();
        Set<String> names = headers.names();
        for (String name : names) {
            header.put(name, headers.get(name));
        }
        return header;
    }

    static SSLParams sslSocketFactory(X509TrustManager trustManager, InputStream bksFile, String password, InputStream[] certificates) {
        SSLParams sslParams = new SSLParams();
        try {
            KeyManager[]     keyManagers   = prepareKeyManager(bksFile, password);
            TrustManager[]   trustManagers = prepareTrustManager(certificates);
            X509TrustManager manager;
            if (trustManager != null) {//优先使用用户自定义的TrustManager
                manager = trustManager;
            } else if (trustManagers != null) {//然后使用默认的TrustManager
                manager = chooseTrustManager(trustManagers);
            } else { //否则使用不安全的TrustManager
                manager = new TrustAllManager();
            }
            // 创建TLS类型的SSLContext对象， that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            // 用上面得到的trustManagers初始化SSLContext，这样sslContext就会信任keyStore中的证书
            // 第一个参数是授权的密钥管理器，用来授权验证，比如授权自签名的证书验证。第二个是被授权的证书管理器，用来验证服务器端的证书
            sslContext.init(keyManagers, new TrustManager[]{manager}, null);
            // 通过sslContext获取SSLSocketFactory对象
            sslParams.sSLSocketFactory = sslContext.getSocketFactory();
            sslParams.trustManager = manager;
            return sslParams;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static KeyManager[] prepareKeyManager(InputStream bksFile, String password) {
        try {
            if (bksFile == null || password == null) return null;
            KeyStore clientKeyStore = KeyStore.getInstance("BKS");
            clientKeyStore.load(bksFile, password.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientKeyStore, password.toCharArray());
            return kmf.getKeyManagers();
        } catch (Exception e) {
        }
        return null;
    }

    private static TrustManager[] prepareTrustManager(InputStream... certificates) {
        if (certificates == null || certificates.length <= 0) return null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            // 创建一个默认类型的KeyStore，存储我们信任的证书
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certStream : certificates) {
                String certificateAlias = Integer.toString(index++);
                // 证书工厂根据证书文件的流生成证书 cert
                Certificate cert = certificateFactory.generateCertificate(certStream);
                // 将 cert 作为可信证书放入到keyStore中
                keyStore.setCertificateEntry(certificateAlias, cert);
                try {
                    if (certStream != null) certStream.close();
                } catch (IOException e) {
                }
            }
            //我们创建一个默认类型的TrustManagerFactory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            //用我们之前的keyStore实例初始化TrustManagerFactory，这样tmf就会信任keyStore中的证书
            tmf.init(keyStore);
            //通过tmf获取TrustManager数组，TrustManager也会信任keyStore中的证书
            return tmf.getTrustManagers();
        } catch (Exception e) {
        }
        return null;
    }

    private static X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    static class SSLParams {
        public SSLSocketFactory sSLSocketFactory;
        public X509TrustManager trustManager;
    }
}
