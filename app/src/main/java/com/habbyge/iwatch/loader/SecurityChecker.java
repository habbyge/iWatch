package com.habbyge.iwatch.loader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by habbyge on 2021/1/5.
 */
public class SecurityChecker {
	private static final String TAG = "iWatch.SecurityChecker";

	private static final String SP_NAME = "_iwatch_";
	private static final String SP_MD5 = "-md5";
	private static final String CLASSES_DEX = "classes.dex";

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	private final Context mContext;
	/**
	 * host publickey
	 */
	private PublicKey mPublicKey;
	/**
	 * host debuggable
	 */
	private boolean mDebuggable;

	public SecurityChecker(Context context) {
		mContext = context;
		init(mContext);
	}

	/**
	 * @param file Dex file
	 * @return true if verify fingerprint success
	 */
	public boolean verifyOpt(File file) {
		String fingerprint = getFileMD5(file);
		String saved = getFingerprint(file.getName());
		return fingerprint != null && TextUtils.equals(fingerprint, saved);
	}

	/**
	 * @param file Dex file
	 */
	public void saveOptSig(File file) {
		String fingerprint = getFileMD5(file);
		saveFingerprint(file.getName(), fingerprint);
	}

	/**
	 * @param path Apk file
	 * @return true if verify apk success
	 */
	public boolean verifyApk(File path) {
		if (mDebuggable) {
			Log.d(TAG, "mDebuggable = true");
			return true;
		}

		JarFile jarFile = null;
		try {
			jarFile = new JarFile(path);

			JarEntry jarEntry = jarFile.getJarEntry(CLASSES_DEX);
			if (null == jarEntry) {// no code
				return false;
			}
			loadDigestes(jarFile, jarEntry);
			Certificate[] certs = jarEntry.getCertificates();
			if (certs == null) {
				return false;
			}
			return check(path, certs);
		} catch (IOException e) {
			Log.e(TAG, path.getAbsolutePath(), e);
			return false;
		} finally {
			try {
				if (jarFile != null) {
					jarFile.close();
				}
			} catch (IOException e) {
				Log.e(TAG, path.getAbsolutePath(), e);
			}
		}
	}

	private void loadDigestes(JarFile jarFile, JarEntry je) throws IOException {
		InputStream is = null;
		try {
			is = jarFile.getInputStream(je);
			byte[] bytes = new byte[8192];
			while (is.read(bytes) > 0) {
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	// verify the signature of the Apk
	private boolean check(File path, Certificate[] certs) {
		if (certs.length > 0) {
			for (int i = certs.length - 1; i >= 0; i--) {
				try {
					certs[i].verify(mPublicKey);
					return true;
				} catch (Exception e) {
					Log.e(TAG, path.getAbsolutePath(), e);
				}
			}
		}
		return false;
	}

	private String getFileMD5(File file) {
		if (!file.isFile()) {
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[8192];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer)) != -1) {
				digest.update(buffer, 0, len);
			}
		} catch (Exception e) {
			Log.e(TAG, "getFileMD5", e);
			return null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				Log.e(TAG, "getFileMD5", e);
			}
		}
		BigInteger bigInt = new BigInteger(digest.digest());
		return bigInt.toString();
	}

	// md5 as fingerprint
	private void saveFingerprint(String fileName, String md5) {
		SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(fileName + SP_MD5, md5);
		editor.commit();
	}

	private String getFingerprint(String fileName) {
		SharedPreferences sharedPreferences = mContext.getSharedPreferences(
				SP_NAME, Context.MODE_PRIVATE);
		return sharedPreferences.getString(fileName + SP_MD5, null);
	}

	// initialize,and check debuggable
	private void init(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), PackageManager.GET_SIGNATURES);

			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			ByteArrayInputStream s = new ByteArrayInputStream(packageInfo.signatures[0].toByteArray());
			X509Certificate cert = (X509Certificate) certFactory.generateCertificate(s);

			mDebuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
			mPublicKey = cert.getPublicKey();
		} catch (NameNotFoundException e) {
			Log.e(TAG, "init", e);
		} catch (CertificateException e) {
			Log.e(TAG, "init", e);
		}
	}
}
