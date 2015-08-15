package com.tenney.utils.jave;

import java.io.File;
import java.io.FileNotFoundException;

public class Test {
	public static void main(String[] args){
		Encoder encoder = new Encoder();
		String parentPath="/Users/tenney/Desktop" ;//Test.class.getResource("/").getPath();
		try {
			encoder.getImage(new File(parentPath+"/rmvb01.mp4"), new File(parentPath+"/rmvb01.jpg"), 1);
		} catch (EncoderException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
	}
}