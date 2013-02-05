package com.zlab.datFM;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class datFM_ZA_Interface extends BroadcastReceiver
{
    private static final String ZARCHIVER_PRO_IEXT_ACTION = "ru.zdevs.zarchiver.pro.action.EXTERNAL";

    private static final String ZARCHIVER_PRO_IEXT_CATEGORY_EXTRACT = "ru.zdevs.zarchiver.pro.category.EXTRACT";
    private static final String ZARCHIVER_PRO_IEXT_CATEGORY_COMPRESS = "ru.zdevs.zarchiver.pro.category.COMPRESS";
    private static final String ZARCHIVER_PRO_IEXT_CATEGORY_OPEN = "ru.zdevs.zarchiver.pro.category.OPEN";
    private static final String ZARCHIVER_PRO_IEXT_CATEGORY_CALLBACK = "ru.zdevs.zarchiver.pro.category.CALLBACK";

    private static final String ZARCHIVER_PRO_IEXT_FIELD_ID = "ru.zdevs.zarchiver.pro.field.ID";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_SUCESSFULL = "ru.zdevs.zarchiver.pro.field.SUCESSFULL";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_ACTION = "ru.zdevs.zarchiver.pro.field.ACTION";

    private static final String ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_PATH = "ru.zdevs.zarchiver.pro.field.ARCHIVEPATH";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_EXTRACT_TO_PATH = "ru.zdevs.zarchiver.pro.field.EXTRACTTO";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_FILE_PATH = "ru.zdevs.zarchiver.pro.field.FILEPATH";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_FILE_LIST = "ru.zdevs.zarchiver.pro.field.FILELIST";

    private static final String ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE = "ru.zdevs.zarchiver.pro.field.TYPE";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE_ZIP = "zip";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE_7Z = "7z";
    private static final String ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE_TAR = "tar";

    private static final String ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL = "ru.zdevs.zarchiver.pro.field.LAVEL";

    private static final int ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL_NORMAL = 5;
    private static final int ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL_FAST = 3;
    private static final int ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL_MAX = 7;

    public static final String ARCHIVE_TYPE_ZIP = ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE_ZIP;
    public static final String ARCHIVE_TYPE_7Z = ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE_7Z;
    public static final String ARCHIVE_TYPE_TAR = ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE_TAR;

    public static final int COMPRESSION_LAVEL_NORMAL = ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL_NORMAL;
    public static final int COMPRESSION_LAVEL_FAST = ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL_FAST;
    public static final int COMPRESSION_LAVEL_MAX = ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL_MAX;

    public static final int ACTION_UNKNOWN = 0;
    public static final int ACTION_EXTRACT = 1;
    public static final int ACTION_COMPRESS = 1;

    public static final int TASKID_BAD = 0;

    private boolean mZArchiverFound = false;

    private OnActionComplete mCallback = null;
    private Context mContext = null;

    public datFM_ZA_Interface(Context context)
    {
        mContext = context;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ZARCHIVER_PRO_IEXT_ACTION);
        filter.addCategory(ZARCHIVER_PRO_IEXT_CATEGORY_CALLBACK);
        context.registerReceiver(this, filter);

        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(ZARCHIVER_PRO_IEXT_ACTION);
        intent.addCategory(ZARCHIVER_PRO_IEXT_CATEGORY_COMPRESS);
        intent.addCategory(ZARCHIVER_PRO_IEXT_CATEGORY_EXTRACT);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        mZArchiverFound = resolveInfo.size() > 0;
    }

    public boolean isSupport()
    {
        return mZArchiverFound;
    }

    private int getTaskID()
    {
        //TaskID will not be zero
        return (int)(Integer.MAX_VALUE*Math.random() + 1);
    }

    public int ExtractArchive(String sArchivePath, String sExtractTo)
    {
        if ( !mZArchiverFound )
            return 0;

        int taskID = getTaskID();

        Intent intent = new Intent(ZARCHIVER_PRO_IEXT_ACTION);
        intent.addCategory(ZARCHIVER_PRO_IEXT_CATEGORY_EXTRACT);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_ID, taskID);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_PATH, sArchivePath);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_EXTRACT_TO_PATH, sExtractTo);
        try
        {
            mContext.startActivity(intent);
        } catch (Exception E)
        {
            return 0;
        }
        return taskID;
    }

    public int CreateArchive(String sArchivePath, String sFilePath, String[] sFileList)
    {
        return CreateArchive(sArchivePath, sFilePath, sFileList, ARCHIVE_TYPE_ZIP, COMPRESSION_LAVEL_FAST);
    }

    public int CreateArchive(String sArchivePath, String sFilePath, String[] sFileList, String sType)
    {
        return CreateArchive(sArchivePath, sFilePath, sFileList, sType, COMPRESSION_LAVEL_FAST);
    }

    public int CreateArchive(String sArchivePath, String sFilePath, String[] sFileNameList, String sType, int iLavel)
    {
        if ( !mZArchiverFound )
            return 0;

        int taskID = getTaskID();

        Intent intent = new Intent(ZARCHIVER_PRO_IEXT_ACTION);
        intent.addCategory(ZARCHIVER_PRO_IEXT_CATEGORY_COMPRESS);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_ID, taskID);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_PATH, sArchivePath);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_FILE_PATH, sFilePath);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_FILE_LIST, sFileNameList);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_TYPE, sType);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_COMPRESSION_LAVEL, iLavel);
        try
        {
            mContext.startActivity(intent);
        } catch (Exception E)
        {
            return 0;
        }
        return taskID;
    }

    boolean OpenArchive(String sArchivePath, String sExtractTo)
    {
        if ( !mZArchiverFound )
            return false;

        Intent intent = new Intent(ZARCHIVER_PRO_IEXT_ACTION);
        intent.addCategory(ZARCHIVER_PRO_IEXT_CATEGORY_OPEN);
        intent.putExtra(ZARCHIVER_PRO_IEXT_FIELD_ARCHIVE_PATH, sArchivePath);
        try
        {
            mContext.startActivity(intent);
        } catch (Exception E)
        {
            return false;
        }
        return true;
    }

    public interface OnActionComplete
    {
        void onActionComplete(int iTaskID, int iAction, boolean bSucessful);
    }

    public void setOnActionComplete( OnActionComplete callback )
    {
        mCallback = callback;
    }

    private static boolean isCallback(Intent intent)
    {
        return ZARCHIVER_PRO_IEXT_ACTION.equals(intent.getAction()) &&
                intent.getCategories().contains(ZARCHIVER_PRO_IEXT_CATEGORY_CALLBACK);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if ( mCallback != null && isCallback(intent) )
        {
            boolean sucessful = intent.getBooleanExtra(ZARCHIVER_PRO_IEXT_FIELD_SUCESSFULL, false);
            int action = ACTION_UNKNOWN;
            if ( ZARCHIVER_PRO_IEXT_CATEGORY_EXTRACT.equals(intent.getStringExtra(ZARCHIVER_PRO_IEXT_FIELD_ACTION)) )
                action = ACTION_EXTRACT;
            else if ( ZARCHIVER_PRO_IEXT_CATEGORY_COMPRESS.equals(intent.getStringExtra(ZARCHIVER_PRO_IEXT_FIELD_ACTION)) )
                action = ACTION_COMPRESS;
            int task_id = intent.getIntExtra(ZARCHIVER_PRO_IEXT_FIELD_ID, TASKID_BAD);

            mCallback.onActionComplete(task_id, action, sucessful);
        }
    }
}