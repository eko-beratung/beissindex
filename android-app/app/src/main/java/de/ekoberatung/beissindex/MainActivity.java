package de.ekoberatung.beissindex;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String HOME = "https://eko-beratung.github.io/beissindex/";
    private static final int IMPORT = 101, EXPORT = 102, LOCATION = 103;
    private WebView browser;
    private android.webkit.ValueCallback<Uri[]> fileCallback;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private byte[] exportBytes;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(18,52,59));
        browser = new WebView(this);
        root.addView(browser,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        WebSettings s=browser.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        browser.addJavascriptInterface(new Downloads(),"AndroidDownloads");
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(!request.isForMainFrame())return false;
                Uri uri=request.getUrl();
                if(isSite(uri))return false;
                if("https".equalsIgnoreCase(uri.getScheme()) || "mailto".equalsIgnoreCase(uri.getScheme())) {
                    try {startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){toast("Link konnte nicht geöffnet werden");}
                }
                return true;
            }
            @Override public void onPageFinished(WebView view,String url) {
                if(isSite(Uri.parse(url))) view.evaluateJavascript(
                    "(function(){if(window.__beissAndroidExport)return;window.__beissAndroidExport=true;" +
                    "var create=URL.createObjectURL.bind(URL),revoke=URL.revokeObjectURL.bind(URL),items=new Map();" +
                    "URL.createObjectURL=function(blob){var url=create(blob);items.set(url,blob);return url;};" +
                    "URL.revokeObjectURL=function(url){items.delete(url);return revoke(url);};" +
                    "var click=HTMLAnchorElement.prototype.click;" +
                    "HTMLAnchorElement.prototype.click=function(){var blob=items.get(this.href);" +
                    "if(this.download&&blob){var name=this.download;blob.text().then(function(t){AndroidDownloads.saveFile(name,t,blob.type);});return;}" +
                    "return click.call(this);};})();",null);
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback) {
                Uri originUri = Uri.parse(origin);
                if(!"https".equalsIgnoreCase(originUri.getScheme()) ||
                   !"eko-beratung.github.io".equalsIgnoreCase(originUri.getHost()) ||
                   browser.getUrl()==null || !isSite(Uri.parse(browser.getUrl()))) {
                    callback.invoke(origin,false,false);return;
                }
                if(hasLocation()){callback.invoke(origin,true,true);}
                else {
                    locationOrigin=origin;locationCallback=callback;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION);
                }
            }
            @Override public boolean onShowFileChooser(WebView view,android.webkit.ValueCallback<Uri[]> cb,FileChooserParams params) {
                if(fileCallback!=null)fileCallback.onReceiveValue(null);
                fileCallback=cb;
                try{startActivityForResult(params.createIntent(),IMPORT);return true;}
                catch(Exception e){fileCallback=null;toast("Keine Datei-App gefunden");return false;}
            }
        });
        browser.setDownloadListener((url,ua,disposition,mime,len)->{
            Uri u=Uri.parse(url);
            if(!"https".equalsIgnoreCase(u.getScheme())){toast("Download nicht unterstützt");return;}
            try {
                DownloadManager.Request req=new DownloadManager.Request(u);
                req.setMimeType(mime);req.setTitle("Beißindex Download");
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"beissindex-download");
                ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
                toast("Download gestartet");
            } catch(Exception e){toast("Download fehlgeschlagen");}
        });
        if(state==null)browser.loadUrl(HOME);else browser.restoreState(state);
    }
    private boolean isSite(Uri u){
        if(u==null||!"https".equalsIgnoreCase(u.getScheme())||!"eko-beratung.github.io".equalsIgnoreCase(u.getHost()))return false;
        String path=u.getPath();
        return path!=null&&(path.equals("/beissindex")||path.startsWith("/beissindex/"));
    }
    private boolean hasLocation(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            ||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==LOCATION&&locationCallback!=null){
            locationCallback.invoke(locationOrigin,hasLocation(),true);
            locationCallback=null;locationOrigin=null;
        }
    }
    private final class Downloads {
        @JavascriptInterface public void saveFile(String name,String content,String type){
            runOnUiThread(()->{
                if(exportBytes!=null){toast("Export läuft bereits");return;}
                exportBytes=content.getBytes(StandardCharsets.UTF_8);
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                String mime=type.split(";")[0];
                i.setType(mime.isEmpty()?"application/octet-stream":mime);
                i.putExtra(Intent.EXTRA_TITLE,name.replace('/','_').replace('\\','_'));
                try{startActivityForResult(i,EXPORT);}catch(Exception e){exportBytes=null;toast("Export nicht möglich");}
            });
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==IMPORT&&fileCallback!=null){
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));
            fileCallback=null;
        }
        if(request==EXPORT){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null&&exportBytes!=null){
                try(OutputStream stream=getContentResolver().openOutputStream(data.getData(),"w")){
                    if(stream==null)throw new IOException("Kein Dateizugriff");
                    stream.write(exportBytes);toast("Datei gespeichert");
                }catch(IOException e){toast("Speichern fehlgeschlagen");}
            }
            exportBytes=null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){if(browser!=null)browser.saveState(state);super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(browser!=null)browser.destroy();super.onDestroy();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
