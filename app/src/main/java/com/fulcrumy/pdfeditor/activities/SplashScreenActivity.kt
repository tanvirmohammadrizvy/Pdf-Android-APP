package com.fulcrumy.pdfeditor.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import com.fulcrumy.pdfeditor.MyApp
import com.fulcrumy.pdfeditor.R
import com.fulcrumy.pdfeditor.data.Constants
import com.fulcrumy.pdfeditor.utils.Utils

class SplashScreenActivity : AppCompatActivity() {

    private var TAG: String = "SplashScreenActivity"
    private var context: Context? = null

    private var appIconSplash: ImageView? = null
    private var splashLoader: LottieAnimationView? = null
    private var appNameSplash: TextView? = null

    private var alertDialogBuilder: AlertDialog.Builder? = null
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyApp.getInstance().isNightModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        setContentView(R.layout.activity_splash_screen)

        appIconSplash = findViewById(R.id.appIconSplash)
        splashLoader = findViewById(R.id.splashLoader)
        appNameSplash = findViewById(R.id.appNameSplash)

        appNameSplash!!.alpha = 0f
        appNameSplash!!.animate()
            .translationY(appNameSplash!!.height.toFloat())
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(1000)
            .translationY(appNameSplash!!.height.toFloat())
            .alpha(1f)
            .setDuration(1200).startDelay = 1500

        appIconSplash!!.alpha = 0f
        appIconSplash!!.animate()
            .translationY(appIconSplash!!.height.toFloat())
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(1000)
            .translationY(appIconSplash!!.height.toFloat())
            .alpha(1f)
            .setDuration(1200).startDelay = 1500


        splashLoader!!.alpha = 0f
        splashLoader!!.animate()
            .translationY(splashLoader!!.height.toFloat())
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(1000)
            .translationY(splashLoader!!.height.toFloat())
            .alpha(1f)
            .setDuration(1200).startDelay = 1500

    }

    override fun onResume() {
        super.onResume()

        if (!Utils.isPermissionGranted(this)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                alertDialogBuilder = AlertDialog.Builder(this)
                    .setTitle("All files permission")
                    .setMessage("Due to Android 11 restrictions, this app requires all files permission")
                    .setPositiveButton("Allow") { dialogInterface, i -> takePermission() }
                    .setNegativeButton("Deny") { dialogInterface, i ->
                        Toast.makeText(applicationContext, getString(R.string.app_name) + " cannot function without the permission", Toast.LENGTH_LONG).show()
                        exitApp()
                    }
                    .setIcon(R.drawable.pdf)

                alertDialog = alertDialogBuilder!!.create()

            } else {
                alertDialogBuilder = AlertDialog.Builder(this)
                    .setTitle("All files permission")
                    .setMessage("Please allow storage permission")
                    .setPositiveButton("Allow") { dialogInterface, i -> takePermission() }
                    .setNegativeButton("Deny") { dialogInterface, i ->
                        Toast.makeText(applicationContext, getString(R.string.app_name) + " cannot function without the permission", Toast.LENGTH_LONG).show()
                        exitApp()
                    }
                    .setIcon(R.drawable.pdf)

                alertDialog = alertDialogBuilder!!.create()

            }

            Handler().postDelayed(object: Runnable {
                override fun run() {
                    alertDialog!!.show()
                }
            }, 4000)

        } else {
            //Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Permission Granted")
            goToMainScreen()
        }
    }

    private fun takePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {

                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 101
                )

                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, 101)
            } catch (e: Exception) {
                e.printStackTrace()

                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 101
                )

                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 101)
            }
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 101
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0) {
            if (requestCode == 101) {
                val readExt = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!readExt) {
                    takePermission()
                }
            }
        }
    }

    private fun goToMainScreen() {
        Handler().postDelayed(object: Runnable {
            override fun run() {
                startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
            }
        }, Constants.SPLASH_SCREEN_TIMEOUT.toLong())

    }

    private fun exitApp() {
        finish()
    }

}