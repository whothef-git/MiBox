package tv.widgetv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;

/** Cached near-side sphere from NASA LROC albedo and LOLA elevation, not a painted Moon disc. */
final class LunarSurface {
    static final int SIZE=384;
    private final int[] albedo=new int[SIZE*SIZE],pixels=new int[SIZE*SIZE];
    private final float[] nx=new float[SIZE*SIZE],nz=new float[SIZE*SIZE],viewZ=new float[SIZE*SIZE];
    private final byte[] tone=new byte[256*256];
    private final Bitmap disc=Bitmap.createBitmap(SIZE,SIZE,Bitmap.Config.ARGB_8888);
    private float last=-1;
    private boolean lastWaxing,available;

    LunarSurface(Context context){
        for(int channel=0;channel<256;channel++){
            float s=channel/255f,linear=s<=.04045f?s/12.92f:(float)Math.pow((s+.055f)/1.055f,2.4);
            for(int e=0;e<256;e++){float value=Math.min(1,linear*e*2.6f/255);tone[(channel<<8)|e]=(byte)Math.round(255*(value<=.0031308f?12.92f*value:1.055f*(float)Math.pow(value,1/2.4)-.055f));}
        }
        Bitmap map=null;
        try{
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=2;
            map=BitmapFactory.decodeResource(context.getResources(),R.drawable.lunar_albedo,options);
            if(map==null)throw new java.io.IOException("Missing lunar albedo");
            int w=map.getWidth(),h=map.getHeight();int[] source=new int[w*h];map.getPixels(source,0,w,0,0,w,h);
            int ew,eh;short[] elevation;
            try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(context.getResources().openRawResource(R.raw.lunar_height))))){
                ew=in.readUnsignedShort();eh=in.readUnsignedShort();
                if(ew!=1440||eh!=720)throw new java.io.IOException("Invalid lunar terrain dimensions");
                elevation=new short[ew*eh];for(int i=0;i<elevation.length;i++)elevation[i]=in.readShort();
            }
            float radius=1737400f;
            for(int row=0;row<SIZE;row++)for(int col=0;col<SIZE;col++){
                int i=row*SIZE+col;float x=(col+.5f-SIZE/2f)/(SIZE/2f),y=(row+.5f-SIZE/2f)/(SIZE/2f);
                float r2=x*x+y*y;if(r2>=1)continue;
                float z=(float)Math.sqrt(1-r2),lon=(float)Math.atan2(x,z),lat=-(float)Math.asin(y);
                float u=.5f+lon/(float)(2*Math.PI),v=.5f-lat/(float)Math.PI;
                int c=sample(source,w,h,u,v);
                int alpha=Math.round(Math.min(1,(1-(float)Math.sqrt(r2))*SIZE/2f)*255);
                albedo[i]=(alpha<<24)|(c&0xFFFFFF);viewZ[i]=z;
                int ex=Math.min(ew-1,Math.max(0,(int)(u*ew))),ey=Math.min(eh-1,Math.max(0,(int)(v*eh)));
                float cosLat=(float)Math.cos(lat),sinLat=(float)Math.sin(lat),sinLon=(float)Math.sin(lon),cosLon=(float)Math.cos(lon);
                // Stored uint16 values are half metres; their constant datum offset cancels in differences.
                float east=(height(elevation,ew,eh,ex+1,ey)-height(elevation,ew,eh,ex-1,ey))*.5f
                        /(2*radius*Math.max(.05f,cosLat)*(float)(2*Math.PI/ew));
                float north=(height(elevation,ew,eh,ex,ey-1)-height(elevation,ew,eh,ex,ey+1))*.5f
                        /(2*radius*(float)(Math.PI/eh));
                east=Math.max(-1,Math.min(1,east));north=Math.max(-1,Math.min(1,north));
                float a=x-east*cosLon+north*sinLat*sinLon,b=y+north*cosLat,cZ=z+east*sinLon+north*sinLat*cosLon;
                float length=(float)Math.sqrt(a*a+b*b+cZ*cZ);nx[i]=a/length;nz[i]=cZ/length;
            }
            available=true;
        }catch(Exception e){Log.w(Config.TAG,"Lunar surface unavailable ("+e.getClass().getSimpleName()+")");}
        finally{if(map!=null)map.recycle();}
    }
    boolean available(){return available;}
    private static int height(short[] h,int w,int size,int x,int y){return h[Math.max(0,Math.min(size-1,y))*w+Math.floorMod(x,w)]&65535;}
    private static int sample(int[] source,int w,int h,float u,float v){
        float x=u*(w-1),y=v*(h-1);int ix=(int)x,iy=(int)y;float fx=x-ix,fy=y-iy;
        int a=source[iy*w+ix],b=source[iy*w+Math.min(w-1,ix+1)],c=source[Math.min(h-1,iy+1)*w+ix],d=source[Math.min(h-1,iy+1)*w+Math.min(w-1,ix+1)];
        int out=0;for(int shift=0;shift<=16;shift+=8){float top=((a>>shift)&255)*(1-fx)+((b>>shift)&255)*fx,bottom=((c>>shift)&255)*(1-fx)+((d>>shift)&255)*fx;out|=Math.round(top*(1-fy)+bottom*fy)<<shift;}return out;
    }
    Bitmap phase(float illuminated,boolean waxing){
        if(!available||last==illuminated&&lastWaxing==waxing)return disc;
        last=illuminated;lastWaxing=waxing;
        float lz=2*illuminated-1,lx=(waxing?1:-1)*(float)Math.sqrt(Math.max(0,1-lz*lz));
        for(int i=0;i<pixels.length;i++){
            int color=albedo[i];if((color>>>24)==0){pixels[i]=0;continue;}
            int col=i%SIZE;float sphereX=(col+.5f-SIZE/2f)/(SIZE/2f);
            float geometric=sphereX*lx+viewZ[i]*lz,mu=Math.max(0,nx[i]*lx+nz[i]*lz);
            // Lommel-Seeliger lunar reflectance avoids the artificial dark rim of a Lambert sphere.
            // Local terrain normals add crater relief; this is not a cast-shadow ray tracer.
            float reflected=geometric>0?Math.min(1.6f,2*mu/Math.max(.001f,mu+viewZ[i])):0;
            int exposure=Math.min(255,Math.round((.012f+1.6f*reflected)*255/2.6f));
            pixels[i]=(color&0xFF000000)|((tone[(((color>>16)&255)<<8)|exposure]&255)<<16)
                    |((tone[(((color>>8)&255)<<8)|exposure]&255)<<8)|(tone[((color&255)<<8)|exposure]&255);
        }
        disc.setPixels(pixels,0,SIZE,0,0,SIZE,SIZE);return disc;
    }
}
