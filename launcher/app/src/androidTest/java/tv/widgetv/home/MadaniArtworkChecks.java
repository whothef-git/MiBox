package tv.widgetv.home;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.TextView;

/** Isolated artwork transport and layout checks. Never changes the launcher verse cache or device controls. */
public final class MadaniArtworkChecks extends Instrumentation {
    private int checks;
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();
        try{
            check(MadaniVerseArtwork.url(37,116).endsWith("/37_116.png"),"artwork bound to the exact surah and ayah");
            for(int[] invalid:new int[][]{{0,1},{115,1},{1,0},{2,287}}){boolean rejected=false;try{MadaniVerseArtwork.url(invalid[0],invalid[1]);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid reference rejected");}
            Bitmap padding=Bitmap.createBitmap(20,16,Bitmap.Config.ARGB_8888);padding.setPixel(5,4,Color.BLACK);padding.setPixel(12,10,0x01000000);
            Bitmap crop=MadaniVerseArtwork.trimTransparent(padding);
            check(crop.getWidth()==8&&crop.getHeight()==7,"transparent page margins cropped");
            check(crop.getPixel(0,0)==Color.BLACK&&Color.alpha(crop.getPixel(7,6))==1,"every nontransparent pixel retained, including faint marks");crop.recycle();
            boolean emptyRejected=false;try{MadaniVerseArtwork.trimTransparent(Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888));}catch(java.io.IOException expected){emptyRejected=true;}check(emptyRejected,"empty artwork rejected");
            Bitmap grayEdges=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888);grayEdges.setPixel(0,0,0xFFBFBFBF);grayEdges.setPixel(1,0,0x00FFFFFF);
            MadaniVerseArtwork.normalizeInk(grayEdges);
            check(Color.alpha(grayEdges.getPixel(0,0))==64,"source gray edge coverage preserved for cream-on-dark display");
            check(Color.alpha(grayEdges.getPixel(1,0))==0,"transparent page background stays transparent after tint preparation");grayEdges.recycle();
            org.json.JSONObject escaped=new org.json.JSONObject().put("id",1).put("surah_id",1).put("nombor_ayat",1).put("nama_surah","Fixture").put("teks_arab","بِ").put("terjemahan_melayu",String.valueOf((char)92)+(char)34+"Teks.");
            PrayerVerse.Entry cleaned=PrayerVerse.parse(new org.json.JSONObject().put("success",true).put("data",escaped).toString());
            check(cleaned.malay.equals(String.valueOf((char)34)+"Teks."),"literal quote escape removed without changing words or quotation mark");
            PrayerVerse.Entry shortVerse=PrayerVerse.parse(PrayerVerse.download(PrayerVerse.API+"ayat/3904"));
            check(shortVerse.surah==37&&shortVerse.ayah==116,"reference under review loaded from the live text API");
            Bitmap shortImage=MadaniVerseArtwork.download(37,116);
            check(shortImage.getWidth()==554&&shortImage.getHeight()==84,"real Madani artwork trims page margins while preserving the complete verse and ornament");
            PrayerVerse.Entry longVerse=PrayerVerse.parse(PrayerVerse.download(PrayerVerse.API+"ayat/289"));
            Bitmap longImage=MadaniVerseArtwork.download(longVerse.surah,longVerse.ayah);
            check(longVerse.surah==2&&longVerse.ayah==282&&longImage.getWidth()>0,"longest verse artwork loads over TV HTTPS");
            java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
            runOnMainSync(()->{try{
                TextView text=new TextView(getTargetContext());text.setLayoutParams(new android.view.ViewGroup.LayoutParams(1600,290));text.setTypeface(getTargetContext().getResources().getFont(R.font.alexandria));
                text.setAutoSizeTextTypeUniformWithConfiguration(1,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);
                text.setGravity(android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL);text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);text.setLineSpacing(0,1.12f);
                PrayerVerse.Entry[] entries={shortVerse,longVerse};Bitmap[] images={shortImage,longImage};
                for(int i=0;i<2;i++){
                    CharSequence content=MadaniVerseArtwork.display(entries[i],images[i],1600);Spanned spans=(Spanned)content;
                    ReplacementSpan[] artwork=spans.getSpans(0,content.length(),ReplacementSpan.class);
                    check(artwork.length==1&&spans.getSpanStart(artwork[0])==0&&spans.getSpanEnd(artwork[0])==1,"Arabic image replaces a single placeholder without re-shaping its glyphs");
                    check(content.toString().contains(entries[i].malay)&&content.toString().endsWith(entries[i].reference()),"Malay wording and reference preserved");
                    text.setText(content);
                    for(int pass=0;pass<3;pass++){text.measure(View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(290,View.MeasureSpec.EXACTLY));text.layout(0,0,1600,290);}
                    Layout layout=text.getLayout();
                    check(layout!=null&&layout.getHeight()<=290&&layout.getLineEnd(layout.getLineCount()-1)==content.length(),"complete short/long image and translation fit without scrolling");
                    check(artwork[0].getSize(text.getPaint(),content,0,1,new Paint.FontMetricsInt())<=1600,"artwork remains within available width");
                }
            }catch(Throwable error){failure.set(error);}});
            if(failure.get()!=null)throw new AssertionError(failure.get());
            result.putString("stream","PASS: "+checks+" Madani artwork checks, including real TV HTTPS and short/long bilingual fit. No real device controls exercised.\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL after "+checks+" Madani artwork checks: "+error+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
}
