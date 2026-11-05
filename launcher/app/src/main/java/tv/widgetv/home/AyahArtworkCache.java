package tv.widgetv.home;

import android.util.AtomicFile;
import java.io.*;
import java.util.zip.CRC32;

/** One replaceable, private cache entry. Android Clear cache safely removes it. */
final class AyahArtworkCache {
    static final int LIMIT=2_097_152;
    private final AtomicFile file;
    AyahArtworkCache(File directory){file=new AtomicFile(new File(directory,"quran-current.art"));}
    byte[] read(String reference){
        try(DataInputStream in=new DataInputStream(file.openRead())){
            if(!reference.equals(in.readUTF()))return null;
            int length=in.readInt();long expected=in.readLong();if(length<1||length>LIMIT)return null;
            byte[] bytes=new byte[length];in.readFully(bytes);if(in.read()!=-1)return null;
            CRC32 crc=new CRC32();crc.update(bytes);return crc.getValue()==expected?bytes:null;
        }catch(IOException invalid){return null;}
    }
    void write(String reference,byte[] bytes)throws IOException{
        if(bytes.length<1||bytes.length>LIMIT)throw new IOException("Artwork cache limit");
        FileOutputStream stream=null;
        try{stream=file.startWrite();DataOutputStream out=new DataOutputStream(stream);
            CRC32 crc=new CRC32();crc.update(bytes);out.writeUTF(reference);out.writeInt(bytes.length);out.writeLong(crc.getValue());out.write(bytes);out.flush();file.finishWrite(stream);
        }catch(IOException failed){if(stream!=null)file.failWrite(stream);throw failed;}
    }
}
