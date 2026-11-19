package com.ikoko.herness;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.provider.MediaStore;
import android.view.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.media.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.*;
import android.util.Base64;

public class MainActivity extends Activity {
    private final int BG=0xff160f0c, PANEL=0xff2b1d17, GREEN=0xff62ff72, GOLD=0xffe5bd67, WHITE=0xfff5eee4;
    private LinearLayout root, content, nav;
    private EditText baseUrl, apiKey, model, chatInput, projectName, projectBody, terminalInput, trimStart, trimEnd;
    private TextView status, transcript, selectedVideo;
    private Button sendBtn;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private Future<?> activeTask;
    private Uri videoUri;
    private String conversation="";
    private String page="chat";
    private final int PICK_SKILL=11, PICK_PROJECT=12, PICK_VIDEO=13;
    private File skillsDir, projectsDir;
    private static final String PREF="ikoko_secure_provider";
    private static final String KEY_ALIAS="IKOKO_HERNESS_AES_KEY";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        skillsDir=new File(getFilesDir(),"skills"); skillsDir.mkdirs();
        projectsDir=new File(getFilesDir(),"projects"); projectsDir.mkdirs();
        buildShell(); showPage("chat"); restoreProvider();
    }

    private GradientDrawable shape(int color,int stroke){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(22);
        if(stroke!=0)d.setStroke(2,stroke); return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v;
    }
    private EditText field(String hint,boolean multi){
        EditText e=new EditText(this); e.setHint(hint); e.setTextColor(WHITE); e.setHintTextColor(0xffaa9988);
        e.setPadding(16,12,16,12); e.setBackground(shape(PANEL,0xff76562d)); e.setSingleLine(!multi);
        if(multi){e.setMinLines(4);e.setGravity(Gravity.TOP);} return e;
    }
    private Button button(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(BG);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(shape(GREEN,GOLD)); return b;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout.LayoutParams lp(int h){return new LinearLayout.LayoutParams(-1,h);}
    private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,h));}
    private void addField(LinearLayout p,EditText e){LinearLayout.LayoutParams q=lp(-2);q.topMargin=8;p.addView(e,q);}
    private void buildShell(){
        ScrollView outer=new ScrollView(this); outer.setFillViewport(true); root=column(); root.setPadding(16,16,16,24);
        root.setBackgroundColor(BG); outer.addView(root); setContentView(outer);
        TextView title=text("G.I  IKOKO HERN\u200bESS",25,GOLD,true); root.addView(title);
        TextView sub=text("AI HARNESS  •  ANDROID 10  •  ARMv7",10,GREEN,true);root.addView(sub);
        status=text("● READY  |  Local tools available",11,GOLD,true);gap(root,8);root.addView(status);
        HorizontalScrollView hs=new HorizontalScrollView(this);nav=new LinearLayout(this);nav.setOrientation(0);hs.addView(nav);
        String[][] items={{"AI Agent","chat"},{"Providers","provider"},{"Skills","skills"},{"Projects","projects"},{"Tools","tools"},{"3D Studio","3d"},{"Video","video"}};
        for(String[] it:items){Button b=button(it[0]);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-2,48);q.rightMargin=6;q.topMargin=10;nav.addView(b,q);b.setOnClickListener(v->showPage(it[1]));}
        root.addView(hs);gap(root,10);content=column();root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        TextView foot=text("POWERED BY I KO KO  •  MIDNIGHT GOLD / NEON GREEN",9,GOLD,true);gap(root,20);root.addView(foot);
    }
    private void header(String title,String detail){
        content.removeAllViews();content.addView(text(title,19,GOLD,true));
        TextView d=text(detail,11,0xffc4b4a4,false);gap(content,4);content.addView(d);gap(content,12);
    }
    private void setStatus(String s,int color){runOnUiThread(()->{status.setText("● "+s);status.setTextColor(color);});}
    private void showPage(String p){
        page=p;
        if(p.equals("provider"))providerPage();
        else if(p.equals("skills"))skillsPage();
        else if(p.equals("projects"))projectsPage();
        else if(p.equals("tools"))toolsPage();
        else if(p.equals("3d"))studioPage();
        else if(p.equals("video"))videoPage();
        else chatPage();
    }

    private void providerPage(){
        header("PROVIDER MANAGER","OpenAI-compatible API · encrypted key storage · automatic model discovery");
        baseUrl=field("Base URL, e.g. https://api.example.com/v1",false);addField(content,baseUrl);
        apiKey=field("API Key (encrypted at rest)",false);apiKey.setInputType(129);addField(content,apiKey);
        model=field("Model ID (auto-filled from /models)",false);addField(content,model);
        Button save=button("SAVE + TEST + LOAD MODELS");gap(content,10);content.addView(save,lp(-2));
        save.setOnClickListener(v->connectProvider());
        restoreProvider();
        TextView info=text("Provider values are stored on this device. API keys use Android Keystore AES-GCM. Never share your key.",11,0xffc7b9aa,false);gap(content,10);content.addView(info);
    }
    private String cleanBase(){
        String s=baseUrl.getText().toString().trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);
        if(s.endsWith("/chat/completions"))s=s.substring(0,s.length()-16);
        if(s.endsWith("/models"))s=s.substring(0,s.length()-7);return s;
    }
    private HttpURLConnection request(String path,String method)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(cleanBase()+path).openConnection();
        c.setConnectTimeout(18000);c.setReadTimeout(90000);c.setRequestMethod(method);
        c.setRequestProperty("Authorization","Bearer "+apiKey.getText().toString().trim());
        c.setRequestProperty("Content-Type","application/json");return c;
    }
    private String response(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();InputStream in=(code>=200&&code<400)?c.getInputStream():c.getErrorStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream();if(in!=null){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}
        String s=out.toString("UTF-8");if(code<200||code>=300)throw new Exception("HTTP "+code+" "+s);return s;
    }
    private void connectProvider(){
        if(baseUrl.getText().length()==0||apiKey.getText().length()==0){setStatus("Enter Base URL and API key",GOLD);return;}
        saveProvider();setStatus("Connecting / loading models…",GREEN);
        io.execute(()->{HttpURLConnection c=null;try{
            c=request("/models","GET");JSONObject j=new JSONObject(response(c));JSONArray a=j.optJSONArray("data");
            if(a!=null&&a.length()>0){String first=a.getJSONObject(0).optString("id","");runOnUiThread(()->{if(model.getText().length()==0)model.setText(first);});}
            setStatus("Provider connected · "+(a==null?0:a.length())+" models",GREEN);
        }catch(Exception e){setStatus("Provider error: "+e.getMessage(),0xffff7777);}finally{if(c!=null)c.disconnect();}});
    }
    private void chatPage(){
        header("AI AGENT","Chat · task planning · code assistance · provider-powered reasoning");
        Button provider=button("OPEN PROVIDER SETTINGS");content.addView(provider,lp(-2));provider.setOnClickListener(v->showPage("provider"));
        transcript=text("Welcome to IKOKO HERN\u200bESS.\nConfigure a provider to begin. Local tools are available from the Tools, Skills and Projects tabs.",13,WHITE,false);
        transcript.setPadding(14,14,14,14);transcript.setBackground(shape(PANEL,0xff72552e));
        LinearLayout.LayoutParams tp=lp(300);tp.topMargin=10;content.addView(transcript,tp);
        chatInput=field("Ask G.I to plan, explain or generate code…",true);addField(content,chatInput);
        LinearLayout row=new LinearLayout(this);row.setOrientation(0);sendBtn=button("SEND");Button stop=button("STOP");
        row.addView(sendBtn,new LinearLayout.LayoutParams(0,52,2));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,52,1);sp.leftMargin=8;row.addView(stop,sp);gap(content,8);content.addView(row);
        sendBtn.setOnClickListener(v->sendChat());stop.setOnClickListener(v->{if(activeTask!=null)activeTask.cancel(true);setStatus("Task cancellation requested",GOLD);sendBtn.setEnabled(true);});
        TextView note=text("Note: provider chat is request/response in this build. Autonomous multi-step execution and live token streaming are not enabled yet.",10,0xffb6a99b,false);gap(content,8);content.addView(note);
    }
    private void appendChat(String s){runOnUiThread(()->{if(transcript!=null){transcript.append(s);}});}
    private void sendChat(){
        String q=chatInput.getText().toString().trim();if(q.isEmpty())return;
        if(baseUrl==null||apiKey==null||model==null||cleanBase().isEmpty()||apiKey.getText().length()==0||model.getText().length()==0){
            Toast.makeText(this,"Configure provider and model first",Toast.LENGTH_LONG).show();showPage("provider");return;
        }
        chatInput.setText("");appendChat("\n\nYOU: "+q+"\n\nG.I: working…");sendBtn.setEnabled(false);setStatus("AI request running",GREEN);
        activeTask=io.submit(()->{HttpURLConnection c=null;try{
            JSONArray msgs=new JSONArray();JSONObject sys=new JSONObject();sys.put("role","system");
            sys.put("content","You are G.I KO KO AI assistant. Help plan tasks and produce safe Android/Termux/GitHub code. Explain destructive actions and request approval.");
            msgs.put(sys);if(!conversation.isEmpty()){JSONArray old=new JSONArray(conversation);for(int i=Math.max(0,old.length()-10);i<old.length();i++)msgs.put(old.getJSONObject(i));}
            JSONObject u=new JSONObject();u.put("role","user");u.put("content",q);msgs.put(u);
            JSONObject body=new JSONObject();body.put("model",model.getText().toString().trim());body.put("messages",msgs);body.put("temperature",0.6);
            c=request("/chat/completions","POST");c.setDoOutput(true);try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            JSONObject r=new JSONObject(response(c));String answer=r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","No content");
            JSONObject a=new JSONObject();a.put("role","assistant");a.put("content",answer);msgs.put(a);conversation=msgs.toString();
            appendChat("\n"+answer);setStatus("AI response received",GREEN);
        }catch(Exception e){appendChat("\n[ERROR] "+e.getMessage());setStatus("AI request failed",0xffff7777);}
        finally{if(c!=null)c.disconnect();runOnUiThread(()->{if(sendBtn!=null)sendBtn.setEnabled(true);});}});
    }

    private void skillsPage(){
        header("SKILLS MANAGER","Import .md / .skill / .zip · inspect SKILL.md · safe ZIP extraction");
        Button importBtn=button("IMPORT SKILL FILE");content.addView(importBtn,lp(-2));importBtn.setOnClickListener(v->pick(PICK_SKILL,"*/*"));
        gap(content,10);Button refresh=button("REFRESH SKILLS");content.addView(refresh,lp(-2));refresh.setOnClickListener(v->skillsPage());
        gap(content,10);File[] fs=skillsDir.listFiles();if(fs==null||fs.length==0){content.addView(text("No skills imported yet.",12,WHITE,false));return;}
        for(File f:fs){LinearLayout box=column();box.setPadding(12,10,12,10);box.setBackground(shape(PANEL,0xff60472c));content.addView(box,lp(-2));box.addView(text(f.getName(),14,GOLD,true));
            TextView desc=text(f.isDirectory()?"Skill folder":"Skill document",10,0xffc8b9a9,false);box.addView(desc);
            LinearLayout r=new LinearLayout(this);Button open=button("VIEW");Button del=button("DELETE");r.addView(open,new LinearLayout.LayoutParams(0,46,1));r.addView(del,new LinearLayout.LayoutParams(0,46,1));box.addView(r);
            open.setOnClickListener(v->viewSkill(f));del.setOnClickListener(v->{deleteTree(f);skillsPage();});
        }
    }
    private void pick(int code,String type){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType(type);i.addCategory(Intent.CATEGORY_OPENABLE);if(code==PICK_SKILL)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/markdown","text/plain","application/zip","application/octet-stream"});startActivityForResult(i,code);}
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();
        if(req==PICK_SKILL)io.execute(()->importSkill(u));
        else if(req==PICK_PROJECT)io.execute(()->loadProject(u));
        else if(req==PICK_VIDEO){videoUri=u;runOnUiThread(()->{if(selectedVideo!=null)selectedVideo.setText("Selected: "+displayName(u));});}
    }
    private String displayName(Uri u){String n="selected file";try(CursorLike c=new CursorLike(getContentResolver().query(u,null,null,null,null))){if(c.cursor!=null&&c.cursor.moveToFirst()){int i=c.cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.cursor.getString(i);}}catch(Exception ignored){}return n;}
    private static class CursorLike implements AutoCloseable{android.database.Cursor cursor;CursorLike(android.database.Cursor c){cursor=c;}public void close(){if(cursor!=null)cursor.close();}}
    private void importSkill(Uri u){
        try{String name=displayName(u);File dest=new File(skillsDir,safeName(name));if(name.toLowerCase().endsWith(".zip")){
            File tmp=new File(getCacheDir(),"skill_import.zip");copyUri(u,tmp);unzipSafe(tmp,new File(skillsDir,stripExt(safeName(name))));tmp.delete();
        }else{copyUri(u,dest);}
            setStatus("Skill imported: "+name,GREEN);runOnUiThread(()->skillsPage());
        }catch(Exception e){setStatus("Skill import failed: "+e.getMessage(),0xffff7777);}
    }
    private String safeName(String s){return s.replaceAll("[^A-Za-z0-9._-]","_");}
    private String stripExt(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}
    private void copyUri(Uri u,File f)throws Exception{try(InputStream in=getContentResolver().openInputStream(u);FileOutputStream out=new FileOutputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}}
    private void unzipSafe(File zip,File dest)throws Exception{
        String root=dest.getCanonicalPath()+File.separator;dest.mkdirs();
        try(ZipInputStream in=new ZipInputStream(new FileInputStream(zip))){ZipEntry e;byte[] b=new byte[8192];while((e=in.getNextEntry())!=null){
            File out=new File(dest,e.getName());String p=out.getCanonicalPath();if(!p.startsWith(root))throw new SecurityException("Unsafe ZIP path");
            if(e.isDirectory())out.mkdirs();else{File par=out.getParentFile();if(par!=null)par.mkdirs();try(FileOutputStream os=new FileOutputStream(out)){int n;while((n=in.read(b))>0)os.write(b,0,n);}}
            in.closeEntry();
        }}
    }
    private void viewSkill(File f){
        try{File target=f.isDirectory()?new File(f,"SKILL.md"):f;if(!target.exists()){Toast.makeText(this,"SKILL.md not found in folder",Toast.LENGTH_LONG).show();return;}
            String s=readFile(target);new AlertDialog.Builder(this).setTitle(f.getName()).setMessage(s.length()>12000?s.substring(0,12000):s).setPositiveButton("CLOSE",null).show();
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void projectsPage(){
        header("PROJECT WORKSPACE","Create and edit source files · import existing text · local project storage");
        projectName=field("Project / filename (e.g. hello.py)",false);addField(content,projectName);
        projectBody=field("Write code or project notes here…",true);addField(content,projectBody);
        Button save=button("SAVE FILE");gap(content,8);content.addView(save,lp(-2));save.setOnClickListener(v->saveProject());
        Button open=button("IMPORT / OPEN TEXT FILE");gap(content,8);content.addView(open,lp(-2));open.setOnClickListener(v->pick(PICK_PROJECT,"text/*"));
        gap(content,12);content.addView(text("Saved files",13,GOLD,true));
        File[] fs=projectsDir.listFiles();if(fs!=null)for(File f:fs){Button b=button("OPEN  "+f.getName());gap(content,5);content.addView(b,lp(-2));b.setOnClickListener(v->{try{projectName.setText(f.getName());projectBody.setText(readFile(f));}catch(Exception e){}});}
    }
    private void saveProject(){
        try{String n=safeName(projectName.getText().toString().trim());if(n.isEmpty())throw new Exception("Filename required");
            File f=new File(projectsDir,n);try(FileOutputStream o=new FileOutputStream(f)){o.write(projectBody.getText().toString().getBytes(StandardCharsets.UTF_8));}
            Toast.makeText(this,"Saved "+f.getName(),Toast.LENGTH_SHORT).show();setStatus("Project saved",GREEN);
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void loadProject(Uri u){try{String n=safeName(displayName(u));File f=new File(projectsDir,n);copyUri(u,f);runOnUiThread(()->{showPage("projects");projectName.setText(n);try{projectBody.setText(readFile(f));}catch(Exception ignored){}});}catch(Exception e){setStatus("Open failed: "+e.getMessage(),0xffff7777);}}
    private String readFile(File f)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(f)){byte[] b=new byte[4096];int n;while((n=in.read(b))>0)o.write(b,0,n);}return o.toString("UTF-8");}
    private void deleteTree(File f){if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File x:a)deleteTree(x);}f.delete();}

    private void toolsPage(){
        header("TOOLS & AGENT CAPABILITIES","Local utilities · safe terminal · code generator · GitHub integration");
        toolButton("SAFE TERMINAL","Run limited read-only commands",()->terminalDialog());
        toolButton("GENERATE BLENDER PYTHON","Create a .py scene script for Blender desktop",()->blenderDialog());
        toolButton("GITHUB REPOSITORY","Open IKOKO HERN\u200bESS source repository",()->openUrl("https://github.com/gikoko611/Ikoko-hernes"));
        toolButton("PROJECT FILES","Open project editor",()->showPage("projects"));
        toolButton("SKILLS LIBRARY","Manage imported agent skills",()->showPage("skills"));
        toolButton("VIDEO WORKSPACE","Open local video tools",()->showPage("video"));
        TextView warning=text("Terminal is deliberately restricted; no unrestricted shell or root execution. GitHub operations open the repository in the browser. Blender rendering requires Blender on another supported computer.",10,0xffc8b9a9,false);gap(content,10);content.addView(warning);
    }
    private void toolButton(String title,String sub,Runnable run){LinearLayout box=column();box.setPadding(12,10,12,10);box.setBackground(shape(PANEL,0xff654b2b));content.addView(box,lp(-2));box.addView(text(title,13,GOLD,true));box.addView(text(sub,10,0xffc5b6a6,false));Button b=button("OPEN");gap(box,6);box.addView(b,lp(-2));b.setOnClickListener(v->run.run());gap(content,8);}
    private void terminalDialog(){
        LinearLayout l=column();terminalInput=field("help | pwd | ls | date",false);l.addView(terminalInput);
        new AlertDialog.Builder(this).setTitle("Restricted Terminal").setView(l).setPositiveButton("RUN",(d,w)->runTerminal(terminalInput.getText().toString().trim())).setNegativeButton("CANCEL",null).show();
    }
    private void runTerminal(String cmd){
        String out;if(cmd.equals("help"))out="Allowed: help, pwd, ls, date";else if(cmd.equals("pwd"))out=getFilesDir().getAbsolutePath();
        else if(cmd.equals("date"))out=new Date().toString();else if(cmd.equals("ls")){String[] a=projectsDir.list();out=a==null?"(empty)":Arrays.toString(a);}
        else out="Blocked. Only help, pwd, ls and date are permitted.";
        new AlertDialog.Builder(this).setTitle("Terminal Output").setMessage(out).setPositiveButton("OK",null).show();
    }
    private void blenderDialog(){
        EditText name=field("Scene name",false);new AlertDialog.Builder(this).setTitle("Blender Python Script").setMessage("Generate a starter Blender scene script. Blender must be installed externally to render it.").setView(name)
        .setPositiveButton("GENERATE",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty())n="IKOKO_Scene";String py="import bpy\\n\\n# "+n+" - generated by IKOKO HERN\\u200bESS\\nbpy.ops.object.select_all(action='SELECT')\\nbpy.ops.object.delete(use_global=False)\\nbpy.ops.mesh.primitive_cube_add(size=2, location=(0,0,1))\\ncube=bpy.context.object\\ncube.name='"+n.replace("'","")+"'\ncube.modifiers.new('Bevel','BEVEL').width=0.08\\ncube.modifiers.new('WeightedNormals','WEIGHTED_NORMAL')\\nbpy.ops.object.camera_add(location=(5,-7,5))\\nbpy.context.scene.camera=bpy.context.object\\nbpy.context.scene.render.resolution_x=1280\\nbpy.context.scene.render.resolution_y=720\\nbpy.context.scene.render.resolution_percentage=100\\n";
            try{File f=new File(projectsDir,safeName(n)+".py");try(FileOutputStream o=new FileOutputStream(f)){o.write(py.getBytes(StandardCharsets.UTF_8));}Toast.makeText(this,"Saved "+f.getName(),Toast.LENGTH_LONG).show();}catch(Exception e){} }).setNegativeButton("CANCEL",null).show();
    }
    private void openUrl(String u){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}

    private void studioPage(){
        header("3D STUDIO","WebGL preview · interactive scene · works best with internet for Three.js CDN");
        WebView w=new WebView(this);w.setBackgroundColor(BG);w.getSettings().setJavaScriptEnabled(true);w.getSettings().setDomStorageEnabled(true);
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{margin:0;background:#160f0c;color:#e5bd67;font-family:sans-serif}#v{height:360px}button{background:#62ff72;border:0;padding:12px;margin:8px;border-radius:8px}</style></head><body><div id='v'></div><p>G.I 3D SCENE PREVIEW</p><button onclick='spin=!spin'>PAUSE / ROTATE</button><script src='https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.min.js'></script><script>let spin=true;let el=document.getElementById('v');if(window.THREE){let s=new THREE.Scene();s.background=new THREE.Color(0x160f0c);let c=new THREE.PerspectiveCamera(70,innerWidth/360,.1,100);c.position.z=4;let r=new THREE.WebGLRenderer({antialias:true});r.setSize(innerWidth,360);el.appendChild(r.domElement);let g=new THREE.BoxGeometry(),m=new THREE.MeshStandardMaterial({color:0x62ff72,metalness:.5,roughness:.25}),o=new THREE.Mesh(g,m);s.add(o);s.add(new THREE.HemisphereLight(0xffffff,0x554422,2));function a(){requestAnimationFrame(a);if(spin){o.rotation.x+=.01;o.rotation.y+=.015}r.render(s,c)}a()}else{el.innerHTML='<p>Three.js could not load. Check internet connection.</p>'}</script></body></html>";
        w.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);content.addView(w,lp(390));
    }

    private void videoPage(){
        header("VIDEO WORKSPACE","Select MP4 · inspect duration · trim/remux without a cloud render service");
        Button choose=button("SELECT VIDEO");content.addView(choose,lp(-2));choose.setOnClickListener(v->pick(PICK_VIDEO,"video/*"));
        selectedVideo=text(videoUri==null?"No video selected": "Selected: "+displayName(videoUri),12,WHITE,false);gap(content,8);content.addView(selectedVideo);
        trimStart=field("Start time (seconds, default 0)",false);addField(content,trimStart);
        trimEnd=field("End time (seconds, required)",false);addField(content,trimEnd);
        Button trim=button("TRIM / EXPORT MP4");gap(content,10);content.addView(trim,lp(-2));trim.setOnClickListener(v->trimVideo());
        TextView note=text("This build uses Android MediaExtractor/MediaMuxer for MP4 stream-copy trimming. It does not bundle FFmpeg, multi-track timeline, audio effects or full re-encoding.",10,0xffc6b8a8,false);gap(content,10);content.addView(note);
    }
    private void trimVideo(){
        if(videoUri==null){Toast.makeText(this,"Select a video first",Toast.LENGTH_LONG).show();return;}
        final long start,end;try{start=(long)(Double.parseDouble(trimStart.getText().toString().isEmpty()?"0":trimStart.getText().toString())*1000000L);end=(long)(Double.parseDouble(trimEnd.getText().toString())*1000000L);if(start<0||end<=start)throw new Exception("Invalid time range");}catch(Exception e){Toast.makeText(this,"Enter valid start and end seconds",Toast.LENGTH_LONG).show();return;}
        setStatus("Trimming video…",GREEN);io.execute(()->{try{File out=new File(getExternalFilesDir(null),"IKOKO_trim_"+System.currentTimeMillis()+".mp4");remuxTrim(videoUri,out,start,end);
            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Export complete").setMessage("Saved to app storage:\n"+out.getAbsolutePath()).setPositiveButton("OK",null).show());setStatus("Video exported",GREEN);
        }catch(Exception e){setStatus("Video export failed: "+e.getMessage(),0xffff7777);}});
    }
    private void remuxTrim(Uri uri,File out,long start,long end)throws Exception{
        MediaExtractor ex=new MediaExtractor();ex.setDataSource(this,uri,null);int count=ex.getTrackCount();MediaMuxer mux=new MediaMuxer(out.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int[] map=new int[count];Arrays.fill(map,-1);for(int i=0;i<count;i++){android.media.MediaFormat f=ex.getTrackFormat(i);String mime=f.getString(android.media.MediaFormat.KEY_MIME);if(mime!=null&&(mime.startsWith("video/")||mime.startsWith("audio/"))){map[i]=mux.addTrack(f);ex.selectTrack(i);}}
        mux.start();ByteBuffer buf=ByteBuffer.allocate(2*1024*1024);MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        while(true){int track=ex.getSampleTrackIndex();if(track<0)break;long t=ex.getSampleTime();if(t>end)break;if(t>=start&&map[track]>=0){buf.clear();int n=ex.readSampleData(buf,0);if(n<0)break;info.offset=0;info.size=n;info.presentationTimeUs=t-start;info.flags=ex.getSampleFlags();mux.writeSampleData(map[track],buf,info);}ex.advance();}
        mux.stop();mux.release();ex.release();
    }

    private SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias(KEY_ALIAS))return (SecretKey)ks.getKey(KEY_ALIAS,null);
        KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();
    }
    private String encrypt(String s)throws Exception{if(s.isEmpty())return "";Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] b=c.doFinal(s.getBytes(StandardCharsets.UTF_8));return Base64.encodeToString(c.getIV(),2)+":"+Base64.encodeToString(b,2);}
    private String decrypt(String s)throws Exception{String[] p=s.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],2)));return new String(c.doFinal(Base64.decode(p[1],2)),StandardCharsets.UTF_8);}
    private void saveProvider(){try{String enc=encrypt(apiKey.getText().toString());getSharedPreferences(PREF,0).edit().putString("url",baseUrl.getText().toString()).putString("model",model.getText().toString()).putString("key",enc).apply();}catch(Exception e){setStatus("Secure storage failed",0xffff7777);}}
    private void restoreProvider(){try{android.content.SharedPreferences p=getSharedPreferences(PREF,0);if(baseUrl!=null){baseUrl.setText(p.getString("url",""));model.setText(p.getString("model",""));String k=p.getString("key","");if(!k.isEmpty())apiKey.setText(decrypt(k));}}catch(Exception e){setStatus("Saved key could not be decrypted",GOLD);}}
    @Override protected void onDestroy(){super.onDestroy();io.shutdownNow();}
}
