package com.dsh.ledger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** OpenAI 兼容的 /chat/completions 客户端（不依赖第三方库） */
public class AiClient {

    public static class Msg {
        public final String role;
        public final String content;
        public Msg(String r, String c) { role = r; content = c; }
    }

    /** 允许用户只填 base（https://api.deepseek.com/v1），也允许直接填完整 endpoint */
    public static String endpoint(String base) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) return b;
        return b + "/chat/completions";
    }

    public static String chat(String base, String key, String model, List<Msg> msgs,
                              String effort) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", 0.6);
        if (effort != null && effort.length() > 0) body.put("reasoning_effort", effort);
        JSONArray arr = new JSONArray();
        for (Msg m : msgs) {
            JSONObject o = new JSONObject();
            o.put("role", m.role);
            o.put("content", m.content);
            arr.put(o);
        }
        body.put("messages", arr);

        HttpURLConnection c = (HttpURLConnection) new URL(endpoint(base)).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(90000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("Authorization", "Bearer " + key.trim());
        c.setRequestProperty("Accept", "application/json");

        OutputStream os = c.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        int code = c.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        String text = read(is);
        if (code < 200 || code >= 300) {
            String msg = text;
            try {
                JSONObject err = new JSONObject(text);
                if (err.has("error")) {
                    JSONObject e = err.getJSONObject("error");
                    msg = e.optString("message", text);
                }
            } catch (Exception ignored) { }
            throw new RuntimeException("HTTP " + code + "：" + brief(msg));
        }

        JSONObject j = new JSONObject(text);
        JSONArray choices = j.optJSONArray("choices");
        if (choices == null || choices.length() == 0) throw new RuntimeException("接口没有返回内容");
        JSONObject m = choices.getJSONObject(0).optJSONObject("message");
        String content = m != null ? m.optString("content", "") : "";
        if (content.trim().length() == 0) throw new RuntimeException("模型返回了空内容");
        return content.trim();
    }

    /** GET {base}/models，返回服务端支持的模型 id 列表 */
    public static java.util.List<String> models(String base, String key) throws Exception {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) b = b.substring(0, b.length() - "/chat/completions".length());
        String url = b + "/models";

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + key.trim());
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        String text = read(is);
        if (code < 200 || code >= 300) throw new RuntimeException("HTTP " + code + "：" + brief(text));

        java.util.List<String> out = new java.util.ArrayList<String>();
        JSONObject j = new JSONObject(text);
        JSONArray data = j.optJSONArray("data");
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject o = data.optJSONObject(i);
                if (o == null) continue;
                String id = o.optString("id", o.optString("name", ""));
                if (id.length() > 0) out.add(id);
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    private static String brief(String s) {
        if (s == null) return "";
        s = s.replace("\n", " ").trim();
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }

    private static String read(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return new String(bos.toByteArray(), "UTF-8");
    }
}
