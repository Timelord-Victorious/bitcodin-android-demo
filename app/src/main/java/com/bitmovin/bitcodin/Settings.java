package com.bitmovin.bitcodin;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Set;

@SuppressWarnings("FieldCanBeLocal")
public final class Settings {
  public static final String AUTH_BASE_URL = "https://portal.bitcodin.com/auth/";
  public static final String STORAGE_DIR =
      android.os.Environment.getExternalStorageDirectory() + "/data/com.bitmovin.bitdash/";
  public static String USER_FOLDER;
  public static String THUMBNAIL_CACHE;
  public static String JOB_CACHE;
  public static boolean INITIALIZED = false;
  public static long FADE_OUT_TIMEOUT = 3500;
  public static long FADE_IN_DURATION = 250;
  public static long FADE_OUT_DURATION = 1000;
  public static long SLIDE_DURATION = 500;
  public static long CONTROL_UPDATE_RATE = 500;
  public static String WIDEVINE_GTS_DEFAULT_BASE_URI =
      "http://widevine-proxy.appspot.com/proxy";
  public static String DEFAULT_PLAYER_URI =
      "http://bitdash-a.akamaihd.net/content/MI201109210084_1/" +
          "mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd";
  public static int DEFAULT_PLAYER_TYPE = PlayerActivity.TYPE_DASH;

  private static String thumbnailCache;
  private static SharedPreferences mPreferences;
  private static SharedPreferences.Editor mPreferenceEditor;

  public static void init(Context context, String apiKey) {
    USER_FOLDER = encryptPassword(apiKey);
    THUMBNAIL_CACHE = STORAGE_DIR + USER_FOLDER + "thumbnails/";
    JOB_CACHE = STORAGE_DIR + USER_FOLDER + "jobs/";

    mPreferences = context.getSharedPreferences("bitcodin", Context.MODE_PRIVATE);
    mPreferenceEditor = mPreferences.edit();
    thumbnailCache = mPreferences.getString(
        "thumbnail_cache",
        "" + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
    );
    mPreferenceEditor.putString("thumbnail_cache", thumbnailCache);
    mPreferenceEditor.apply();

    INITIALIZED = true;
  }

  public static String encryptPassword(String password) {
    ArrayList<String> supportedHashingAlgorithms = new ArrayList<>();
    /* from http://stackoverflow.com/questions/12844472/get-every-algorithm-messagedigest-can-use */
    Provider[] providers = Security.getProviders();
    for (Provider p : providers) {
      Set<Provider.Service> services = p.getServices();
      for (Provider.Service s : services) {
        if ("MessageDigest".equals(s.getType())) {
          supportedHashingAlgorithms.add(s.getAlgorithm());
        }
      }
    }

    MessageDigest mMessageDigest;
    try {
      if (supportedHashingAlgorithms.contains("SHA-512")) {
        mMessageDigest = MessageDigest.getInstance("SHA-512");
      } else if (supportedHashingAlgorithms.contains("SHA-384")) {
        mMessageDigest = MessageDigest.getInstance("SHA-384");
      } else if (supportedHashingAlgorithms.contains("SHA-256")) {
        mMessageDigest = MessageDigest.getInstance("SHA-256");
      } else {
        mMessageDigest = MessageDigest.getInstance(supportedHashingAlgorithms.get(0));
      }
    } catch (Exception ex) {
      mMessageDigest = null;
    }

    if (mMessageDigest != null) {
      mMessageDigest.update(password.getBytes());
      byte hash[] = mMessageDigest.digest();
      byte small[] = new byte[10];
      int idx = 0;
      for (byte b : hash) {
        small[idx++] ^= b;
        idx = (idx > 9) ? 0 : idx;
      }

      return bytesToHex(small) + "/";
    }
    return "generic/";
  }

  /* from http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java */
  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
}