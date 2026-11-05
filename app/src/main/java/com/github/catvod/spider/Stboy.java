package com.github.catvod.spider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.catvod.Runtime;
import com.github.catvod.bean.Class;
import com.github.catvod.bean.Style;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.Download;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Response;
import com.github.catvod.utils.Task;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;

public class Stboy extends Spider {

    private static final String HOST = "https://www.stboy.net/";
    private static final String COVER = "https://img.icons8.com/?size=100&id=L9VNoA5emll8&format=png&color=000000";
    private static final String PROXY_PREFIX = "https://xf.stre.sryze.cc/";
    private static final String SITE_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String BD_UA_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String BD_UA_NET = "netdisk;12.24.6;piano;android-android;16;JSbridge4.4.0;jointBridge;1.1.0";
    private static final String DEFAULT_CK = "HstCfa3668293=1786123040751;HstCmu3668293=1786123040751;HstCnv3668293=1;HstCns3668293=1;HwLz_2132_saltkey=Q7aAffco;HwLz_2132_lastvisit=1786119503;HwLz_2132_sendmail=1;HwLz_2132_seccodecStxsYj4=6411.be348b2c87bf8c6bec;HwLz_2132_ulastactivity=3ff9PJ%2BQng48NadIHcp%2BDTbQN4pizBa2yvg63kY3tIenOsydFaih;HwLz_2132_auth=6d06zsMtHlhh2qCTC7hr78CRWnr91SJ%2BDQAmWQozQ2DcX9yfQztNjc1qfJgzTfm3NNSwTQsrOSdQOdipKnEgaEgG0LPY;HwLz_2132_lastcheckfeed=2762406%7C1786123105;HwLz_2132_onlineusernum=25843;HstCla3668293=1786123107126;HstPn3668293=7;HstPt3668293=7;HwLz_2132_checkpm=1;HwLz_2132_sid=xMuYG4;HwLz_2132_lip=2a09%3Abac1%3A19e0%3A48%3A%3A26a%3Acd%2C1786123107;HwLz_2132_lastact=1786123107%09misc.php%09patch";

    private static final String[][] CLASSES = {{"百度☁️", "467::916"}, {"配置⚙️", "config"}, {"磁力🧲", "magnet_folder"}, {"BL📀", "474"}, {"小说📖", "all"}, {"漫画🎨", "157"}, {"图库🩸", "134"}};
    private static final String[][] MAGNET_TYPES = {{"亚洲", "35"}, {"欧美", "28"}, {"台湾", "44"}, {"日本有码", "42"}, {"日本无码", "43"}};
    private static final Set<String> GALLERY_FIDS = Set.of("157", "134", "135", "136", "155", "263", "264", "265", "296", "297", "304", "305", "268", "269", "266", "267", "270", "271", "272", "273", "274", "275", "276", "277", "278", "279", "280", "281", "300", "298", "242", "292", "293", "295", "294");

