package com.habbyge.iwatch.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by habbyge on 2021/1/5.
 */
public class FileUtil {

	public static void copyFile(File src, File dest) throws IOException {
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		try {
			if (!dest.exists()) {
				dest.createNewFile();
			}
			inChannel = new FileInputStream(src).getChannel();
			outChannel = new FileOutputStream(dest).getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null) {
				inChannel.close();
			}
			if (outChannel != null) {
				outChannel.close();
			}
		}
	}

	public static boolean deleteFile(File file) {
		if (!file.exists()) {
			return true;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				deleteFile(f);
			}
		}
		return file.delete();
	}

	public static Enumeration<JarEntry> parseJarFile(File jarInputFile) {
		try {
			JarFile jarFile = new JarFile(jarInputFile); // JarFile
			return jarFile.entries();
		} catch (Exception e) {
			Log.e("iWatch.FileUtil", "parseJarFile, exception: " + e.getMessage());
			return null;
		}
	}
}
