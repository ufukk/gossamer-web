package com.conceptimago.notificationlibrary;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;

public class Notify {

    private Context mContext;
    private NotificationManager notificationManager;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder mBuilder;
    private String mBigPictureUrl = "", mOnlyBigPictureUrl = "";

    /**
     * init variables
     * @param context, context of the parent activity
     * @param intentStringToLaunchActivity intent string to start activity
     */
    public Notify(Context context, String intentStringToLaunchActivity) {
        mContext = context;
        /** get notification manager **/
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        /** get pending intent to launch activity after notification has been touched **/
        pendingIntent = getPendingIntent(intentStringToLaunchActivity, null);
        /** create builder **/
        mBuilder = new NotificationCompat.Builder(mContext).setAutoCancel(true);
    }

    /**
     * init for direct intent
     * @param context, context of the parent activity
     */
    public Notify(Context context) {
        mContext = context;
        /** get notification manager **/
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        /** create builder **/
        mBuilder = new NotificationCompat.Builder(mContext).setAutoCancel(true);
    }

    /** get intent from real intent **/
    public PendingIntent getPendingIntent(Intent intentToLaunchActivity, String actionString) {
        return PendingIntent.getActivity(
                mContext, (int) System.currentTimeMillis(),
                intentToLaunchActivity, PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * get pending intent
     * @param intentStringToLaunchActivity intent activity
     * @param actionString action button string
     * @return pending intent
     */
    public PendingIntent getPendingIntent(String intentStringToLaunchActivity, String actionString) {
        Intent intentData = new Intent(intentStringToLaunchActivity);
        /** set data to intent for notification opening **/
        if(actionString != null) intentData.putExtra("actionString", actionString);
        return PendingIntent.getActivity(
                mContext, (int) System.currentTimeMillis(),
                intentData, PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * create basic notification with title text and icon
     * @param notificationIcon notification icon
     * @param title title of notification
     * @param content content of notification
     */
    public Notify createBasicNotification(int notificationIcon, String title, String content) {
        mBuilder.setSmallIcon(notificationIcon)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent);
        return this;
    }

    /**
     *
     * @param notificationIcon notification icon
     * @param title title
     * @param content content of the big text
     */
    public Notify createBigTextNotification(int notificationIcon, String title, String content) {
        mBuilder.setSmallIcon(notificationIcon)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(pendingIntent);
        return this;
    }

    /**
     * notification for only big picture
     * @param notificationIcon
     * @param onlyBigPictureUrl
     */
    public Notify createOnlyBigPictureNotification(int notificationIcon, String onlyBigPictureUrl) {
        mOnlyBigPictureUrl = onlyBigPictureUrl;
        mBuilder.setSmallIcon(notificationIcon).setContentIntent(pendingIntent);
        return this;
    }

    /**
     * create big picture notifications
     * @param notificationIcon notification icon
     * @param title notification title
     * @param content notification content
     * @param bigPictureUrl big picture url
     */
    public Notify createBigPictureNotification(int notificationIcon, String title, String content, String bigPictureUrl) {
        mBigPictureUrl = bigPictureUrl;
        mBuilder.setSmallIcon(notificationIcon)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent);
        return this;
    }

    /**
     * adds action to notification
     * @param iconForAction icon of the action
     * @param intentStringToLaunchActivity intent activity
     * @param actionString title of action
     */
    public Notify addActionToNotification(int iconForAction, String intentStringToLaunchActivity, String actionString) {
        PendingIntent actionPendingIntent = getPendingIntent(intentStringToLaunchActivity, actionString);
        mBuilder.addAction(iconForAction, actionString, actionPendingIntent);
        return this;
    }

    /**
     * push notification to user
     * some of them waits for callback to push notification
     **/
    public void pushNotification(int notificationId) {
        /** expanded pic **/
        if(!mBigPictureUrl.equals("")) bigPictureUrl(notificationId);
        /** only big picture **/
        else if(!mOnlyBigPictureUrl.equals("")) onlyBigPictureUrl(notificationId);
        else notificationManager.notify(notificationId, mBuilder.build());
    }

    /**
     * only big picture url method
     * @param notificationId
     */
    private void onlyBigPictureUrl(final int notificationId) {
        glideMethod(notificationId, mOnlyBigPictureUrl, 1);
    }

    /**
     * big picture url method
     * @param notificationId
     */
    private void bigPictureUrl(final int notificationId) {
        glideMethod(notificationId, mBigPictureUrl, 2);
    }

    /**
     * glide for downloading
     * @param notificationId
     * @param downloadUrl
     * @param type
     */
    private void glideMethod(final int notificationId, String downloadUrl, final int type) {
        Glide
                .with(mContext)
                .load(downloadUrl)
                /** get picture as bitmap **/
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        /** add remoteview to notification content **/
                        if(type == 1) {
                            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.picturewidget);
                            /** image bitmap **/
                            remoteViews.setImageViewBitmap(R.id.pictureWidgetIv, resource);
                            mBuilder.setContent(remoteViews);
                        } else
                            /** add big picture **/
                            mBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(resource));
                        notificationManager.notify(notificationId, mBuilder.build());
                    }
                });
    }

    /**
     * add key listener for notification action
     * you will receive key as action string
     * eg. adding "buy now" action will result in seeing "buy now" string in bundles
     * **/
    public void addActionKeyListener(ActionListener actionListener) {
        /** if action key is pressed **/
        Bundle actionKeys = ((Activity) mContext).getIntent().getExtras();
        if (actionKeys != null && actionListener != null) actionListener.actionKeyListener(actionKeys);
    }

    /** add static action key listener **/
    public static void addStaticActionKeyListener(ActionListener actionListener) {
        /** if action key is pressed **/
        Bundle actionKeys = ((Activity) actionListener).getIntent().getExtras();
        if (actionKeys != null) {
            NotificationManager notificationManager = (NotificationManager) ((Activity)actionListener).getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            actionListener.actionKeyListener(actionKeys);
        }
    }

    /** interface for notification action **/
    public interface ActionListener {
        void actionKeyListener(Bundle actionKeys);
    }

    public Notify createAppRedirectNotification(String packageToMatch, int notificationIcon, String title, String content) {
        /** get package manager **/
        final PackageManager pm = mContext.getPackageManager();
        /** get installed applications **/
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        /** is installed ? set to false first **/
        boolean isInstalled = false;
        /** launch intent **/
        Intent launchIntent = null;
        /** get packages **/
        if(packages != null && packages.size() > 0) {
            /** info of application **/
            for (ApplicationInfo packageInfo : packages) {
                /** if matches **/
                if(packageInfo.packageName != null && packageInfo.packageName.equals(packageToMatch)) {
                    /** if package is installed, than get the intent **/
                    launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName);
                    isInstalled = true;
                }
            }
        }

        /** not installed so go to google play **/
        if(!isInstalled) {
            try {
                /** launch app so its installed **/
                launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageToMatch));
            } catch (android.content.ActivityNotFoundException anfe) {
                /** go to google play **/
                launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageToMatch));
            }
        }

        /** get pending intent to launch activity after notification has been touched **/
        pendingIntent = getPendingIntent(launchIntent, null);

        /** build notification **/
        mBuilder.setSmallIcon(notificationIcon)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent);

        return this;

    }

}
