package com.example.abhishek.livenesstest.tracker

import android.util.Log
import com.example.abhishek.livenesstest.event.*
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.face.Face
import org.greenrobot.eventbus.EventBus


/**
 * Created by Abhishek on 5/12/2018.
 */

class FaceTracker : Tracker<Face>() {
    private var leftClosed: Boolean = false
    private var rightClosed: Boolean = false
    private val right = -30.0
    private val left = 30.0

    override fun onUpdate(detections: Detector.Detections<Face>?, face: Face?) {
        if (leftClosed && face!!.getIsLeftEyeOpenProbability() > PROB_THRESHOLD) {
            leftClosed = false
        } else if (!leftClosed && face!!.getIsLeftEyeOpenProbability() < PROB_THRESHOLD) {
            leftClosed = true
        }
        if (rightClosed && face!!.getIsRightEyeOpenProbability() > PROB_THRESHOLD) {
            rightClosed = false
        } else if (!rightClosed && face!!.getIsRightEyeOpenProbability() < PROB_THRESHOLD) {
            rightClosed = true
        }

        if (leftClosed && !rightClosed) {
            EventBus.getDefault().post(LeftEyeClosedEvent())
        } else if (rightClosed && !leftClosed) {
            EventBus.getDefault().post(RightEyeClosedEvent())
        } else if (!leftClosed && !rightClosed) {
            EventBus.getDefault().post(NeutralFaceEvent())
        } else if (rightClosed && leftClosed) {
            EventBus.getDefault().post(BothEyesClosedEvent())
        }


        if(face!!.eulerY < right)
            EventBus.getDefault().post(RightHeadRotation())
        else if(face.eulerY > left)
            EventBus.getDefault().post(LeftHeadRotation())

        Log.e("face", face.eulerY.toString())
        Log.e("face", face.getIsLeftEyeOpenProbability().toString())
        Log.e("face", face.getIsRightEyeOpenProbability().toString())
    }

    companion object {

        private val PROB_THRESHOLD = 0.5f
        private val TAG = FaceTracker::class.java.simpleName
    }
}