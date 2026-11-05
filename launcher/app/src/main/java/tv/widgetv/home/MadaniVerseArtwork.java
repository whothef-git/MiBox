package tv.widgetv.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Online, pre-rendered ayah artwork. Holds only the most recently requested verse in memory. */
final class MadaniVerseArtwork {
    interface Source { byte[] get(int surah,int ayah)throws Exception; }
    private final AyahArtworkCache disk;
    private final Source source;
    MadaniVerseArtwork(android.content.Context context){this(context.getCacheDir(),MadaniVerseArtwork::downloadBytes);}
    MadaniVerseArtwork(java.io.File directory,Source source){disk=new AyahArtworkCache(directory);this.source=source;}
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private String imageKey="",attemptedKey="";
    private Bitmap image;
    private long attemptedAt;
    private boolean loading,failed,closed;
    private static String key(PrayerVerse.Entry entry){return entry.surah+":"+entry.ayah;}
    Bitmap get(PrayerVerse.Entry entry){return key(entry).equals(imageKey)?image:null;}
    boolean failed(PrayerVerse.Entry entry){return failed&&key(entry).equals(attemptedKey);}
    void maybeFetch(PrayerVerse.Entry entry,Runnable done){
        String requested=key(entry);long now=SystemClock.elapsedRealtime();
        if(closed||loading||get(entry)!=null||requested.equals(attemptedKey)&&now-attemptedAt<60_000)return;
        loading=true;failed=false;attemptedKey=requested;attemptedAt=now;
        worker.execute(()->{
            Bitmap result=null;
            try{
                byte[] cached=disk.read(requested);
                if(cached!=null)try{result=decode(cached);}catch(Exception corrupt){/* Refresh invalid cached PNG. */}
                if(result==null){byte[] bytes=source.get(entry.surah,entry.ayah);result=decode(bytes);
                    try{disk.write(requested,bytes);}catch(IOException cacheUnavailable){/* Online rendering still works. */}}
            }catch(Exception unavailable){android.util.Log.w(Config.TAG,"quran: Madani artwork unavailable ("+unavailable.getClass().getSimpleName()+")");}
            Bitmap loaded=result;
            main.post(()->{
                if(closed){if(loaded!=null)loaded.recycle();return;}
                loading=false;failed=loaded==null;
                if(loaded!=null){image=loaded;imageKey=requested;}
                done.run();
            });
        });
    }
    static String url(int surah,int ayah){
        QuranReference.globalId(surah,ayah);
        return "https://cdn.islamic.network/quran/images/high-resolution/"+surah+"_"+ayah+".png";
    }
    static Bitmap download(int surah,int ayah)throws Exception{return decode(downloadBytes(surah,ayah));}
    static byte[] downloadBytes(int surah,int ayah)throws Exception{
        HttpURLConnection conn=(HttpURLConnection)new URL(url(surah,ayah)).openConnection();
        byte[] bytes;
        try{
            conn.setConnectTimeout(8000);conn.setReadTimeout(12000);conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept","image/png");conn.setRequestProperty("User-Agent","Octahedron-TV/3.1");
            if(conn.getResponseCode()!=200)throw new IOException("Ayah artwork HTTP "+conn.getResponseCode());
            try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int count;
                while((count=in.read(buffer))!=-1){if(out.size()+count>2_097_152)throw new IOException("Ayah artwork too large");out.write(buffer,0,count);}
                bytes=out.toByteArray();
            }
        }finally{conn.disconnect();}
        return bytes;
    }
    static Bitmap decode(byte[] bytes)throws IOException{
        if(bytes.length<1||bytes.length>AyahArtworkCache.LIMIT)throw new IOException("Invalid artwork size");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<1||options.outHeight<1||options.outWidth>4096||options.outHeight>16384
                ||(long)options.outWidth*options.outHeight>6_000_000)throw new IOException("Invalid ayah artwork dimensions");
        options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inMutable=true;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(bitmap==null)throw new IOException("Invalid ayah artwork");
        normalizeInk(bitmap);return trimTransparent(bitmap);
    }
    /** The CDN's gray antialias pixels encode coverage against white; preserve it when tinting on dark sky. */
    static void normalizeInk(Bitmap bitmap){
        int w=bitmap.getWidth();int[] row=new int[w];
        for(int y=0;y<bitmap.getHeight();y++){
            bitmap.getPixels(row,0,w,0,y,w,1);
            for(int x=0;x<w;x++){
                int c=row[x],coverage=255-((((c>>16)&255)*54+((c>>8)&255)*183+(c&255)*19+128)>>8);
                row[x]=(((c>>>24)*coverage+127)/255)<<24;
            }
            bitmap.setPixels(row,0,w,0,y,w,1);
        }
    }
    /** Remove transparent page margins only; preserve every non-transparent pixel, including verse number. */
    static Bitmap trimTransparent(Bitmap source)throws IOException{
        int w=source.getWidth(),h=source.getHeight(),left=w,top=h,right=-1,bottom=-1;int[] row=new int[w];
        for(int y=0;y<h;y++){
            source.getPixels(row,0,w,0,y,w,1);
            for(int x=0;x<w;x++)if((row[x]>>>24)!=0){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=y;}
        }
        if(right<left){source.recycle();throw new IOException("Empty ayah artwork");}
        Bitmap cropped=Bitmap.createBitmap(source,left,top,right-left+1,bottom-top+1);
        if(cropped!=source)source.recycle();return cropped;
    }
    static CharSequence display(PrayerVerse.Entry entry,Bitmap artwork,int width){return display(entry,artwork,width,290);}
    static CharSequence display(PrayerVerse.Entry entry,Bitmap artwork,int width,int height){
        SpannableStringBuilder text=new SpannableStringBuilder("\uFFFC");
        text.setSpan(new ArtworkSpan(artwork,width,Math.max(1,Math.round(height*.52f))),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append("\n\n");text.setSpan(new GapSpan(Math.max(6,width/133)),2,3,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append(entry.malay).append("\n\n");int start=text.length();
        text.setSpan(new GapSpan(Math.max(5,width/133)),start-1,start,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append(entry.reference());
        text.setSpan(new RelativeSizeSpan(.75f),start,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(Lapis.MIST_400),start,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return text;
    }
    private static final class GapSpan implements android.text.style.LineHeightSpan {
        private final int height;GapSpan(int height){this.height=height;}
        @Override public void chooseHeight(CharSequence text,int start,int end,int spanStart,int vertical,Paint.FontMetricsInt fm){
            fm.top=fm.ascent=-height;fm.descent=fm.bottom=fm.leading=0;
        }
    }
    static final class ArtworkSpan extends ReplacementSpan {
        private final Bitmap bitmap;private final int maxWidth,maxHeight;
        private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final RectF destination=new RectF();
        ArtworkSpan(Bitmap bitmap,int width,int height){this.bitmap=bitmap;maxWidth=Math.max(1,width);maxHeight=height;ink.setColorFilter(new PorterDuffColorFilter(Lapis.PLASTER_100,PorterDuff.Mode.SRC_IN));}
        // Arabic uses its own area budget; Malay autosizing cannot collapse it into a small central strip.
        private float scale(Paint paint){return Math.min(1.2f,Math.min(maxWidth/(float)bitmap.getWidth(),maxHeight/(float)bitmap.getHeight()));}
        @Override public int getSize(Paint paint,CharSequence text,int start,int end,Paint.FontMetricsInt fm){
            float scale=scale(paint);int height=(int)Math.ceil(bitmap.getHeight()*scale);
            if(fm!=null){fm.top=fm.ascent=-height;fm.descent=fm.bottom=0;fm.leading=0;}
            return (int)Math.ceil(bitmap.getWidth()*scale);
        }
        @Override public void draw(Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint paint){
            float scale=scale(paint);destination.set(x,y-bitmap.getHeight()*scale,x+bitmap.getWidth()*scale,y);
            canvas.drawBitmap(bitmap,null,destination,ink);
        }
    }
    void shutdown(){closed=true;main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