    private static final Pattern RE_TID = Pattern.compile("(?:tid=|thread-)(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_FID = Pattern.compile("(?:fid=|forum-)(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_MAGNET = Pattern.compile("magnet:\\?xt=urn:btih:[^\\s<\"'\\(\\)]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_ED2K = Pattern.compile("ed2k://\\|file\\|[^\\r\\n<\"'\\(\\)]+?\\|/", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_EU_SPAM = Pattern.compile("\\bGirls\\s+(?:In|From)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAN_PAT = Pattern.compile("(?:https?://)?(?:(?:pan|yun)\\.baidu\\.com/(?:s/[A-Za-z0-9_-]+|share/init\\?surl=[A-Za-z0-9_-]+)|pan\\.quark\\.cn/s/[A-Za-z0-9_-]+|(?:fast|drive|pan)\\.uc\\.cn/s/[A-Za-z0-9_-]+|pan\\.xunlei\\.com/s/[A-Za-z0-9_#-]+|(?:www\\.)?(?:alipan|aliyundrive)\\.com/s/[A-Za-z0-9_-]+|115\\.com/s/[A-Za-z0-9_-]+|cloud\\.189\\.cn/(?:t/|web/share\\?code=)[A-Za-z0-9_-]+|(?:caiyun|yun)\\.139\\.com/w/[A-Za-z0-9_-]+|(?:mypikpak|pikpak)\\.(?:com|me)/s/[A-Za-z0-9_-]+|mega\\.nz/(?:file|folder)/[A-Za-z0-9_#-]+|[a-zA-Z0-9-]+\\.(?:lanzou[a-z]|lanzou)\\.com/[A-Za-z0-9_-]+|(?:[a-zA-Z0-9-]+\\.)?v8l\\.cn/s/[A-Za-z0-9_-]+)(?:\\?[A-Za-z0-9_&=%#-]+)?", Pattern.CASE_INSENSITIVE);
    private static final String HTML_TEMPLATE = """
<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover"><style>:root{--bg:#0b0d0e;--bg2:#111314;--fg:#e6dfd2;--muted:#9b9183;--gold:#d8c39c;--line:rgba(216,195,156,.28);--fs:16px;--lh:1.86;--side:30px;--ctl:rgba(12,13,14,.94)}[data-theme=night]{--bg:#0b0d0e;--bg2:#111314;--fg:#e6dfd2;--muted:#9b9183;--gold:#d8c39c;--line:rgba(216,195,156,.28)}[data-theme=paper]{--bg:#f4ecd8;--bg2:#f4ecd8;--fg:#2f2a22;--muted:#857967;--gold:#7b5a2e;--line:rgba(123,90,46,.22);--ctl:rgba(30,26,21,.94)}[data-theme=green]{--bg:#dcebd6;--bg2:#dcebd6;--fg:#203024;--muted:#6d796e;--gold:#526b42;--line:rgba(82,107,66,.22);--ctl:rgba(23,32,25,.94)}*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}html,body{margin:0;width:100%;height:100%;background:var(--bg);color:var(--fg);font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Noto Sans CJK SC","Microsoft YaHei",sans-serif;overflow:hidden;user-select:none}.reader{position:fixed;inset:0;overflow-y:auto;background:radial-gradient(circle at 50% -8%,rgba(255,255,255,.055),transparent 36%),linear-gradient(180deg,var(--bg2),var(--bg));padding:calc(26px + env(safe-area-inset-top)) var(--side) calc(86px + env(safe-area-inset-bottom));scroll-behavior:smooth}.chapter-title{text-align:center;font-weight:700;font-size:1.16em;line-height:1.46;margin:14px 0 24px;color:var(--gold);letter-spacing:.035em}.chapter-title:after{content:"";display:block;width:54%;max-width:260px;height:1px;margin:18px auto 0;background:linear-gradient(90deg,transparent,var(--line),transparent)}.chapter-meta{text-align:center;color:var(--muted);font-size:.72em;margin:-12px 0 30px}.paragraph{font-size:var(--fs);line-height:var(--lh);letter-spacing:.04em;text-align:justify;text-indent:2em;margin:0 0 1.22em;word-break:break-word}.end{text-align:center;color:var(--muted);font-size:12px;opacity:.5;margin:34px 0 0}.footer{position:fixed;left:0;right:0;bottom:0;z-index:50;padding:0 var(--side) calc(10px + env(safe-area-inset-bottom));pointer-events:none;background:linear-gradient(180deg,transparent,rgba(0,0,0,.32))}.progress{height:1px;background:rgba(230,223,210,.18);margin-bottom:10px;position:relative}.progress i{position:absolute;left:0;top:-3px;width:7px;height:7px;border-radius:50%;background:var(--fg)}.footrow{display:flex;align-items:center;justify-content:space-between;color:var(--muted);font-size:12px}#controls{position:fixed;left:12px;right:12px;bottom:12px;background:var(--ctl);color:#fff;padding:14px 14px calc(16px + env(safe-area-inset-bottom));border-radius:22px;transform:translateY(135%);transition:.25s;z-index:999;display:flex;flex-direction:column;gap:12px;box-shadow:0 -8px 30px rgba(0,0,0,.36);backdrop-filter:blur(12px)}#controls.active{transform:translateY(0)}.row{display:flex;justify-content:space-between;align-items:center;gap:10px}.btns{display:flex;background:rgba(255,255,255,.12);border-radius:22px;padding:4px;flex:1}.btn{flex:1;text-align:center;padding:9px 0;font-size:14px;border-radius:18px}.nav{background:rgba(255,255,255,.12);border-radius:13px;padding:11px 18px;font-weight:700;min-width:68px;text-align:center}.info{font-size:14px;padding:8px 12px;background:rgba(255,255,255,.1);border-radius:12px}.themes{display:flex;justify-content:space-around;width:100%;padding-bottom:14px}.dot{width:34px;height:34px;border-radius:50%;border:2px solid rgba(255,255,255,.25);position:relative}.dot.active{border-color:#fff;transform:scale(1.12)}.dot:after{content:attr(data-label);position:absolute;bottom:-18px;left:50%;transform:translateX(-50%);font-size:10px;opacity:.72;white-space:nowrap}#catalog{position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:2000;display:none;opacity:0;transition:.25s}#catalog.show{display:block;opacity:1}.panel{position:absolute;bottom:0;left:0;width:100%;height:72%;background:var(--bg2);color:var(--fg);border-radius:22px 22px 0 0;display:flex;flex-direction:column;transform:translateY(100%);transition:.25s}.show .panel{transform:translateY(0)}.head{padding:16px;text-align:center;font-weight:700;border-bottom:1px solid rgba(128,128,128,.16);font-size:17px;color:var(--gold)}.list{flex:1;overflow-y:auto;padding:8px 12px}.item{padding:15px 12px;border-bottom:1px solid rgba(128,128,128,.08);font-size:16px}.item.active{color:var(--gold);font-weight:700;background:rgba(255,255,255,.055);border-radius:10px}.close{position:absolute;right:18px;top:15px;font-size:24px;color:var(--muted)}</style></head><body data-theme="night"><div class="reader" id="reader" onclick="toggleMenu()"><div id="content">Loading...</div><div class="end">-- END --</div></div><div class="footer"><div class="progress"><i id="knob"></i></div><div class="footrow"><span id="clock">--:--</span><span><span id="cur2">1</span>/<span id="tot2">1</span>章</span><span id="percent">0.0%</span></div></div><div id="catalog" onclick="toggleCatalog()"><div class="panel" onclick="event.stopPropagation()"><div class="head">目录 <span class="close" onclick="toggleCatalog()">×</span></div><div class="list" id="catlist"></div></div></div><div id="controls" onclick="event.stopPropagation()"><div class="row"><div class="nav" onclick="prevChapter()">上一章</div><div class="info" onclick="toggleCatalog()">目录 <span id="cur">1</span>/<span id="tot">1</span></div><div class="nav" onclick="nextChapter()">下一章</div></div><div class="row"><span style="font-size:14px;opacity:.72">字号</span><div class="btns"><div class="btn" onclick="size(-1)">A-</div><div class="btn" onclick="size(0)">默认</div><div class="btn" onclick="size(1)">A+</div></div></div><div class="row"><span style="font-size:14px;opacity:.72">行距</span><div class="btns"><div class="btn" onclick="line(-.06)">紧凑</div><div class="btn" onclick="line(0)">标准</div><div class="btn" onclick="line(.06)">宽松</div></div></div><div class="themes"><div class="dot active" style="background:#111314" data-theme="night" data-label="夜间" onclick="theme('night')"></div><div class="dot" style="background:#f4ecd8" data-theme="paper" data-label="纸张" onclick="theme('paper')"></div><div class="dot" style="background:#dcebd6" data-theme="green" data-label="护眼" onclick="theme('green')"></div></div></div><script>const bookData=###BOOK_DATA###,content=document.getElementById('content'),root=document.documentElement,body=document.body,cur=document.getElementById('cur'),tot=document.getElementById('tot'),cur2=document.getElementById('cur2'),tot2=document.getElementById('tot2'),reader=document.getElementById('reader'),catlist=document.getElementById('catlist'),knob=document.getElementById('knob'),percent=document.getElementById('percent'),clock=document.getElementById('clock');let idx=0,fs=16,lh=1.86;function now(){let d=new Date();clock.innerText=String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0')}function init(){now();setInterval(now,30000);if(!bookData||!bookData.length){content.innerHTML='<p class="paragraph">Empty</p>';return}tot.innerText=bookData.length;tot2.innerText=bookData.length;catlist.innerHTML=bookData.map((c,i)=>{let d=document.createElement('div');d.innerHTML=c;let t=d.querySelector('.chapter-title');return `<div class="item" id="cat-${i}" onclick="jump(${i})">${t?t.innerText:'第 '+(i+1)+' 节'}</div>`}).join('');render(0)}function render(i){if(i<0||i>=bookData.length)return;idx=i;cur.innerText=i+1;cur2.innerText=i+1;content.innerHTML=bookData[i];reader.scrollTop=0;updateProgress();document.querySelectorAll('.item').forEach(e=>e.classList.remove('active'));let a=document.getElementById('cat-'+i);if(a){a.classList.add('active');a.scrollIntoView({block:'center'})}}function updateProgress(){let p=bookData.length?((idx+1)/bookData.length*100):0;percent.innerText=p.toFixed(1)+'%';knob.style.left=Math.max(0,Math.min(100,p))+'%'}reader.addEventListener('scroll',()=>{let max=reader.scrollHeight-reader.clientHeight,base=bookData.length?idx/bookData.length*100:0,part=max>0?(reader.scrollTop/max)*(100/bookData.length):0,p=base+part;percent.innerText=Math.max(0,Math.min(100,p)).toFixed(1)+'%';knob.style.left=Math.max(0,Math.min(100,p))+'%'},{passive:true});function prevChapter(){if(idx>0)render(idx-1)}function nextChapter(){if(idx<bookData.length-1)render(idx+1)}function toggleMenu(){document.getElementById('controls').classList.toggle('active')}function toggleCatalog(){let p=document.getElementById('catalog');p.classList.toggle('show');document.getElementById('controls').classList.remove('active')}function jump(i){render(i);toggleCatalog()}function size(d){fs=d?Math.max(12,Math.min(26,fs+d)):16;root.style.setProperty('--fs',fs+'px')}function line(d){lh=d?Math.max(1.5,Math.min(2.1,lh+d)):1.86;root.style.setProperty('--lh',lh)}function theme(t){root.setAttribute('data-theme',t);body.setAttribute('data-theme',t);document.querySelectorAll('.dot').forEach(e=>e.classList.remove('active'));let x=document.querySelector(`.dot[data-theme="${t}"]`);if(x)x.classList.add('active')}init();</script></body></html>""";

    private static final String IMG_TEMPLATE = """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover"><meta name="referrer" content="no-referrer"><style>html,body{margin:0;background:#050505;color:#fff;font-family:-apple-system,BlinkMacSystemFont,sans-serif;min-height:100%;overscroll-behavior:none}.container{display:grid;grid-template-columns:1fr;gap:10px;padding:10px 10px calc(18px + env(safe-area-inset-bottom))}.item{width:100%;background:#111;border-radius:10px;overflow:hidden}.item img{width:100%;height:auto;display:block;min-height:60px;background:#111}.overlay{position:fixed;inset:0;background:#000;display:none;align-items:center;justify-content:center;z-index:1000;touch-action:pan-y}.overlay.active{display:flex}.overlay img{width:auto;height:auto;max-width:100%;max-height:100%;object-fit:contain}.counter{position:fixed;left:50%;bottom:calc(18px + env(safe-area-inset-bottom));transform:translateX(-50%);font-size:12px;color:rgba(255,255,255,.78);background:rgba(0,0,0,.36);padding:5px 10px;border-radius:999px;z-index:1002}</style></head><body><div class="container">###IMG_TAGS###</div><div id="overlay" class="overlay" onclick="closeZoom()"><img id="z"><div class="counter" id="counter"></div></div><script>let imgs=[],idx=0,startX=0,startY=0,ov=document.getElementById('overlay'),z=document.getElementById('z'),counter=document.getElementById('counter');function toggleZoom(i){imgs=Array.from(document.querySelectorAll('.container img'));idx=imgs.indexOf(i);show();ov.classList.add('active')}function closeZoom(){ov.classList.remove('active')}function show(){if(idx<0||idx>=imgs.length)return;z.src=imgs[idx].src;counter.innerText=(idx+1)+' / '+imgs.length}function chg(d){let n=idx+d;if(n>=0&&n<imgs.length){idx=n;show()}}ov.addEventListener('touchstart',e=>{let t=e.changedTouches[0];startX=t.clientX;startY=t.clientY},{passive:true});ov.addEventListener('touchend',e=>{let t=e.changedTouches[0],dx=t.clientX-startX,dy=t.clientY-startY;if(Math.abs(dx)>55&&Math.abs(dx)>Math.abs(dy)*1.2)chg(dx<0?1:-1)},{passive:true});function retryImage(img){let r=parseInt(img.getAttribute('data-retry')||'0');if(r>=5)return;img.setAttribute('data-retry',r+1);setTimeout(()=>{let c=img.src.replace(/([?&])retry=\\d+/,'');img.src=c+(c.includes('?')?'&':'?')+'retry='+Date.now()},1500*(r+1))}</script></body></html>""";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, String> fidMap = new HashMap<>();
    private volatile boolean destroyed;
    private DownloadDialog downloadDialog;
    private String extBaiduCookie = "";
    private String extSiteCookie = "";
    private String bdCookieMem = "";

    {
        for (String fid : GALLERY_FIDS) fidMap.put(fid, "comic::{tid}");
        fidMap.put("474", "thread::{tid}::pan");
        fidMap.put("467", "thread::{tid}::baidu");
        fidMap.put("27", "thread::{tid}::video");
        fidMap.put("videofid", "thread::{tid}::video");
        fidMap.put("videotype", "thread::{tid}::video");
        fidMap.put("video", "thread::{tid}::video");
        fidMap.put("all", "novel::{tid}");
        fidMap.put("224", "novel::{tid}");
        for (String[] t : MAGNET_TYPES) fidMap.put(t[1], "thread::{tid}::video");
    }

    @Override
    public void init(Context context, String extend) {
        if (extend == null || extend.isEmpty()) return;
        try {
            JsonObject ex = JsonParser.parseString(extend).getAsJsonObject();
            String v = ex.has("baidu_cookie") ? ex.get("baidu_cookie").getAsString() : ex.has("bduss") ? ex.get("bduss").getAsString() : "";
            if (!v.isEmpty() && !v.contains("BDUSS=") && !v.contains("=")) v = "BDUSS=" + v;
            extBaiduCookie = v;
            if (ex.has("cookie")) extSiteCookie = ex.get("cookie").getAsString();
        } catch (Exception ignored) {
            if (extend.contains("BDUSS=")) extBaiduCookie = extend;
        }
    }

    @Override
    public String homeContent(boolean filter) {
        List<Class> classes = new ArrayList<>();
        for (String[] c : CLASSES) {
            Class item = Class.create().id(c[1]).name(c[0]);
            if (c[1].equals("config")) item.flag("1");
            classes.add(item);
        }
        return Response.create().classes(classes).string();
    }

    @Override
    public String homeVideoContent() {
        return Response.create().vods(List.of()).string();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        int page = pg == null || pg.isEmpty() ? 1 : Integer.parseInt(pg);
        if (tid.equals("config")) return Response.create().page().vods(configCards()).string();
        if (tid.startsWith("baidupan::")) return baiduShare(tid.substring(10), page);
        if (tid.equals("magnet_folder")) {
            List<Vod> items = new ArrayList<>();
            for (String[] t : MAGNET_TYPES) items.add(folderCard("video::" + t[1], t[0]));
            return page(items);
        }
        if (tid.equals("134")) return page == 1 ? page(subforums("134")) : parseList(fetchDoc("forum.php?mod=forumdisplay&fid=134&page=" + page), page, "134");
        if (tid.startsWith("video::")) return routeVideo(tid.substring(7), page);
        if (tid.startsWith("thread::")) {
            String[] parts = tid.split("::");
            return page(threadImages(parts[1], parts.length > 2 ? parts[2] : "normal"));
        }
        if (tid.startsWith("comic::")) return page(gallery(tid.substring(7)));
        if (tid.startsWith("gallery::")) return page(gallery(tid.substring(9)));
        if (tid.startsWith("novel::")) return page(novel(tid.substring(7)));
        if (tid.startsWith("read::")) return page(novel(tid.substring(6)));
        return defaultList(tid, page);
    }

    private String defaultList(String tid, int pg) throws Exception {
        String[] parts = tid.split("::");
        String fid = parts[0];
        String typeid = parts.length > 1 ? parts[1] : "";
        String path = !typeid.isEmpty()
        ? "forum.php?mod=forumdisplay&fid=" + fid + "&filter=typeid&typeid=" + typeid + "&page=" + pg
        : "forum.php?mod=forumdisplay&fid=" + (tid.equals("all") ? "224" : tid) + "&page=" + pg;
        return parseList(fetchDoc(path), pg, tid);
    }

    private String routeVideo(String val, int pg) throws Exception {
        String res = parseList(fetchDoc("forum.php?mod=forumdisplay&fid=" + val + "&page=" + pg), pg, "videofid::" + val);
        if (!JsonParser.parseString(res).getAsJsonObject().getAsJsonArray("list").isEmpty()) return res;
        return parseList(fetchDoc("forum.php?mod=forumdisplay&fid=27&filter=typeid&typeid=" + val + "&page=" + pg), pg, "videotype::" + val);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) {
        return Response.create().page().vods(List.of()).string();
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        List<Vod> items = new ArrayList<>();
        for (String id : ids) items.add(detailOne(id));
        return Response.create().vods(items).string();
    }

    private Vod detailOne(String fid) throws Exception {
        String prefix = fid.contains("::") ? fid.substring(0, fid.indexOf("::")) : fid;
        String tid = fid.contains("::") ? fid.substring(fid.indexOf("::") + 2).split("::")[0] : fid;
        if (prefix.equals("read") || prefix.equals("novel")) {
            novel(tid);
            return Vod.create().id(fid).name("Read").playFrom("Read").playUrl("Play$" + fid);
        }
        if (prefix.equals("gallery") || prefix.equals("comic")) {
            gallery(tid);
            return Vod.create().id(fid).name("Gallery").playFrom("Gallery").playUrl("Play$" + fid);
        }
        return threadDetail(fid, tid);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String prefix = id.contains("::") ? id.substring(0, id.indexOf("::")) : id;
        String tid = id.contains("::") ? id.substring(id.indexOf("::") + 2).split("::")[0] : id;
        if (prefix.equals("read") || prefix.equals("novel")) novel(tid);
        if (prefix.equals("gallery") || prefix.equals("comic")) gallery(tid);
        String url = id.toLowerCase(Locale.ROOT).startsWith("ed2k://") || id.toLowerCase(Locale.ROOT).startsWith("magnet:?") ? id : "";
        return Response.create().url(url).header(Map.of("User-Agent", SITE_UA)).string();
    }

    @Override
    public String action(String action) throws Exception {
        if (action.equals("baidu_menu")) return baiduMenu();
        if (action.equals("baidu_clear")) {
            bdClear();
            return Response.notify("已清除百度 Cookie");
        }
        if (action.equals("site_cookie")) return siteCookieDialog();
        if (action.equals("baidu_help")) return Response.notify("百度分类点帖子封面 → 分享文件列表 → 点文件即取直链，弹下载窗下载到 Download 目录（可暂停/取消）");
        if (action.startsWith("bdl/")) return baiduDownload(action.substring(4));
        return Response.notify("不支持此操作");
    }

    @Override
    public void destroy() {
        destroyed = true;
        main.post(() -> {
            if (downloadDialog != null) downloadDialog.close();
        });
    }

    private String page(List<Vod> items) {
        return Response.create().page().vods(items).string();
    }

    private String page(List<Vod> items, int pg, int pagecount, int total) {
        return Response.create().page(pg, pagecount, items.size(), total).vods(items).string();
    }

    private Vod folderCard(String id, String name) {
        return Vod.create().id(id).name(name).pic(COVER).remarks("").tag("folder").style(Style.create().type("rect").ratio(1.0f));
    }

    private Vod listCard(String id, String name, String remarks) {
        return Vod.create().id(id).name(name).pic(COVER).remarks(remarks).style(Style.create().type("list").ratio(1.1f));
    }
    private String siteCookie() {
        String v = local.get("site_cookie");
        if (v != null && !v.isEmpty()) {
            try {
                return new JSONObject(v).optString("v");
            } catch (Exception ignored) {
            }
        }
        return extSiteCookie.isEmpty() ? DEFAULT_CK : extSiteCookie;
    }

    private Map<String, String> siteHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", SITE_UA);
        headers.put("Referer", HOST);
        headers.put("Cookie", siteCookie());
        return headers;
    }

    private String proxify(String u) {
        if (u == null || u.isEmpty()) return "";
        if (u.startsWith("//")) u = "https:" + u;
        if ((u.startsWith("http://") || u.startsWith("https://")) && !u.startsWith(PROXY_PREFIX)) return PROXY_PREFIX + u;
        return u;
    }

    private String abs(String u) {
        if (u == null) return "";
        u = u.replace("&amp;", "&").trim();
        if (u.isEmpty()) return "";
        try {
            return java.net.URI.create(HOST).resolve(u).toString();
        } catch (Exception e) {
            return u;
        }
    }

    private String fetch(String path) throws Exception {
        return net.get(proxify(abs(path)), siteHeaders());
    }

    private Document fetchDoc(String path) throws Exception {
        return Jsoup.parse(fetch(path), HOST);
    }

    private String clean(String s) {
        if (s == null) return "";
        String t = s.replaceAll("&##(\\d+);", "&#$1;");
        t = t.replaceAll("&\\s*#(\\d+);", "&#$1;");
        t = org.jsoup.parser.Parser.unescapeEntities(t, false);
        return t.replaceAll("\\s+", " ").trim();
    }

    private String tidOf(String href) {
        if (href == null) return "";
        Matcher m = RE_TID.matcher(href.replace("&amp;", "&"));
        return m.find() ? m.group(1) : "";
    }

    private String fidOf(String href) {
        if (href == null) return "";
        Matcher m = RE_FID.matcher(href.replace("&amp;", "&"));
        return m.find() ? m.group(1) : "";
    }

    private String b64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private String ub64(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }

    private String imageSrc(String u) {
        if (u == null || u.isEmpty()) return "";
        try {
            return proxyUrl(Map.of("url", b64(u), "type", "img"));
        } catch (Exception e) {
            return proxify(u);
        }
    }

    private String parseList(Document doc, int pg, String currentFid) {
        List<Vod> items = new ArrayList<>();
        for (Element row : doc.select("tbody[id^=normalthread_]")) {
            Element a = row.select("a.s.xst, a.xst").first();
            if (a == null) continue;
            String tid = tidOf(a.attr("href"));
            String title = clean(a.text());
            if (tid.isEmpty() || skipThread(title, currentFid)) continue;
            Element num = row.select("td.num").first();
            String remark = "";
            if (num != null) {
                Element xi2 = num.select("a.xi2").first();
                remark = xi2 != null ? xi2.text() : num.text().replace("\n", " ");
            }
            items.add(item(tid, title, remark, currentFid));
        }
        int pagecount = pg;
        boolean hasNum = false;
        for (Element a : doc.select(".pg a")) {
            String t = a.text().trim();
            if (t.matches("\\d+")) {
                pagecount = Math.max(pagecount, Integer.parseInt(t));
                hasNum = true;
            }
        }
        if (!hasNum) pagecount = !doc.select("a.nxt, .pgbtn").isEmpty() ? pg + 1 : pg;
        return page(items, pg, pagecount, 999999);
    }

    private boolean skipThread(String title, String fid) {
        String f = fid == null ? "" : fid;
        return (f.contains("28") || f.endsWith("::28")) && RE_EU_SPAM.matcher(clean(title)).find();
    }

    private Vod item(String tid, String title, String remark, String fid) {
        String f = (fid == null ? "" : fid).split("::")[0];
        String tpl = fidMap.getOrDefault(f, "novel::{tid}");
        return Vod.create().id(tpl.replace("{tid}", tid)).name(title.isEmpty() ? "Thread " + tid : title).pic(COVER).remarks(remark == null ? "" : remark).tag("folder").style(Style.create().type("list").ratio(1.1f));
    }

    private List<Vod> subforums(String fid) throws Exception {
        List<Vod> out = new ArrayList<>();
        for (Element item : fetchDoc("forum.php?mod=forumdisplay&fid=" + fid).select(".fl_g")) {
            Element a = item.select("dl dt a").first();
            if (a == null) continue;
            String x = fidOf(a.attr("href"));
            if (!x.isEmpty()) out.add(folderCard(x, clean(a.text())));
        }
        return out;
    }

    private List<Vod> threadImages(String tid, String mode) throws Exception {
        String raw = fetch("forum.php?mod=viewthread&tid=" + tid);
        Document doc = Jsoup.parse(raw, HOST);
        Element subj = doc.select("#thread_subject").first();
        String title = subj == null ? "" : clean(subj.text());
        if (title.isEmpty()) title = "-";
        List<String> imgs = images(raw);
        String first = imgs.isEmpty() ? COVER : imageSrc(imgs.get(0));
        List<Vod> cards = new ArrayList<>();
        if (mode.equals("baidu")) {
            List<String> bdLinks = new ArrayList<>();
            for (String u : extractLinks(raw)[0]) if (u.contains("baidu.com")) bdLinks.add(u);
            if (!bdLinks.isEmpty()) {
                for (int i = 0; i < bdLinks.size(); i++) {
                    String[] sp = bdParse(bdLinks.get(i));
                    String tok = b64(new JSONObject().put("surl", sp[0]).put("pwd", sp[1]).put("dir", "/").toString());
                    cards.add(Vod.create().id("baidupan::" + tok).name(i == 0 ? title : title + " · 分享" + (i + 1)).pic(first).remarks("百度网盘 · 点开看文件").tag("folder").style(Style.create().type("rect").ratio(1.33f)));
                }
                for (int i = 0; i < imgs.size(); i++) cards.add(Vod.create().id("comic::" + tid).name("Pic [" + (i + 1) + "]").pic(imageSrc(imgs.get(i))).remarks("").tag("folder").style(Style.create().type("rect").ratio(1.33f)));
                return cards;
            }
        }
        String prefix = mode.equals("baidu") ? "playbaidu" : mode.equals("pan") ? "playpan" : "play";
        cards.add(Vod.create().id(prefix + "::" + tid + "::source").name(title).pic(first).remarks("").style(Style.create().type("rect").ratio(1.33f)));
        for (int i = 0; i < imgs.size(); i++) cards.add(Vod.create().id(prefix + "::" + tid + "::img" + (i + 1)).name("Pic [" + (i + 1) + "]").pic(imageSrc(imgs.get(i))).remarks("").style(Style.create().type("rect").ratio(1.33f)));
        return cards;
    }

    private List<String> images(String raw) {
        Document doc = Jsoup.parse(raw == null ? "" : raw, HOST);
        Element post = doc.select("#postlist .plhin .pcb").first();
        if (post == null) post = doc.body();
        post.select(".quote, .pstatus, .jammer, font.jammer, script, style, .sign").remove();
        List<String> imgs = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String[] badSrc = {"rleft.gif", "rright.gif", "none.gif", "static/image", "smiley", "avatar"};
        String[] badWords = {"红丝带", "上帝的恩赐", "珍爱生命", "三菱重工"};
        for (Element img : post.select("img")) {
            String src = img.attr("zoomfile");
            if (src.isEmpty()) src = img.attr("file");
            if (src.isEmpty()) src = img.attr("src");
            src = src.replace("&amp;", "&").trim();
            if (src.isEmpty() || src.startsWith("data:") || src.startsWith("javascript:")) continue;
            String low = src.toLowerCase(Locale.ROOT);
            boolean bad = false;
            for (String k : badSrc) if (low.contains(k)) bad = true;
            String alt = img.attr("alt") + img.attr("title");
            for (String w : badWords) if (alt.contains(w)) bad = true;
            if (bad) continue;
            String u = abs(src);
            String key = u.contains("&aid=") ? u.split("&aid=")[1].split("&")[0] : u.split("\\?")[0].toLowerCase(Locale.ROOT);
            if (!seen.add(key)) continue;
            imgs.add(u);
        }
        return imgs;
    }
    private String[][] extractLinks(String raw) {
        Document doc = Jsoup.parse(raw == null? "": raw, HOST);
        Element post = doc.select("#postlist .plhin .pcb, #postlist .pcb, .pcb, table[id^=pid] td.t_f").first();
        String scope = post == null? doc.body().html(): post.html();
        String norm = scope.replaceAll("(?is)<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", " $1 $2 ");
        String text = org.jsoup.parser.Parser.unescapeEntities(norm.replaceAll("<[^>]+>", " "), false).replace('　', ' ').replaceAll("\\s+", " ");
        Map<String, String> pan = new LinkedHashMap<>();
        Matcher m = PAN_PAT.matcher(text);
        while (m.find()) {
            String u = m.group(0).replaceAll("[.,;，。；、)\\]}'\"#]+$", "");
            if (!u.startsWith("http://") &&!u.startsWith("https://")) u = "https://" + u;
            String baseU = u.split("\\?")[0];
            String ctx = text.substring(Math.max(0, m.start() - 80), Math.min(text.length(), m.end() + 80));
            Matcher pm = Pattern.compile("(?:pwd|提取码|提取|访问码|code)[：:\\s]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE).matcher(ctx);
            if (!pm.find()) {
                pm = Pattern.compile("(?<!解压)密码[：:\\s]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE).matcher(ctx);
                if (!pm.find()) pm = null;
            }
            if (u.contains("v8l.cn")) {
                if (pm!= null) u = u + " 提取码：" + pm.group(1);
            } else if (pm!= null &&!u.contains("pwd=") &&!u.contains("code=")) {
                u += (u.contains("?")? "&pwd=": "?pwd=") + pm.group(1);
            }
            boolean hasPwd = u.contains("pwd=") || u.contains("提取码：");
            String old = pan.get(baseU);
            boolean oldHas = old!= null && (old.contains("pwd=") || old.contains("提取码："));
            if (old == null || (hasPwd &&!oldHas)) pan.put(baseU, u);
        }
        Set<String> ed = new LinkedHashSet<>();
        Matcher mm = RE_MAGNET.matcher(text);
        while (mm.find()) ed.add(mm.group(0).replaceAll("[.,;，。；、)\\]}'\"]+$", ""));
        Matcher me = RE_ED2K.matcher(text);
        while (me.find()) ed.add(me.group(0).trim());
        String unzip = "";
        Matcher um = Pattern.compile("(?:解压密码|解压码)[：:\\s]*([A-Za-z0-9_@#!$%^&*.-]+)").matcher(text);
        if (!um.find()) {
            um = Pattern.compile("解压.*?密码[：:\\s]*([A-Za-z0-9_@#!$%^&*.-]+)").matcher(text);
            if (!um.find()) um = null;
        }
        if (um!= null) unzip = um.group(1).replaceAll("[.,;，。；、-]+$", "");
        return new String[][]{pan.values().toArray(new String[0]), ed.toArray(new String[0]), new String[]{unzip}};
    }

    private Vod threadDetail(String fid, String tid) throws Exception {
        String raw = fetch("forum.php?mod=viewthread&tid=" + tid);
        Document doc = Jsoup.parse(raw, HOST);
        Element subj = doc.select("#thread_subject").first();
        String title = subj == null? "": clean(subj.text());
        if (title.isEmpty()) title = "-";
        String[][] links = extractLinks(raw);
        List<String> panLinks = new ArrayList<>();
        for (String u: links[0]) if (!fid.endsWith("::baidu") ||!u.contains("baidu.com")) panLinks.add(u);
        List<String> edLinks = List.of(links[1]);
        String unzip = links[2][0];
        StringBuilder content = new StringBuilder(title);
        if (!panLinks.isEmpty()) content.append("\n\n【点击展开网盘链接（长按以复制）】:\n").append(String.join("\n", panLinks));
        if (!edLinks.isEmpty()) content.append("\n\n【磁力/ED2K】:\n").append(String.join("\n", edLinks));
        if (!unzip.isEmpty()) content.append("\n\n【解压密码】: ").append(unzip);
        List<String> plays = new ArrayList<>();
        for (int i = 0; i < edLinks.size(); i++) plays.add(edLinks.size() > 1? "Play " + (i + 1) + "$" + edLinks.get(i): "Play$" + edLinks.get(i));
        return Vod.create().id(fid).name(title).pic("").remarks("")
        .playFrom(edLinks.isEmpty()? "网盘": "磁力ED2K")
        .playUrl(plays.isEmpty()? "Play$#": String.join("#", plays))
        .content(content.toString());
    }

    @Override
    public Object[] proxy(Map<String, String> params) throws Exception {
        String u = ub64(params.getOrDefault("url", ""));
        JsonObject opts = new JsonObject();
        opts.addProperty("timeout", 20000);
        JsonObject headers = new JsonObject();
        headers.addProperty("User-Agent", SITE_UA);
        headers.addProperty("Referer", HOST);
        headers.addProperty("Cookie", siteCookie());
        opts.add("headers", headers);
        CompletableFuture<Net.StreamResult> pending = net.requestStreamAsync(proxify(u), NetOptions.from(opts.toString()));
        Net.StreamResult res;
        try {
            res = pending.get();
        } catch (InterruptedException e) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        }
        Map<String, String> outHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e: res.headers.entrySet()) {
            Object v = e.getValue();
            outHeaders.put(e.getKey(), v instanceof List<?> list? String.join(", ", list.stream().map(Object::toString).toList()): String.valueOf(v));
        }
        return new Object[]{res.code, res.contentType, res, outHeaders};
    }

    private List<Vod> gallery(String tid) throws Exception {
        List<String> imgs = images(fetch("forum.php?mod=viewthread&tid=" + tid));
        if (imgs.isEmpty()) return List.of(listCard("err", "未提取到图片", ""));
        StringBuilder tags = new StringBuilder();
        for (String u: imgs) tags.append("<div class=\"item\"><img src=\"").append(escapeAttr(imageSrc(u))).append("\" onclick=\"toggleZoom(this)\" onerror=\"retryImage(this)\" data-retry=\"0\"></div>");
        popup(IMG_TEMPLATE.replace("###IMG_TAGS###", tags.toString()));
        return List.of(Vod.create().id("gallery::" + tid).name(imgs.size() + " 张").pic(imageSrc(imgs.get(0))).remarks("").style(Style.create().type("rect").ratio(1.33f)));
    }

    private List<Vod> novel(String tid) throws Exception {
        String raw = fetch("forum.php?mod=viewthread&tid=" + tid);
        Document doc = Jsoup.parse(raw, HOST);
        Element subj = doc.select("#thread_subject").first();
        String title = subj == null? "": clean(subj.text());
        if (title.isEmpty()) title = "-";
        Element author = doc.select("a[href*=authorid=]").first();
        if (author!= null) {
            Matcher am = Pattern.compile("authorid=(\\d+)").matcher(author.attr("href"));
            if (am.find()) {
                raw = fetch("forum.php?mod=viewthread&tid=" + tid + "&authorid=" + am.group(1));
                doc = Jsoup.parse(raw, HOST);
            }
        }
        List<String> parts = new ArrayList<>();
        for (Element post: doc.select("td.t_f[id^=postmessage_]")) {
            post.select("span[style*=display:none], font[size=0], font.jammer, .jammer, .quote, .pstatus, script, style, ignore_js_op").remove();
            String txt = novelPart(post.html());
            String plain = txt.replaceAll("<[^>]+>", "").trim();
            if (!plain.isEmpty() && plain.length() >= 30) {
                parts.add((parts.isEmpty()? "<div class=\"chapter-title\">" + escapeHtml(title) + "</div><div class=\"chapter-meta\">ID: " + tid + "</div>": "") + txt);
            }
        }
        String data = new JSONArray(parts.isEmpty() ? List.of("<p class=\"paragraph\">暂无内容</p>") : parts).toString();
        popup(HTML_TEMPLATE.replace("###BOOK_DATA###", data));
        return List.of(Vod.create().id("read::" + tid).name("Read").pic("").remarks("").style(Style.create().type("rect")));
    }

    private String novelPart(String h) {
        String t = (h == null? "": h)
        .replaceAll("(?is)<script.*?</script>|<style.*?</style>|<div class=\"quote\">.*?</div>|<i class=\"pstatus\">.*?</i>|<ignore_js_op>.*?</ignore_js_op>|<[^>]+class=[\"'][^\"']*jammer[^\"']*[\"'][^>]*>.*?</[^>]+>", "")
        .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>", "\n");
        t = org.jsoup.parser.Parser.unescapeEntities(t.replaceAll("<[^>]+>", ""), false).replace('　', ' ').replaceAll("[ \\t\\r\\f]+", " ");
        List<String> out = new ArrayList<>();
        for (String line: t.split("\n")) {
            String x = line.trim();
            if (x.isEmpty()) continue;
            if (x.length() < 34 && Pattern.compile("^(第[0-9一二三四五六七八九十百千]+[章回节卷]|Chapter\\s*\\d+|番外|楔子|序章)", Pattern.CASE_INSENSITIVE).matcher(x).find()) out.add("<h3 class=\"chapter-title\">" + escapeHtml(x) + "</h3>");
            else out.add("<p class=\"paragraph\">" + escapeHtml(x) + "</p>");
        }
        return String.join("\n", out);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeAttr(String s) {
        return escapeHtml(s).replace("\"", "&quot;");
    }

    private void popup(String page) {
        main.post(() -> {
            if (destroyed) return;
            Activity act = getActivity();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;
            try {
                Dialog d = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                WebView w = new WebView(act);
                WebSettings ws = w.getSettings();
                ws.setJavaScriptEnabled(true);
                ws.setDomStorageEnabled(true);
                ws.setMediaPlaybackRequiresUserGesture(false);
                w.setWebViewClient(new WebViewClient());
                w.setBackgroundColor(Color.BLACK);
                w.loadDataWithBaseURL(HOST, page, "text/html", "utf-8", null);
                d.setContentView(w);
                d.show();
                Window win = d.getWindow();
                if (win!= null) {
                    win.getDecorView().setPadding(0, 0, 0, 0);
                    win.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                    win.getDecorView().setSystemUiVisibility(5894);
                    WindowManager.LayoutParams lp = win.getAttributes();
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                    lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                    win.setAttributes(lp);
                    win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                }
            } catch (Exception ignored) {
            }
        });
    }
    private List<Vod> configCards() {
        String cookie = bdCookie();
        String user = localStr("baidu_user");
        String bdStatus = !cookie.contains("BDUSS=") ? "未登录（点此选择登录方式）" : user.isEmpty() ? "已保存 Cookie（点此管理）" : "已登录：" + user;
        List<Vod> cards = new ArrayList<>();
        cards.add(Vod.create().id("action/baidu_menu").name("摆渡网盘").pic(COVER).remarks(bdStatus).action("baidu_menu").style(Style.create().type("list").ratio(1.1f)));
        cards.add(Vod.create().id("action/site_cookie").name("搜同 Cookie").pic(COVER).remarks("内置默认可用，失效时点此更新").action("site_cookie").style(Style.create().type("list").ratio(1.1f)));
        cards.add(Vod.create().id("action/baidu_clear").name("清除百度 Cookie").pic(COVER).remarks("删除本地保存的 BDUSS").action("baidu_clear").style(Style.create().type("list").ratio(1.1f)));
        cards.add(Vod.create().id("action/baidu_help").name("使用说明").pic(COVER).remarks("百度帖子封面 → 分享文件列表 → 点文件下载").action("baidu_help").style(Style.create().type("list").ratio(1.1f)));
        return cards;
    }

    private String localStr(String key) {
        String v = local.get(key);
        if (v == null || v.isEmpty()) return "";
        try {
            return new JSONObject(v).optString("v");
        } catch (Exception e) {
            return "";
        }
    }

    private void localSet(String key, String value) {
        try {
            local.set(key, new JSONObject().put("v", value).toString());
        } catch (Exception ignored) {
        }
    }

    private String bdCookie() {
        if (!bdCookieMem.isEmpty()) return bdCookieMem;
        String v = localStr("baidu_cookie");
        if (!v.isEmpty()) {
            bdCookieMem = v;
            return v;
        }
        return extBaiduCookie;
    }

    private String bdCk(String cookie, String sekey) {
        List<String> parts = new ArrayList<>();
        for (String p : String.valueOf(cookie == null ? "" : cookie).split(";")) {
            String t = p.trim();
            if (!t.isEmpty() && !t.startsWith("BDCLND=") && !t.startsWith("PANPSC=")) parts.add(t);
        }
        if (sekey != null && !sekey.isEmpty()) parts.add("BDCLND=" + sekey);
        return String.join("; ", parts);
    }

    private void bdSave(String cookie) {
        bdCookieMem = bdCk(cookie, "");
        localSet("baidu_cookie", bdCookieMem);
    }

    private void bdClear() {
        bdCookieMem = "";
        local.delete("baidu_cookie");
        local.delete("baidu_user");
    }

    private JSONObject bdOpts(String cookie, String ua, JSONObject extra) throws Exception {
        JSONObject h = new JSONObject().put("Cookie", cookie).put("User-Agent", ua);
        if (extra != null) for (String k : new String[]{"Referer", "Origin", "Content-Type"}) if (extra.has(k)) h.put(k, extra.getString(k));
        return new JSONObject().put("headers", h);
    }

    private JSONObject bdGetJ(String url, String cookie, String ua, JSONObject extra) throws Exception {
        return new JSONObject(net.json(url, bdOpts(cookie, ua, extra).toString()));
    }

    private JSONObject bdPostJ(String url, JSONObject data, String body, String cookie, String ua, JSONObject extra) throws Exception {
        JSONObject opts = bdOpts(cookie, ua, extra);
        opts.put("method", "POST");
        if (data != null) {
            opts.put("data", data);
            opts.put("postType", "form");
        }
        if (body != null) opts.put("body", body);
        return new JSONObject(net.json(url, opts.toString()));
    }

    private JSONObject bdTplvar(String field, String cookie) throws Exception {
        return bdGetJ("https://pan.baidu.com/api/gettemplatevariable?clienttype=0&app_id=250528&web=1&fields=%5B%22" + field + "%22%5D", cookie, BD_UA_WEB, null);
    }

    private String bdUsername(String cookie) {
        try {
            JSONObject j = bdTplvar("username", cookie);
            if ((!j.has("errno") || j.optInt("errno") == 0) && j.has("result")) {
                String u = j.getJSONObject("result").optString("username");
                return u == null ? "" : u;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String bdBdstoken(String cookie) throws Exception {
        JSONObject j = bdTplvar("bdstoken", cookie);
        if (j.has("errno") && j.getInt("errno") != 0) {
            if (j.getInt("errno") == -6) throw new Exception("获取 bdstoken 失败 errno=-6：这个 Cookie 不是网页版登录态（多为从 App 抓的），网页登录态验证不通过；请在电脑/手机浏览器登录 pan.baidu.com 后复制 Cookie");
            throw new Exception("获取 bdstoken 失败 errno=" + j.getInt("errno"));
        }
        return j.getJSONObject("result").getString("bdstoken");
    }

    private String[] bdParse(String u) {
        Matcher m = Pattern.compile("[?&]surl=([A-Za-z0-9_-]+)").matcher(u);
        String surl;
        if (m.find()) {
            surl = m.group(1);
        } else {
            m = Pattern.compile("/s/([A-Za-z0-9_-]+)").matcher(u);
            m.find();
            surl = m.group(1);
            if (surl.startsWith("1")) surl = surl.substring(1);
        }
        Matcher pm = Pattern.compile("[?&]pwd=([A-Za-z0-9]+)").matcher(u);
        return new String[]{surl, pm.find() ? pm.group(1) : ""};
    }

    private String bdVerify(String surl, String pwd, String cookie) throws Exception {
        if (pwd == null || pwd.isEmpty()) return "";
        JSONObject data = new JSONObject().put("pwd", pwd).put("vcode_str", "").put("vcode", "");
        JSONObject extra = new JSONObject().put("Referer", "https://pan.baidu.com/s/" + surl);
        JSONObject j = bdPostJ("https://pan.baidu.com/share/verify?surl=" + surl, data, null, cookie, BD_UA_WEB, extra);
        if (j.has("errno") && j.getInt("errno") != 0) throw new Exception("验证提取码失败 errno=" + j.getInt("errno"));
        return j.getString("randsk");
    }

    private JSONObject bdList(String surl, String sekey, String dir, String cookie, int page) throws Exception {
        String root = dir == null || dir.isEmpty() || dir.equals("/") ? "1" : "0";
        String url = "https://pan.baidu.com/rest/2.0/xpan/share?method=list&shorturl=" + surl + "&page=" + page + "&num=100&root=" + root + "&dir=" + java.net.URLEncoder.encode(dir == null || dir.isEmpty() ? "/" : dir, StandardCharsets.UTF_8);
        if (sekey != null && !sekey.isEmpty()) url += "&sekey=" + sekey;
        JSONObject extra = new JSONObject().put("Referer", "https://pan.baidu.com/s/" + surl);
        JSONObject j = bdGetJ(url, bdCk(cookie, sekey), BD_UA_WEB, extra);
        if (j.has("errno") && j.getInt("errno") != 0) throw new Exception("获取分享文件列表失败 errno=" + j.getInt("errno"));
        return j;
    }

    private String bdSize(double n) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        while (n >= 1024 && i < units.length - 1) {
            n /= 1024;
            i++;
        }
        return i == 0 ? String.format(Locale.ROOT, "%dB", (long) n) : String.format(Locale.ROOT, "%.2f%s", n, units[i]);
    }

    private String baiduShare(String token, int pg) {
        try {
            JSONObject info = new JSONObject(ub64(token));
            String cookie = bdCookie();
            String sekey = bdVerify(info.getString("surl"), info.optString("pwd"), cookie);
            JSONObject j = bdList(info.getString("surl"), sekey, info.optString("dir"), cookie, pg);
            JSONArray files = j.has("list") && !j.isNull("list") ? j.getJSONArray("list") : new JSONArray();
            List<Vod> cards = new ArrayList<>();
            for (int i = 0; i < files.length(); i++) {
                JSONObject it = files.getJSONObject(i);
                String name = it.getString("server_filename");
                if (it.getString("isdir").equals("1")) {
                    String tok = b64(new JSONObject().put("surl", info.getString("surl")).put("pwd", info.optString("pwd")).put("dir", it.getString("path")).toString());
                    cards.add(Vod.create().id("baidupan::" + tok).name(name).pic(COVER).remarks("文件夹").tag("folder").style(Style.create().type("list").ratio(1.1f)));
                } else {
                    String tok = b64(new JSONObject().put("surl", info.getString("surl")).put("pwd", info.optString("pwd")).put("share_id", j.getString("share_id")).put("uk", j.getString("uk")).put("sekey", sekey).put("fs_id", it.getString("fs_id")).put("name", name).toString());
                    cards.add(Vod.create().id("action/bdl/" + tok).name(name).pic(COVER).remarks(bdSize(it.getDouble("size"))).action("bdl/" + tok).style(Style.create().type("list").ratio(1.1f)));
                }
            }
            return page(cards, pg, files.length() == 100 ? pg + 1 : pg, 999999);
        } catch (Exception e) {
            return page(List.of(Vod.create().id("err").name("百度分享解析失败").pic(COVER).remarks(String.valueOf(e.getMessage())).style(Style.create().type("list").ratio(1.1f))));
        }
    }

    private String bdShareDownload(JSONObject info, String cookie) throws Exception {
        String surl = info.getString("surl");
        String page = net.get("https://pan.baidu.com/s/1" + surl, Map.of("Cookie", bdCk(cookie, info.optString("sekey")), "User-Agent", BD_UA_WEB, "Referer", "https://pan.baidu.com/"));
        Matcher sm = Pattern.compile("\"sign\":\"([^\"]+)\"").matcher(page);
        Matcher tm = Pattern.compile("\"timestamp\":(\\d+)").matcher(page);
        if (!sm.find() || !tm.find()) throw new Exception("分享页未取到 sign/timestamp");
        String url = "https://pan.baidu.com/api/sharedownload?app_id=250528&channel=chunlei&clienttype=12&web=1&sign=" + sm.group(1) + "&timestamp=" + tm.group(1);
        JSONObject data = new JSONObject().put("encrypt", "0").put("product", "share").put("uk", info.getString("uk")).put("primaryid", info.getString("share_id")).put("fid_list", "[" + info.getString("fs_id") + "]").put("path_list", "[]");
        JSONObject extra = new JSONObject().put("Referer", "https://pan.baidu.com/s/1" + surl);
        JSONObject j = bdPostJ(url, data, null, bdCk(cookie, info.optString("sekey")), BD_UA_WEB, extra);
        if (j.has("errno") && j.getInt("errno") != 0) throw new Exception("sharedownload errno=" + j.getInt("errno"));
        String dlink = j.getJSONArray("list").getJSONObject(0).optString("dlink");
        if (dlink.isEmpty()) throw new Exception("sharedownload 未返回 dlink");
        return dlink;
    }

    private String askCookie() throws Exception {
        JSONObject opts = new JSONObject()
        .put("title", "摆渡网盘")
        .put("message", "粘贴完整 Cookie，需包含 BDUSS；保存后用于获取直链与下载。")
        .put("cancelable", true)
        .put("inputs", new JSONArray().put(new JSONObject().put("name", "cookie").put("label", "Cookie").put("type", "textarea").put("required", true).put("helperText", "适合已有 BDUSS")))
        .put("buttons", new JSONArray().put(new JSONObject().put("id", "save").put("label", "保存")));
        JSONObject r = dialog(opts);
        if (r == null) return bdCookie();
        String v = r.getJSONObject("values").optString("cookie").trim();
        if (!v.isEmpty() && !v.contains("BDUSS=") && !v.contains("=")) v = "BDUSS=" + v;
        if (!v.contains("BDUSS=")) return bdCookie();
        bdSave(v);
        return v;
    }

    private String baiduMenu() throws Exception {
        String cookie = bdCookie();
        String user = localStr("baidu_user");
        String status = !cookie.contains("BDUSS=") ? "未登录" : user.isEmpty() ? "已保存 Cookie" : "已登录：" + user;
        JSONObject opts = new JSONObject()
        .put("title", "摆渡网盘")
        .put("message", "请选择摆渡登录方式（当前状态：" + status + "）")
        .put("cancelable", true)
        .put("inputs", new JSONArray().put(new JSONObject().put("name", "way").put("label", "登录方式").put("type", "select").put("value", "manual")
        .put("options", new JSONArray()
        .put(new JSONObject().put("value", "app").put("label", "App 扫码（推荐）"))
        .put(new JSONObject().put("value", "manual").put("label", "手动输入 Cookie"))
        .put(new JSONObject().put("value", "copy").put("label", "复制当前 Cookie"))
        .put(new JSONObject().put("value", "clear").put("label", "清除 Cookie")))))
        .put("buttons", new JSONArray()
        .put(new JSONObject().put("id", "next").put("label", "下一步"))
        .put(new JSONObject().put("id", "close").put("label", "关闭")));
        JSONObject r = dialog(opts);
        if (r == null || !r.optString("button").equals("next")) return "{}";
        String way = r.getJSONObject("values").optString("way");
        if (way.equals("manual")) {
            String before = bdCookie();
            String v = askCookie();
            if (!v.contains("BDUSS=")) return Response.notify("未保存 Cookie");
            if (v.equals(before)) return Response.notify("已保存百度 Cookie");
            String name = bdUsername(v);
            if (name.isEmpty()) return Response.notify("Cookie 已保存，但百度网页接口验证失败：这个多半是 App 抓的 Cookie，网页登录态验证不通过；请改用浏览器登录 pan.baidu.com 后复制的 Cookie");
            try {
                bdBdstoken(v);
            } catch (Exception e) {
                return Response.notify("Cookie 已保存，但取 bdstoken 失败：这个多半是 App 抓的 Cookie，网页登录态验证不通过；请改用浏览器登录 pan.baidu.com 后复制的 Cookie");
            }
            localSet("baidu_user", name);
            return Response.notify("百度已登录：" + name);
        }
        if (way.equals("copy")) {
            if (!cookie.contains("BDUSS=")) return Response.notify("尚未保存 Cookie");
            ClipboardManager manager = (ClipboardManager) Runtime.context().getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("baidu_cookie", cookie));
            Runtime.toast("已复制当前 Cookie");
            return "{}";
        }
        if (way.equals("clear")) {
            bdClear();
            return Response.notify("已清除百度 Cookie");
        }
        if (way.equals("app")) return qrLogin();
        return "{}";
    }

    private String qrLogin() throws Exception {
        JSONObject ua = new JSONObject().put("headers", new JSONObject().put("User-Agent", BD_UA_WEB));
        JSONObject q = new JSONObject(net.json("https://passport.baidu.com/v2/api/getqrcode?lp=pc&qrloginfrom=pc&apiver=v3", ua.toString()));
        String sign = q.optString("sign");
        String imgurl = q.optString("imgurl");
        if (!imgurl.startsWith("http")) imgurl = "https://" + imgurl;
        if (sign.isEmpty() || imgurl.isEmpty()) return Response.notify("扫码初始化失败，请改用手动输入 Cookie");
        JSONObject ropts = new JSONObject().put("buffer", 1).put("headers", new JSONObject().put("User-Agent", BD_UA_WEB));
        JSONObject r = new JSONObject(net.req(imgurl, ropts.toString()));
        JSONArray content = r.getJSONArray("content");
        byte[] bytes = new byte[content.length()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) content.getInt(i);
        String image = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        boolean[] saved = {false};
        JSONObject dopts = new JSONObject()
        .put("title", "摆渡网盘")
        .put("message", "请用百度网盘 App 扫一扫，扫码确认后点「我已扫码」")
        .put("image", image)
        .put("cancelable", true)
        .put("buttons", new JSONArray()
        .put(new JSONObject().put("id", "done").put("label", "我已扫码").put("close", false))
        .put(new JSONObject().put("id", "close").put("label", "关闭")));
        dialog(dopts, event -> {
            if (!event.getString("button").equals("done")) return null;
            JSONObject u;
            try {
                JSONObject pollOpts = new JSONObject().put("timeout", 25000).put("headers", new JSONObject().put("User-Agent", BD_UA_WEB));
                u = new JSONObject(net.json("https://passport.baidu.com/channel/unicast?channel_id=" + sign + "&tpl=mn&_sdkFrom=1", pollOpts.toString()));
            } catch (Exception e) {
                return new JSONObject().put("message", "等待扫码…（确认后再次点「我已扫码」）");
            }
            String cv = u.optString("channel_v");
            if (cv.isEmpty()) return new JSONObject().put("message", "等待扫码…");
            JSONObject c = new JSONObject(cv);
            int st = c.optInt("status");
            if (st == 0) return new JSONObject().put("message", "等待扫码…");
            if (st == 1) return new JSONObject().put("message", "已扫码，请在手机上确认登录");
            String cookie = qrExchange(c.optString("v"));
            if (cookie.isEmpty()) return new JSONObject().put("message", "换取登录态失败，请重试或改用手动输入");
            bdSave(cookie);
            String name = bdUsername(cookie);
            if (name.isEmpty()) return new JSONObject().put("message", "已拿到 Cookie 但验证未通过，请改用手动输入");
            localSet("baidu_user", name);
            saved[0] = true;
            return new JSONObject().put("close", true);
        });
        return saved[0] ? Response.notify("百度已登录：" + localStr("baidu_user")) : "{}";
    }

    private String qrExchange(String v) {
        try {
            JSONObject ropts = new JSONObject().put("redirect", 0).put("headers", new JSONObject().put("User-Agent", BD_UA_WEB));
            JSONObject r = new JSONObject(net.req("https://passport.baidu.com/v3/login/main/qrbdusslogin?v=" + java.net.URLEncoder.encode(v, StandardCharsets.UTF_8), ropts.toString()));
            Map<String, String> found = new HashMap<>();
            if (r.has("headers")) {
                JSONObject h = r.getJSONObject("headers");
                for (String key : new String[]{"set-cookie", "Set-Cookie"}) {
                    if (!h.has(key)) continue;
                    Object val = h.get(key);
                    List<String> lines = new ArrayList<>();
                    if (val instanceof JSONArray arr) for (int i = 0; i < arr.length(); i++) lines.add(arr.getString(i));
                    else lines.add(String.valueOf(val));
                    for (String line : lines) for (String part : line.split(";")) {
                        String t = part.trim();
                        int eq = t.indexOf('=');
                        if (eq > 0) found.put(t.substring(0, eq), t.substring(eq + 1));
                    }
                }
            }
            String body = r.optString("content");
            if (body.startsWith("{")) {
                JSONObject cj = new JSONObject(body);
                if (cj.has("bduss")) found.put("BDUSS", cj.getString("bduss"));
                if (cj.has("ptoken")) found.put("PTOKEN", cj.getString("ptoken"));
                if (cj.has("stoken")) found.put("STOKEN", cj.getString("stoken"));
            }
            if (!found.containsKey("BDUSS")) return "";
            StringBuilder sb = new StringBuilder();
            for (String k : new String[]{"BDUSS", "PTOKEN", "STOKEN", "BAIDUID"}) if (found.containsKey(k)) sb.append(k).append('=').append(found.get(k)).append("; ");
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String siteCookieDialog() throws Exception {
        JSONObject opts = new JSONObject()
        .put("title", "搜同 Cookie")
        .put("message", "粘贴浏览器登录 stboy.net 后的完整 Cookie；清空保存则恢复内置默认。")
        .put("cancelable", true)
        .put("inputs", new JSONArray().put(new JSONObject().put("name", "cookie").put("label", "Cookie").put("type", "textarea").put("value", siteCookie())))
        .put("buttons", new JSONArray().put(new JSONObject().put("id", "save").put("label", "保存")));
        JSONObject r = dialog(opts);
        if (r == null) return "{}";
        String v = r.getJSONObject("values").optString("cookie").trim();
        if (v.isEmpty()) {
            local.delete("site_cookie");
            return Response.notify("已恢复内置默认 Cookie");
        }
        localSet("site_cookie", v);
        return Response.notify("已保存搜同 Cookie");
    }

    private String baiduDownload(String token) {
        Runtime.toast("请稍等，正在获取下载链接...");
        try {
            JSONObject info = new JSONObject(ub64(token));
            String cookie = bdCookie();
            if (!cookie.contains("BDUSS=")) cookie = askCookie();
            if (!cookie.contains("BDUSS=")) return Response.notify("未设置百度 Cookie（BDUSS），无法下载");
            String name = info.optString("name");
            String url = bdShareDownload(info, cookie);
            main.post(() -> showDownload(url, name, "pan.baidu.com", null));
            return "{}";
        } catch (Exception e) {
            return Response.notify("百度下载失败：" + e.getMessage());
        }
    }

    private void showDownload(String url, String name, String ua, Runnable onSuccess) {
        if (destroyed) return;
        if (downloadDialog != null) {
            Runtime.toast("请先完成或取消当前下载");
            return;
        }
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Runtime.toast("当前无可用画面");
            return;
        }
        try {
            downloadDialog = new DownloadDialog(activity, url, name, ua, onSuccess);
            downloadDialog.show();
        } catch (Exception e) {
            if (downloadDialog != null) downloadDialog.close();
            Runtime.toast(String.valueOf(e.getMessage()));
        }
    }

    private final class DownloadDialog implements Download.Callback, View.OnAttachStateChangeListener {

        private final String name;
        private final File folder;
        private final File target;
        private final File temporary;
        private final Download download;
        private final AlertDialog window;
        private final View anchor;
        private final TextView status;
        private final TextView details;
        private final ProgressBar progress;
        private final Runnable onSuccess;
        private Future<?> verification;
        private String transferred = "等待服务器响应";
        private boolean paused;
        private boolean finishing;
        private volatile boolean closed;

        private DownloadDialog(Activity activity, String url, String name, String ua, Runnable onSuccess) throws IOException {
            this.name = name.replace("/", "_");
            this.onSuccess = onSuccess;
            folder = prepareFolder();
            target = new File(folder, this.name);
            String suffix = this.name.contains(".") ? this.name.substring(this.name.lastIndexOf('.')) : ".tmp";
            if (suffix.length() > 8) suffix = ".tmp";
            temporary = File.createTempFile(".stboy-", suffix, folder);
            Request request = new Request.Builder().url(url).header("User-Agent", ua).build();
            download = new Download(request, temporary).threads(4);
            anchor = activity.getWindow().getDecorView();
            status = new TextView(activity);
            progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            details = new TextView(activity);
            window = new AlertDialog.Builder(activity).setTitle(this.name).setView(createContent(activity)).setPositiveButton("暂停", null).setNegativeButton("取消", null).create();
        }

        private File prepareFolder() throws IOException {
            File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("无法创建下载目录");
            return folder;
        }

        private View createContent(Activity activity) {
            LinearLayout content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
            content.setPadding(padding, padding / 3, padding, 0);
            status.setText("正在连接…");
            status.setTextSize(14);
            status.setPadding(0, padding / 2, 0, padding / 2);
            content.addView(status);
            progress.setMax(100);
            progress.setIndeterminate(true);
            content.addView(progress, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            details.setText("等待服务器响应");
            details.setTextSize(14);
            details.setPadding(0, padding / 3, 0, padding / 3);
            content.addView(details);
            return content;
        }

        private void show() {
            window.setCanceledOnTouchOutside(false);
            window.setOnDismissListener(ignored -> close());
            window.show();
            window.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignored -> togglePause());
            window.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(ignored -> close());
            anchor.addOnAttachStateChangeListener(this);
            download.start(this);
        }

        private void togglePause() {
            if (closed || finishing) return;
            if (paused) resume();
            else pause();
        }

        private void resume() {
            paused = false;
            status.setText("正在继续下载…");
            window.getButton(AlertDialog.BUTTON_POSITIVE).setText("暂停");
            download.start(this);
        }

        private void pause() {
            status.setText("正在暂停…");
            window.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            download.pause();
        }

        @Override
        public void progress(long total, long downloaded, long speed) {
            if (closed || finishing) return;
            boolean known = total > 0;
            int percent = known ? (int) Math.min(100, downloaded * 100.0 / total) : 0;
            progress.setIndeterminate(!known);
            if (known) progress.setProgress(percent);
            status.setText(known ? name + " 下载中 · " + percent + "%" : name + " 下载中");
            transferred = size(downloaded) + (known ? " / " + size(total) : "");
            details.setText(transferred + " · " + size(speed) + "/s");
        }

        private String size(long bytes) {
            return Formatter.formatShortFileSize(Runtime.context(), Math.max(0, bytes));
        }

        @Override
        public void paused() {
            if (closed) return;
            paused = true;
            progress.setIndeterminate(false);
            status.setText("已暂停");
            details.setText(transferred);
            window.getButton(AlertDialog.BUTTON_POSITIVE).setText("继续");
            window.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }

        @Override
        public void success(File file) {
            if (closed) return;
            finishing = true;
            status.setText("正在保存文件…");
            progress.setIndeterminate(true);
            window.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            verification = Task.submit(() -> finishDownload(file));
        }

        private void finishDownload(File file) {
            try {
                if (file.length() == 0) throw new IOException("下载文件为空");
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("下载已取消");
                Path.move(file, target);
                if (onSuccess != null) onSuccess.run();
                main.post(() -> complete());
            } catch (Exception e) {
                main.post(() -> error(String.valueOf(e.getMessage())));
            }
        }

        private void complete() {
            if (closed) return;
            String n = name;
            close();
            Runtime.toast("下载完成：" + n);
        }

        @Override
        public void error(String msg) {
            if (closed) return;
            finishing = true;
            status.setText("下载失败");
            details.setText(msg == null || msg.isEmpty() ? "无法完成下载" : msg);
            progress.setIndeterminate(false);
            window.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            window.getButton(AlertDialog.BUTTON_NEGATIVE).setText("关闭");
            temporary.delete();
        }

        private void close() {
            if (closed) return;
            closed = true;
            download.cancel();
            if (verification != null) verification.cancel(true);
            anchor.removeOnAttachStateChangeListener(this);
            window.dismiss();
            temporary.delete();
            if (downloadDialog == this) downloadDialog = null;
        }

        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            close();
        }
    }
}
