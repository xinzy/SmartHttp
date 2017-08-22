package com.xinzy.http;

import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Created by Xinzy on 2017/7/18.
 */

class LoggingInterceptor implements Interceptor {

    private static final String TAG = SmartHttp.TAG;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public Response intercept(Chain chain) throws IOException {
        StringBuffer sb = new StringBuffer();

        final Request request = chain.request();
        sb.append(logRequest(request));

        final long start = SystemClock.uptimeMillis();
        final Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            sb.append("Http failed").append(e);
            Utils.debug(sb.toString());
            throw e;
        }
        final long take = SystemClock.uptimeMillis() - start;
        sb.append(logResponse(response, take));
        Utils.debug(sb.toString());

        return response;
    }

    private String logRequest(Request request) {
        StringBuffer sb = new StringBuffer().append("----------Request-------\n");
        try {
            sb.append(request.method()).append("  ").append(request.url()).append('\n')
                    .append("Headers --> ").append(request.headers().toString().replace("\n", ";")).append('\n')
                    .append("RequestBody --> ");

            RequestBody body = request.body();
            if (body != null) {

                if (body instanceof FormBody) {
                    Buffer buffer = new Buffer();
                    body.writeTo(buffer);
                    sb.append(buffer.readString(UTF8));
                } else {
                    sb.append(" (binary ").append(body.contentLength()).append("-byte body omitted)");
                }
            }
        } catch (Exception e) {
        }
        sb.append('\n');
        return sb.toString();
    }

    private String logResponse(Response response, long took) {
        StringBuffer sb = new StringBuffer().append("----------Response-------\n");
        try {
            final ResponseBody body = response.body();
            sb.append("ResponseCode --> ").append(response.code()).append(" ContentLength --> ").append(body.contentLength())
                    .append("  took ").append(took).append(" ms").append("\n")
                    .append("Headers --> ").append(response.headers().toString().replace("\n", ";")).append('\n')
                    .append("ResponseBody --> ");

//            BufferedSource source = body.source();
//            Buffer buffer = source.buffer();
//            if (isPlaintext(buffer)) {
//                sb.append(formatOutput(source.readString(UTF8)));
//            } else {
//                sb.append(" (binary ").append(body.contentLength()).append("-byte body omitted)");
//            }
        } catch (Exception e) {
        }
        sb.append('\n');
        return sb.toString();
    }

    private boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false;
        }
    }

    private String formatOutput(String input) {
        try {
            try {
                new JSONObject(input);
                return format(input);
            } catch (Exception e) {
                new JSONArray(input);
                return format(input);
            }
        } catch (Exception e) {
            return input;
        }
    }

    String format(String jsonStr) {
        int level = 0;
        final int length = jsonStr.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            char c = jsonStr.charAt(i);
            if (level > 0 && '\n' == sb.charAt(sb.length() - 1)) {
                sb.append(getBlank(level));
            }
            switch (c) {
                case '{':
                case '[':
                    sb.append(c).append('\n');
                    level++;
                    break;
                case ',':
                    sb.append(c).append('\n');
                    break;
                case '}':
                case ']':
                    sb.append('\n');
                    level--;
                    sb.append(getBlank(level));
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        return sb.toString();
    }

    private String getBlank(int level) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < level; i++) {
            sb.append('\t');
        }
        return sb.toString();
    }
}
