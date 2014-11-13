package myexpenses.ng2.com.myexpenses.BroadcastReceivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import myexpenses.ng2.com.myexpenses.Activities.OverviewActivity;
import myexpenses.ng2.com.myexpenses.R;
import myexpenses.ng2.com.myexpenses.Utils.AlarmService;
import myexpenses.ng2.com.myexpenses.Utils.SharedPrefsManager;

//broadcast receiver that receives the intents broadcasted by the alarm manager and starts our alarm service
public class ReminderReceiver extends BroadcastReceiver {

    public ReminderReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        /*
        //on receive , create an intent with our service and start it
        Intent service = new Intent(context , AlarmService.class);
        context.startService(service);
*/

        //when service starts , open the preference file
        SharedPrefsManager prefsManager = new SharedPrefsManager(context);
        //if the daily reminder is enabled then go on to the procedure
        //if(prefsManager.getPrefsReminder()) {
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_key_reminder",false)){
            //get the notification service from the system
            NotificationManager manager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
            //create an intent to be  used on click notification
            Intent intent1 = new Intent(context, OverviewActivity.class);

            //create the notification and add flags
            Notification notification = new Notification(R.drawable.ic_launcher, "This is a test message", System.currentTimeMillis());
            intent1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            //this ensures that clicking on the notification opens up the overview activity
            PendingIntent pendingNotificationIntent = PendingIntent.getActivity(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.setLatestEventInfo(context, "Alarm Manager Demo", "notification content", pendingNotificationIntent);

            //build the notification and issue it
            manager.notify(0, notification);
        }

    }
}