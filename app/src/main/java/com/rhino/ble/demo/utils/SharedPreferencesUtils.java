package com.rhino.ble.demo.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.rhino.log.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.Set;

/**
 * <p>The utils of SharedPreferences</p>
 *
 * @author LuoLin
 * @since Create on 2018/10/7.
 **/
public class SharedPreferencesUtils {

    private final static String DEFAULT_SHARE_PREFERENCES_FILE_NAME = "share_preferences";
    private Context mContext;
    private SharedPreferences mSharedPreferences;

    private static SharedPreferencesUtils instance;
    public static SharedPreferencesUtils getInstance() {
        if (instance == null) {
            synchronized (SharedPreferencesUtils.class) {
                if (instance == null) {
                    instance = new SharedPreferencesUtils();
                }
            }
        }
        return instance;
    }

    private SharedPreferencesUtils() {
    }

    public void init(Context context) {
        this.init(context, DEFAULT_SHARE_PREFERENCES_FILE_NAME);
    }

    public void init(Context context, String sharePreferencesFileName) {
        this.mContext = context.getApplicationContext();
        this.mSharedPreferences = mContext.getSharedPreferences(sharePreferencesFileName, Context.MODE_PRIVATE);
    }

    public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        if (edit != null) {
            edit.putBoolean(key, value);
            edit.commit();
        }
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        if (edit != null) {
            edit.putString(key, value);
            edit.commit();
        }
    }

    public void putInt(String key, int value) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        if (edit != null) {
            edit.putInt(key, value);
            edit.commit();
        }
    }

    public void putFloat(String key, float value) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        if (edit != null) {
            edit.putFloat(key, value);
            edit.commit();
        }
    }

    public void putLong(String key, long value) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        if (edit != null) {
            edit.putLong(key, value);
            edit.commit();
        }
    }

    public void putStringSet(String key, Set<String> value) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        if (edit != null) {
            edit.putStringSet(key, value);
            edit.commit();
        }
    }

    public void putObject(String key, Object object) {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream out = null;
        try {
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(object);
            String objectVal = new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));
            SharedPreferences.Editor edit = mSharedPreferences.edit();
            if (edit != null) {
                edit.putString(key, objectVal);
                edit.commit();
            }
        } catch (IOException e) {
            LogUtils.e(e);
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                LogUtils.e(e);
            }
        }
    }

    public String getString(String key) {
        return mSharedPreferences.getString(key, "");
    }

    public String getString(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public int getInt(String key, int defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return mSharedPreferences.getLong(key, defValue);
    }

    public Set<String> getStringSet(String key, Set<String> defValue) {
        return mSharedPreferences.getStringSet(key, defValue);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> cls) {
        if (mSharedPreferences.contains(key)) {
            String objectVal = mSharedPreferences.getString(key, null);
            byte[] buffer = Base64.decode(objectVal, Base64.DEFAULT);
            ByteArrayInputStream bais = null;
            ObjectInputStream ois = null;
            try {
                bais = new ByteArrayInputStream(buffer);
                ois = new ObjectInputStream(bais);
                T t = (T) ois.readObject();
                return t;
            } catch (StreamCorruptedException e) {
                LogUtils.e(e);
            } catch (IOException e) {
                LogUtils.e(e);
            } catch (ClassNotFoundException e) {
                LogUtils.e(e);
            } finally {
                try {
                    if (bais != null) {
                        bais.close();
                    }
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException e) {
                    LogUtils.e(e);
                }
            }
        }
        return null;
    }

    public boolean contains(Context context, String key) {
        return mSharedPreferences.contains(key);
    }

    public void remove(String key) {
        mSharedPreferences.edit().remove(key).commit();
    }

    public void clearAll() {
        mSharedPreferences.edit().clear().commit();
    }

    public Map<String, ?> getAll(Context context) {
        return mSharedPreferences.getAll();
    }


}
