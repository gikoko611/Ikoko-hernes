package com.ikoko.herness;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(25, 17, 14), PANEL = Color.rgb(43, 29, 23);
    private final int GREEN = Color.rgb(91, 255, 95), GOLD = Color.rgb(224, 181, 82), WHITE = Color.rgb(245, 240, 230);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private EditText baseUrl, apiKey, model, prompt;
    private TextView transcript, status;
    private Button connectButton, sendButton;
    private String conversation = "";
    private static final String PREF = "ikoko_secure_provider";
    private static final String KEY_ALIAS = "IKOKO_HERNESS_AES_KEY";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        restoreProvider();
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(22);
        if (stroke != 0) d.setStroke(2, stroke);
        return d;
    }
    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private EditText field(String hint, boolean multi) {
        EditText e = new EditText(this); e.setSingleLine(!multi); e.setTextColor(WHITE);
        e.setHintTextColor(0xffaa9988); e.setHint(hint); e.setPadding(18, 12, 18, 12);
        e.setBackground(shape(PANEL, 0xff76562d));
        if (multi) { e.setMinLines(3); e.setGravity(Gravity.TOP); }
        return e;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(BG);
        b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(GREEN, GOLD)); return b;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(18, 20, 18, 24);
        root.setBackgroundColor(BG); scroll.addView(root); setContentView(scroll);
        TextView title = label("G.I  IKOKO HERN​ESS", 25, GOLD); root.addView(title);
        TextView sub = label("AI AGENT  •  ARMv7 EDITION", 11, GREEN); sub.setPadding(2, 4, 0, 18); root.addView(sub);
        root.addView(label("PROVIDER SETTINGS", 13, GOLD));
        baseUrl = field("Base URL (e.g. https://api.example.com/v1)", false); root.addView(baseUrl);
        apiKey = field("API key", false); apiKey.setInputType(129); LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(-1, -2); gap.topMargin=9; root.addView(apiKey,gap);
        model = field("Model ID", false); gap = new LinearLayout.LayoutParams(-1,-2); gap.topMargin=9; root.addView(model,gap);
        connectButton = button("SAVE + LOAD MODELS"); gap = new LinearLayout.LayoutParams(-1,-2); gap.topMargin=10; root.addView(connectButton,gap);
        status = label("● Offline / Not connected", 12, GOLD); status.setPadding(2,10,2,16); root.addView(status);
        connectButton.setOnClickListener(v -> connect());
        root.addView(label("AI CHAT", 13, GOLD));
        transcript = new TextView(this); transcript.setTextColor(WHITE); transcript.setTextSize(14);
        transcript.setText("Welcome. Add an OpenAI-compatible provider, then load models.\n");
        transcript.setPadding(14,14,14,14); transcript.setBackground(shape(PANEL,0xff4c3b2a));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, 260); tp.topMargin=8; root.addView(transcript,tp);
        prompt = field("Message the agent…", true); gap = new LinearLayout.LayoutParams(-1,-2); gap.topMargin=10; root.addView(prompt,gap);
        sendButton = button("SEND MESSAGE"); gap = new LinearLayout.LayoutParams(-1,-2); gap.topMargin=10; root.addView(sendButton,gap);
        sendButton.setOnClickListener(v -> sendMessage());
        TextView foot = label("SKILLS  •  FILE TOOLS  •  3D STUDIO  •  VIDEO TOOLS", 10, GREEN);
        foot.setPadding(2,22,2,4); root.addView(foot);
        TextView note = new TextView(this); note.setText("Build 0.1.0 · Provider/chat foundation. Additional agent tools are being added in staged releases."); note.setTextColor(0xffb7a99b); note.setTextSize(11); root.addView(note);
    }

    private void runStatus(String s, int color) { runOnUiThread(() -> { status.setText("● " + s); status.setTextColor(color); }); }
    private String cleanBase() {
        String s = baseUrl.getText().toString().trim();
        while (s.endsWith("/")) s=s.substring(0,s.length()-1);
        if (s.endsWith("/chat/completions")) s=s.substring(0,s.length()-16);
        if (s.endsWith("/models")) s=s.substring(0,s.length()-7);
        return s;
    }
    private HttpURLConnection open(String path) throws Exception {
        URL u = new URL(cleanBase() + path);
        HttpURLConnection c = (HttpURLConnection)u.openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(45000);
        c.setRequestProperty("Authorization","Bearer "+apiKey.getText().toString().trim());
        c.setRequestProperty("Content-Type","application/json");
        return c;
    }
    private void connect() {
        String url=cleanBase(), key=apiKey.getText().toString().trim();
        if (url.isEmpty() || key.isEmpty()) { runStatus("Enter Base URL and API key", GOLD); return; }
        saveProvider(); connectButton.setEnabled(false); runStatus("Loading model list…", GREEN);
        worker.execute(() -> {
            HttpURLConnection c=null;
            try {
                c=open("/models"); c.setRequestMethod("GET");
                int code=c.getResponseCode(); String body=read(c,code);
                if (code<200 || code>=300) throw new Exception("HTTP "+code+": "+body);
                JSONObject json=new JSONObject(body); JSONArray data=json.optJSONArray("data");
                if (data!=null && data.length()>0) {
                    String current=model.getText().toString().trim();
                    if (current.isEmpty()) model.setText(data.getJSONObject(0).optString("id",""));
                }
                runStatus("Connected · Models loaded: "+(data==null?0:data.length()), GREEN);
                append("\n[System] Provider connection successful.\n");
            } catch(Exception e) { runStatus("Connection failed: "+e.getMessage(), 0xffff7777); }
            finally { if(c!=null)c.disconnect(); runOnUiThread(()->connectButton.setEnabled(true)); }
        });
    }
    private void sendMessage() {
        final String question=prompt.getText().toString().trim();
        if(question.isEmpty()) return;
        if(cleanBase().isEmpty() || apiKey.getText().toString().trim().isEmpty() || model.getText().toString().trim().isEmpty()) {
            runStatus("Set provider URL, API key and model first", GOLD); return;
        }
        prompt.setText(""); append("\nYOU: "+question+"\n\nG.I: thinking…\n");
        runStatus("Request in progress…", GREEN); sendButton.setEnabled(false);
        worker.execute(() -> {
            HttpURLConnection c=null;
            try {
                JSONArray messages=new JSONArray();
                if(!conversation.isEmpty()) {
                    JSONArray old=new JSONArray(conversation);
                    for(int i=Math.max(0,old.length()-12);i<old.length();i++) messages.put(old.getJSONObject(i));
                }
                JSONObject user=new JSONObject(); user.put("role","user"); user.put("content",question); messages.put(user);
                JSONObject req=new JSONObject(); req.put("model",model.getText().toString().trim()); req.put("messages",messages); req.put("temperature",0.7);
                c=open("/chat/completions"); c.setRequestMethod("POST"); c.setDoOutput(true);
                try(OutputStream os=c.getOutputStream()){ os.write(req.toString().getBytes(StandardCharsets.UTF_8)); }
                int code=c.getResponseCode(); String body=read(c,code);
                if(code<200 || code>=300) throw new Exception("HTTP "+code+": "+body);
                JSONObject response=new JSONObject(body); String answer=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","(empty response)");
                JSONObject assistant=new JSONObject(); assistant.put("role","assistant"); assistant.put("content",answer); messages.put(assistant); conversation=messages.toString();
                append("\nG.I: "+answer+"\n"); runStatus("Response received",GREEN);
            } catch(Exception e) { append("\n[Error] "+e.getMessage()+"\n"); runStatus("Request failed",0xffff7777); }
            finally { if(c!=null)c.disconnect(); runOnUiThread(()->sendButton.setEnabled(true)); }
        });
    }
    private String read(HttpURLConnection c,int code) throws Exception {
        InputStream in=(code>=200 && code<400)?c.getInputStream():c.getErrorStream();
        if(in==null)return ""; ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[4096]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n);
        return out.toString("UTF-8");
    }
    private void append(String s) { runOnUiThread(()->transcript.append(s)); }
    private void saveProvider() {
        try {
            String enc=encrypt(apiKey.getText().toString());
            getSharedPreferences(PREF,0).edit().putString("url",baseUrl.getText().toString()).putString("model",model.getText().toString()).putString("key",enc).apply();
        } catch(Exception e) { runStatus("Secure key storage failed: "+e.getMessage(),0xffff7777); }
    }
    private void restoreProvider() {
        try {
            android.content.SharedPreferences p=getSharedPreferences(PREF,0);
            baseUrl.setText(p.getString("url","")); model.setText(p.getString("model",""));
            String enc=p.getString("key","");
            if(!enc.isEmpty()) apiKey.setText(decrypt(enc));
        } catch(Exception e) { runStatus("Saved provider could not be unlocked",GOLD); }
    }
    private SecretKey getSecretKey() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(ks.containsAlias(KEY_ALIAS)) return ((SecretKey)ks.getKey(KEY_ALIAS,null));
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return kg.generateKey();
    }
    private String encrypt(String plain) throws Exception {
        if(plain.isEmpty())return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,getSecretKey());
        byte[] data=cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(data,Base64.NO_WRAP);
    }
    private String decrypt(String value) throws Exception {
        String[] p=value.split(":",2); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,getSecretKey(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    @Override protected void onDestroy(){ super.onDestroy(); worker.shutdownNow(); }
}
