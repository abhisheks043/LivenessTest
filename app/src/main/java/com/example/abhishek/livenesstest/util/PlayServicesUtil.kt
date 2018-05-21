package com.example.abhishek.livenesstest.util

/**
 * Created by Abhishek on 5/12/2018.
 */

import android.app.Activity
import android.util.Log

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class PlayServicesUtil {

    private val TAG = PlayServicesUtil::class.java.simpleName

    fun isPlayServicesAvailable(activity: Activity, requestCode: Int): Boolean {

        if (activity == null) {
            return false
        }

        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, "GooglePlayServices resolvable error occurred: " + apiAvailability.getErrorString(resultCode))
                apiAvailability.getErrorDialog(activity, resultCode, requestCode).show()
            } else {
                Log.e(TAG, "GooglePlayServices not supported")

                // finish activity
                activity.finish()
            }

            return false
        }

        // All good
        return true
    }
}