package ru.korshun.cobaguardidea.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;


public class ImgCryptoDecoder {

    private String imgName, imgPath, imgTempPath, enc = "";
    private OutputStream out = null;

    public ImgCryptoDecoder(String path, String tempPath) {
        this.imgPath = path;
        this.imgTempPath = tempPath;
	}

    private String getImgName() {
        return imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    private String getImgPath() {
        return imgPath;
    }

    private String getImgTempPath() {
        return imgTempPath;
    }

    public void decodeFile() {
            for (File f: new File(getImgTempPath()).listFiles()) {
                if (f.isFile()) {
                    f.delete();
                }
            }

            try {
                enc = load();
            } catch (IOException e) {
                e.printStackTrace();
            }

        File f = new File(getImgTempPath() + File.separator + getImgName());

            try {
                f.createNewFile();
            } catch(IOException e) { e.printStackTrace(); }

            if (f.exists()) {
                try {
                    out = new FileOutputStream(f);
                    out.write(this.decode(enc));
                } catch(IOException e) { e.printStackTrace();
                } finally {
                    if(out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    }



    private String load() throws IOException {
	    File f = new File(getImgPath() + File.separator + getImgName());
	    String str = "";
	    BufferedReader fin = null;
		    try {
		    	fin = new BufferedReader(new FileReader(f));
		    	String line;
		    		while ((line = fin.readLine()) != null) { str += line; }
		    } catch(IOException e) { e.printStackTrace();
		    } finally { if(fin != null) { fin.close(); } }
	    return str;
	}


    private byte[] decode(String str) {
			if (str == null)  { return  null; }
		byte data[] = str.getBytes();
	    return decode(data);
	}

    private byte[] decode(byte[] data) {

	    int tail = data.length;
	        while (data[tail-1] == '=') { tail--; }
	    byte dest[] = new byte[tail - data.length/4];

	        for (int idx = 0; idx < data.length; idx++) {
	            if (data[idx] == '=')                             { data[idx] = 0; }
	            else if (data[idx] == '/')                        { data[idx] = 63; }
	            else if (data[idx] == '+')                        { data[idx] = 62; }
	            else if (data[idx] >= '0'  &&  data[idx] <= '9')  { data[idx] = (byte)(data[idx] - ('0' - 52)); }
	            else if (data[idx] >= 'a'  &&  data[idx] <= 'z')  { data[idx] = (byte)(data[idx] - ('a' - 26)); }
	            else if (data[idx] >= 'A'  &&  data[idx] <= 'Z')  { data[idx] = (byte)(data[idx] - 'A'); }
	        }

	    int sidx, didx;
	        for (sidx = 0, didx=0; didx < dest.length-2; sidx += 4, didx += 3) {
	            dest[didx] =      (byte) (((data[sidx] << 2) & 255) | ((data[sidx+1] >>> 4) & 3));
	            dest[didx+1] =    (byte) (((data[sidx+1] << 4) & 255) | ((data[sidx+2] >>> 2) & 017));
	            dest[didx+2] =    (byte) (((data[sidx+2] << 6) & 255) | (data[sidx+3] & 077));
	        }
	        if (didx < dest.length) { dest[didx] =      (byte) (((data[sidx] << 2) & 255) | ((data[sidx+1] >>> 4) & 3)); }
	        if (++didx < dest.length) { dest[didx] =    (byte) (((data[sidx+1] << 4) & 255) | ((data[sidx+2] >>> 2) & 017)); }

	    return dest;
	  }
}
