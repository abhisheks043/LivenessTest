package com.example.abhishek.livenesstest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.example.abhishek.livenesstest.util.PlayServicesUtil
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.face.FaceDetector
import java.util.concurrent.atomic.AtomicBoolean
import com.example.abhishek.livenesstest.tracker.FaceTracker
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.example.abhishek.livenesstest.event.*
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERM = 69
    private val TAG = "FaceTracker"
    private var mFaceDetector: FaceDetector? = null
    private var mCameraSource: CameraSource? = null
    private val updating = AtomicBoolean(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //check for playservices installed or not
        val ps = PlayServicesUtil()
        ps.isPlayServicesAvailable(this,69)

        //camera permissions
        if(isCameraPermissionGranted())
            createCameraResources()
        else
            requestCameraPermission()


    }

    private fun isCameraPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission(){
        val permissions = arrayOf(Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(this,permissions,REQUEST_CAMERA_PERM)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraResources()
            return
        }

        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("EyeControl")
                .setMessage("No camera permission")
                .setPositiveButton("Ok", listener)
                .show()
    }


    override fun onResume() {
        super.onResume()

        // register the event bus
        EventBus.getDefault().register(this)

        // start the camera feed
        if (mCameraSource != null && isCameraPermissionGranted()) {
            try {
                mCameraSource?.start()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SecurityException){
                e.printStackTrace()
            }

        } else {
            Log.e(TAG, "onResume: Camera.start() error")
        }
    }


    override fun onPause() {
        super.onPause()

        // unregister from the event bus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }

        // stop the camera source
        if (mCameraSource != null) {
            mCameraSource?.stop()
        } else {
            Log.e(TAG, "onPause: Camera.stop() error")
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // release them all...
        if (mFaceDetector != null) {
            mFaceDetector?.release()
        } else {
            Log.e(TAG, "onDestroy: FaceDetector.release() error")
        }
        if (mCameraSource != null) {
            mCameraSource?.release()
        } else {
            Log.e(TAG, "onDestroy: Camera.release() error")
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLeftEyeClosed(e: LeftEyeClosedEvent) {
        Log.e("camera","LEFT")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRightEyeClosed(e: RightEyeClosedEvent) {
        Log.e("camera", "RIGHT")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBothEyesClosed(e: BothEyesClosedEvent) {
        Log.e("camera", "BOTH")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun RightHeadRotation(e: RightHeadRotation) {
    //    Toast.makeText(this, "Right Rotation Detected", Toast.LENGTH_SHORT).show()
        Log.e("camera", "RIGHT HEAD")
        if(!switch_head.isChecked && catchUpdatingLock()){
            switch_head.isChecked = true
            releaseUpdatingLock()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun LeftHeadRotation(e: LeftHeadRotation) {
    //    Toast.makeText(this, "Left Rotation Detected", Toast.LENGTH_SHORT).show()
        Log.e("camera", "LEFT HEAD")
        if(switch_head.isChecked && catchUpdatingLock()){
            switch_head.isChecked = false
            releaseUpdatingLock()
        }
    }


    private fun catchUpdatingLock(): Boolean {
        // set updating and return previous value
        return !updating.getAndSet(true)
    }

    private fun releaseUpdatingLock() {
        updating.set(false)
    }


    private fun createCameraResources(){
        val context = applicationContext

        // create and setup the face detector
        mFaceDetector = FaceDetector.Builder(context)
                .setProminentFaceOnly(true) // optimize for single, relatively large face
                .setTrackingEnabled(true) // enable face tracking
                .setClassificationType(/* eyes open and smile */FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE) // for one face this is OK
                .build()


        // now that we've got a detector, create a processor pipeline to receive the detection
        // results
        mFaceDetector?.setProcessor(LargestFaceFocusingProcessor(mFaceDetector, FaceTracker()))

        // operational...?
        if (!mFaceDetector!!.isOperational) {
            Log.w(TAG, "createCameraResources: detector NOT operational")
        } else {
            Log.d(TAG, "createCameraResources: detector operational")
        }

        // Create camera source that will capture video frames
        // Use the front camera
        mCameraSource = CameraSource.Builder(this, mFaceDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30f)
                .build()


    }




}
