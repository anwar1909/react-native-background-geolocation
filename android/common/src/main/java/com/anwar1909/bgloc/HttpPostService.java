package com.anwar1909.bgloc;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;

public class HttpPostService {
    public static final int BUFFER_SIZE = 1024;

    private String mUrl;
    private HttpURLConnection mHttpURLConnection;

    public interface UploadingProgressListener {
        void onProgress(int progress);
    }

    public HttpPostService(String url) {
        mUrl = url;
    }

    public HttpPostService(final HttpURLConnection httpURLConnection) {
        mHttpURLConnection = httpURLConnection;
    }

    private HttpURLConnection openConnection() throws IOException {
        if (mHttpURLConnection == null) {
            mHttpURLConnection = (HttpURLConnection) new URL(mUrl).openConnection();
        }
        return mHttpURLConnection;
    }

    public String postJSON(JSONObject json, Map<String, String> headers) throws IOException {
        String jsonString = "null";
        if (json != null) {
            jsonString = json.toString();
        }

        return postJSONString(jsonString, headers);
    }
    // public int postJSON(JSONObject json, Map headers) throws IOException {
    //     String jsonString = "null";
    //     if (json != null) {
    //         jsonString = json.toString();
    //     }

    //     return postJSONString(jsonString, headers);
    // }

    public String postJSON(JSONArray json, Map<String, String> headers) throws IOException {
        String jsonString = "null";
        if (json != null) {
            jsonString = json.toString();
        }

        return postJSONString(jsonString, headers);
    }
    // public int postJSON(JSONArray json, Map headers) throws IOException {
    //     String jsonString = "null";
    //     if (json != null) {
    //         jsonString = json.toString();
    //     }

    //     return postJSONString(jsonString, headers);
    // }

    public String postJSONString(String body, Map<String, String> headers) throws IOException {
        if (headers == null) {
            headers = new HashMap();
        }

        HttpURLConnection conn = this.openConnection();
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(body.length());
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        // Tambahkan custom headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }

        // Kirim request body
        try (OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream())) {
            os.write(body);
        }

        // Ambil response code
        int status = conn.getResponseCode();

        // Pilih stream sesuai status
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

        // Baca response body
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        // return conn.getResponseCode();
        return response.toString();
    }

    public int postJSONFile(File file, Map<String, String> headers, UploadingProgressListener listener) throws IOException {
        return postJSONFile(new FileInputStream(file), headers, listener);
    }

    public int postJSONFile(InputStream stream, Map<String, String> headers, UploadingProgressListener listener) throws IOException {
        if (headers == null) {
            headers = new HashMap();
        }

        final long streamSize = stream.available();
        HttpURLConnection conn = this.openConnection();

        conn.setDoInput(false);
        conn.setDoOutput(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            conn.setFixedLengthStreamingMode(streamSize);
        } else {
            conn.setChunkedStreamingMode(0);
        }
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }

        long progress = 0;
        int bytesRead = -1;
        byte[] buffer = new byte[BUFFER_SIZE];

        // BufferedInputStream is = null;
        // BufferedOutputStream os = null;
        try (
            BufferedInputStream is = new BufferedInputStream(stream);
            BufferedOutputStream os = new BufferedOutputStream(conn.getOutputStream())
        ) {

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                os.flush();
                progress += bytesRead;
                int percentage = (int) ((progress * 100L) / streamSize);
                if (listener != null) {
                    listener.onProgress(percentage);
                }
            }
        }

        return conn.getResponseCode();
    }

    public static String postJSON(String url, JSONObject json, Map<String, String> headers) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSON(json, headers);
    }

    public static String postJSON(String url, JSONArray json, Map<String, String> headers) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSON(json, headers);
    }

    public static int postJSONFile(String url, File file, Map<String, String> headers, UploadingProgressListener listener) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSONFile(file, headers, listener);
    }

    public static int postJSONForCode(String url, Object json, Map<String, String> headers) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSONStringForCode(json.toString(), headers);
    }

    private int postJSONStringForCode(String body, Map<String, String> headers) throws IOException {
        if (headers == null) {
            headers = new HashMap<>();
        }

        HttpURLConnection conn = this.openConnection();
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(body.length());
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }

        try (OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream())) {
            os.write(body);
        }

        // langsung return status code
        return conn.getResponseCode();
    }
}
