package com.fulcrumy.pdfeditor.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.util.Constants
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.google.android.gms.ads.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.fulcrumy.pdfeditor.MyApp
import com.fulcrumy.pdfeditor.R
import com.fulcrumy.pdfeditor.data.DbHelper
import com.fulcrumy.pdfeditor.fragments.SettingsFragment
import com.fulcrumy.pdfeditor.fragments.TableContentsFragment
import com.fulcrumy.pdfeditor.helper.DataUpdatedEvent.PermanetlyDeleteEvent
import com.fulcrumy.pdfeditor.helper.ScrollHandle
import com.fulcrumy.pdfeditor.utils.AdManagerInterstitial
import com.fulcrumy.pdfeditor.utils.Utils
import com.shockwave.pdfium.PdfPasswordException
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*

class PDFViewerActivity : AppCompatActivity() {

    val TAG = PDFViewerActivity::class.java.simpleName
    private val mHideHandler = Handler()
    private val mShowPart2Runnable = Runnable { `lambda$new$2$PDFViewerActivity`() }

    var pageNumberTextview: TextView? = null
    var backgroundLoadPdf: AsyncTask<*, *, *>? = null
    var colorPrimaryDark = 0
    var colorPrimaryDarkNight = 0
    var context: Context? = null
    var dbHelper: DbHelper? = null
    var filePath: String? = null
    var fitPolicy: FitPolicy? = null
    var flags = 0
    var mActionBar: ActionBar? = null
    var mAdView: AdView? = null
    var mPassword = ""
    var nightModeEnabled = false
    var onPageChangeListener = OnPageChangeListener { i, i2 ->
        pageNumberTextview!!.text = (i + 1).toString() + " / " + i2
    }
    var openPdfProgress: ProgressBar? = null
    var onLoadCompleteListener = OnLoadCompleteListener {
        openPdfProgress!!.visibility = 8
        pageNumberTextview!!.visibility = 0
    }
    var pageNumber = 0
    var pdfContainer: LinearLayout? = null
    var pdfFileLocation: String? = null
    var pdfView: PDFView? = null
    private val mHidePart2Runnable = Runnable { pdfView!!.systemUiVisibility = 4615 }
    var sharedPreferences: SharedPreferences? = null
    var swipeHorizontalEnabled = false
    var toolbar: Toolbar? = null
    var uri: Uri? = null
    var onErrorListener = OnErrorListener { th ->
        if (th is PdfPasswordException) {
            showEnterPasswordDialog()
            return@OnErrorListener
        }
        Toast.makeText(this@PDFViewerActivity, th.message, 1).show()
        openPdfProgress!!.visibility = 8
    }
    var view: View? = null
    private var AUTO_HIDE = false
    private var mMenu: Menu? = null
    private var mVisible = true
    private val mHideRunnable = Runnable { `lambda$new$3$PDFViewerActivity`() }
    var toggleFullScreen = View.OnClickListener { view -> `lambda$new$4$PDFViewerActivity`(view) }
    private var rememberLastPage = false
    private var showRemoveAds = false
    private var stayAwake = false

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        if (MyApp.getInstance().isNightModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        setContentView(R.layout.activity_pdf_viewer)
        toolbar = findViewById<View>(R.id.toolbar_home) as Toolbar
        openPdfProgress = findViewById<View>(R.id.progress_bar_open_pdf) as ProgressBar
        pageNumberTextview = findViewById<View>(R.id.page_numbers) as TextView
        pdfView = findViewById<View>(R.id.pdfView) as PDFView
        pdfContainer = findViewById<View>(R.id.pdf_container) as LinearLayout
        context = this
        setSupportActionBar(toolbar)
        val supportActionBar = supportActionBar
        mActionBar = supportActionBar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences = defaultSharedPreferences
        stayAwake = defaultSharedPreferences.getBoolean(SettingsFragment.KEY_PREFS_STAY_AWAKE, true)
        rememberLastPage =
            sharedPreferences!!.getBoolean(SettingsFragment.KEY_PREFS_REMEMBER_LAST_PAGE, true)
        var i = 0
        AUTO_HIDE = sharedPreferences!!.getBoolean(SettingsFragment.AUTO_FULL_SCREEN_ENABLED, false)
        swipeHorizontalEnabled =
            sharedPreferences!!.getBoolean(SettingsFragment.SWIPE_HORIZONTAL_ENABLED, false)
        nightModeEnabled =
            sharedPreferences!!.getBoolean(SettingsFragment.NIGHT_MODE_ENABLED_PDFVIEW, false)
        val decorView = (context as Activity?)!!.window.decorView
        view = decorView
        flags = decorView.systemUiVisibility
        colorPrimaryDark = context!!.resources.getColor(R.color.colorPrimaryDark)
        colorPrimaryDarkNight = context!!.resources.getColor(R.color.colorPrimaryDarkNight)
        Constants.THUMBNAIL_RATIO = 0.7f
        val intent = intent
        pdfFileLocation =
            intent.getStringExtra(com.fulcrumy.pdfeditor.data.Constants.PDF_LOCATION)
        showRemoveAds = intent.getBooleanExtra(
            com.fulcrumy.pdfeditor.data.Constants.SHOW_REMOVE_ADS,
            false
        )
        uri = intent.data
        dbHelper = DbHelper.getInstance(this)
        pdfView!!.keepScreenOn = stayAwake
        if (rememberLastPage) {
            i = dbHelper!!.getLastOpenedPage(pdfFileLocation)
        }
        pageNumber = i
        val fitPolicy2 =
            if (Utils.isTablet(this) || swipeHorizontalEnabled) FitPolicy.HEIGHT else FitPolicy.WIDTH
        fitPolicy = fitPolicy2
        loadPdfFile(mPassword, pageNumber, swipeHorizontalEnabled, nightModeEnabled, fitPolicy2)
        pdfView!!.setOnClickListener(toggleFullScreen)
        setShowRemoveAds()
    }

    public override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val asyncTask = backgroundLoadPdf
        asyncTask?.cancel(true)
    }

    public override fun onDestroy() {
        super.onDestroy()
        val asyncTask = backgroundLoadPdf
        asyncTask?.cancel(true)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_pdf_viewer, menu)
        mMenu = menu
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        val findItem = mMenu!!.findItem(R.id.action_toggle_view)
        val findItem2 = mMenu!!.findItem(R.id.action_toggle_night_mode)
        setupToggleSwipeIcons(findItem, swipeHorizontalEnabled)
        setupNightModeIcons(findItem2, nightModeEnabled)
        Handler(Looper.getMainLooper()).postDelayed({
        }, 2000)
        return true
    }

    public override fun onActivityResult(i: Int, i2: Int, intent: Intent?) {
        super.onActivityResult(i, i2, intent)
        if (i == 7 && i2 == -1) {
            pdfView!!.jumpTo(intent!!.getIntExtra(com.fulcrumy.pdfeditor.data.Constants.PAGE_NUMBER, pdfView!!.currentPage) - 1, true)
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_bookmark -> addPageBookmark(this, filePath, pdfView!!.currentPage + 1)
            R.id.action_contents -> showContents(filePath)
            R.id.action_delete -> showDeleteConfirmDialog(filePath)
            R.id.action_jump_to_page -> jumpToPage()
            R.id.action_pdf_tools -> showPdfTools()
            R.id.action_print -> print()
            R.id.action_share -> share()
            R.id.action_share_as_picture -> shareAsPicture()
            R.id.action_toggle_night_mode -> toggleNightMode(menuItem)
            R.id.action_toggle_view -> togglePDFView(menuItem)
        }
        return super.onOptionsItemSelected(menuItem)
    }

    public override fun onStop() {
        super.onStop()
        sharedPreferences!!.edit().putInt(TableContentsFragment.SAVED_STATE, 0).apply()
        if (rememberLastPage && !TextUtils.isEmpty(pdfFileLocation)) {
            AsyncTask.execute { `lambda$onStop$0$PDFViewerActivity`() }
        }
    }

    /* synthetic */   fun `lambda$onStop$0$PDFViewerActivity`() {
        dbHelper!!.addLastOpenedPage(filePath, pdfView!!.currentPage)
    }

    fun loadPdfFile(str: String?, i: Int, z: Boolean, z2: Boolean, fitPolicy2: FitPolicy?) {
        val configurator: PDFView.Configurator?
        val uri2 = uri
        if (uri2 != null) {
            try {
                filePath = uri2.path
                mActionBar!!.title = File(filePath).name as CharSequence
            } catch (e: Exception) {
                mActionBar!!.title = "View PDF"
                e.printStackTrace()
            }
            configurator = pdfView!!.fromUri(uri)
        } else if (!TextUtils.isEmpty(pdfFileLocation)) {
            filePath = pdfFileLocation
            val file = File(pdfFileLocation)
            val str2 = TAG
            Log.d(str2, "path from selection " + file.path)
            mActionBar!!.title = file.name as CharSequence
            val fromFile = pdfView!!.fromFile(file)
            AsyncTask.execute(object : Runnable {
                /* synthetic */ var `f$1`: File? = null
                override fun run() {
                    `lambda$loadPdfFile$1$PDFViewerActivity`(`f$1`)
                }

                init {
                    `f$1` = file
                }
            })
            configurator = fromFile
        } else {
            configurator = null
        }
        configurator?.scrollHandle(
            ScrollHandle(
                this
            )
        )?.password(str)
            ?.enableAnnotationRendering(true)?.pageFitPolicy(fitPolicy2)?.spacing(6)?.defaultPage(i)
            ?.swipeHorizontal(z)?.autoSpacing(z)?.pageFling(z)?.pageSnap(z)?.nightMode(z2)
            ?.onPageChange(onPageChangeListener)?.onLoad(onLoadCompleteListener)?.onError(
                onErrorListener
            )
            ?.load()
    }

    /* synthetic */   fun `lambda$loadPdfFile$1$PDFViewerActivity`(file: File?) {
        dbHelper!!.addRecentPDF(file!!.absolutePath)
    }

    fun getName(uri2: Uri?): String {
        val query = contentResolver.query(
            uri2!!,
            null as Array<String?>?,
            null as String?,
            null as Array<String?>?,
            null as String?
        )
            ?: return "Unknown"
        val columnIndex = query.getColumnIndex("_display_name")
        query.moveToFirst()
        val string = query.getString(columnIndex)
        query.columnNames
        query.close()
        return string
    }

    fun showShareAsPicture(uri2: Uri?) {
    }

    /* synthetic */   fun `lambda$new$2$PDFViewerActivity`() {
        val supportActionBar = supportActionBar
        supportActionBar?.show()
    }

    /* synthetic */   fun `lambda$new$4$PDFViewerActivity`(view2: View?) {
        toggle()
    }

    fun togglePDFView(menuItem: MenuItem) {
        swipeHorizontalEnabled =
            sharedPreferences!!.getBoolean(SettingsFragment.SWIPE_HORIZONTAL_ENABLED, false)
        val z = sharedPreferences!!.getBoolean(SettingsFragment.NIGHT_MODE_ENABLED_PDFVIEW, false)
        val edit = sharedPreferences!!.edit()
        setupToggleSwipeIcons(menuItem, !swipeHorizontalEnabled)
        if (swipeHorizontalEnabled) {
            loadPdfFile(
                mPassword,
                pdfView!!.currentPage,
                !swipeHorizontalEnabled,
                z,
                FitPolicy.WIDTH
            )
            edit.putBoolean(SettingsFragment.SWIPE_HORIZONTAL_ENABLED, !swipeHorizontalEnabled)
                .apply()
            Toast.makeText(context, "Vertical swipe enabled", 0).show()
            return
        }
        loadPdfFile(mPassword, pdfView!!.currentPage, !swipeHorizontalEnabled, z, FitPolicy.HEIGHT)
        edit.putBoolean(SettingsFragment.SWIPE_HORIZONTAL_ENABLED, !swipeHorizontalEnabled).apply()
        Toast.makeText(context, "Horizontal swipe enabled", 0).show()
    }

    fun addPageBookmark(context2: Context, str: String?, i: Int) {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context2)
        val editText = EditText(context2)
        editText.setHint(R.string.enter_title)
        val f = context2.resources.displayMetrics.density
        materialAlertDialogBuilder.setTitle(R.string.add_bookmark)
            .setPositiveButton(R.string.ok, object : DialogInterface.OnClickListener {
                /* synthetic */ var `f$1`: Context? = null
                /* synthetic */ var `f$2`: EditText? = null
                /* synthetic */ var `f$3`: String? = null
                /* synthetic */ var `f$4` = 0
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    `lambda$addPageBookmark$5$PDFViewerActivity`(
                        `f$1`,
                        `f$2`,
                        `f$3`,
                        `f$4`,
                        dialogInterface,
                        i
                    )
                }

                init {
                    `f$1` = context2
                    `f$2` = editText
                    `f$3` = str
                    `f$4` = i
                }
            } as DialogInterface.OnClickListener)
            .setNegativeButton(R.string.cancel, null as DialogInterface.OnClickListener?)
        val create = materialAlertDialogBuilder.create()
        val i2 = (24.0f * f).toInt()
        create.setView(editText, i2, (8.0f * f).toInt(), i2, (f * 5.0f).toInt())
        create.show()
    }

    /* synthetic */   fun `lambda$addPageBookmark$5$PDFViewerActivity`(
        context2: Context?,
        editText: EditText?,
        str: String?,
        i: Int,
        dialogInterface: DialogInterface?,
        i2: Int
    ) {
        DbHelper.getInstance(context2).addBookmark(
            str,
            if (TextUtils.isEmpty(editText!!.text.toString())) getString(R.string.bookmark) else editText.text.toString(),
            i
        )
        Toast.makeText(
            context2,
            getString(R.string.page) + " " + i + " " + getString(R.string.bookmark_added),
            0
        ).show()
    }

    fun showContents(str: String?) {
    }

    fun jumpToPage() {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context!!)
        materialAlertDialogBuilder.setTitle(R.string.jump_to_page)
            .setView(R.layout.dialog_jump_to_page).setPositiveButton(
            R.string.ok, null as DialogInterface.OnClickListener?
        ).setNegativeButton(R.string.cancel, null as DialogInterface.OnClickListener?)

        val aa: Drawable? = ContextCompat.getDrawable(context!!, R.drawable.popup_bg)
        aa!!.setTint(ContextCompat.getColor(context!!, R.color.background_color_day_night))
        materialAlertDialogBuilder.setBackground(aa)
        val create = materialAlertDialogBuilder.create()
        create.show()
        create.getButton(-1).setOnClickListener(object : View.OnClickListener {
            /* synthetic */ var `f$1`: TextInputEditText? = null
            /* synthetic */ var `f$2`: AlertDialog? = null
            override fun onClick(view: View) {
                `lambda$jumpToPage$6$PDFViewerActivity`(`f$1`, `f$2`, view)
            }

            init {
                `f$1` = create.findViewById(R.id.jump_to)
                `f$2` = create
            }
        })
    }

    /* synthetic */   fun `lambda$jumpToPage$6$PDFViewerActivity`(
        textInputEditText: TextInputEditText?,
        alertDialog: AlertDialog?,
        view2: View?
    ) {
        if (textInputEditText != null && textInputEditText.text != null) {
            val obj = textInputEditText.text.toString()
            if (isValidPageNumber(obj)) {
                alertDialog!!.dismiss()
                pdfView!!.jumpTo(obj.toInt() - 1, true)
                return
            }
            textInputEditText.error = getString(R.string.invalid_page_number)
        }
    }

    fun toggleNightMode(menuItem: MenuItem) {
        val z = sharedPreferences!!.getBoolean(SettingsFragment.NIGHT_MODE_ENABLED_PDFVIEW, false)
        nightModeEnabled = z
        setupNightModeIcons(menuItem, !z)
        pdfView!!.setNightMode(!nightModeEnabled)
        pdfView!!.invalidate()
        sharedPreferences!!.edit()
            .putBoolean(SettingsFragment.NIGHT_MODE_ENABLED_PDFVIEW, !nightModeEnabled).apply()
        setupToggleSwipeIcons(
            mMenu!!.findItem(R.id.action_toggle_view),
            sharedPreferences!!.getBoolean(SettingsFragment.SWIPE_HORIZONTAL_ENABLED, false)
        )
    }

    fun showPdfTools() {
    }

    fun setupToggleSwipeIcons(menuItem: MenuItem, z: Boolean) {
        if (z) {
            menuItem.setIcon(R.drawable.ic_action_swipe_vertical)
            menuItem.setTitle(R.string.swipe_vertical)
            return
        }
        menuItem.setIcon(R.drawable.ic_action_swipe_horizontal)
        menuItem.setTitle(R.string.swipe_horizontal)
    }

    fun setupNightModeIcons(menuItem: MenuItem, z: Boolean) {
        val resources = context!!.resources
        if (z) {
            val i = getResources().configuration.uiMode
            if (AppCompatDelegate.getDefaultNightMode() == 2) {
                menuItem.setIcon(R.drawable.ic_action_light_mode_night)
            } else {
                menuItem.setIcon(R.drawable.ic_action_light_mode)
            }
            menuItem.setTitle(R.string.light_mode)
            pdfView!!.setBackgroundColor(resources.getColor(R.color.colorPDFViewBgNight))
            return
        }
        if (AppCompatDelegate.getDefaultNightMode() == 2) {
            menuItem.setIcon(R.drawable.ic_action_night_mode_night)
        } else {
            menuItem.setIcon(R.drawable.ic_action_night_mode_light)
        }
        menuItem.setTitle(R.string.night_mode)
        pdfView!!.setBackgroundColor(context!!.resources.getColor(R.color.background_color_day_night))
    }

    fun showEnterPasswordDialog() {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context!!)
        materialAlertDialogBuilder.setTitle(R.string.password_protected)
            .setPositiveButton(R.string.ok, null as DialogInterface.OnClickListener?)
            .setCancelable(false).setView(
            R.layout.dialog_edit_password
        ).setNegativeButton(
            R.string.cancel,
            DialogInterface.OnClickListener { dialogInterface, i ->
                `lambda$showEnterPasswordDialog$7$PDFViewerActivity`(
                    dialogInterface,
                    i
                )
            })
        val aa: Drawable? = ContextCompat.getDrawable(context!!, R.drawable.popup_bg)
        aa!!.setTint(ContextCompat.getColor(context!!, R.color.background_color_day_night))
        materialAlertDialogBuilder.setBackground(aa);
        val create = materialAlertDialogBuilder.create()
        create.show()
        val textInputEditText = create.findViewById<View>(R.id.input_text) as TextInputEditText?
        create.getButton(-1).setOnClickListener(View.OnClickListener {
            val pDDocument: PDDocument
            if (textInputEditText != null && textInputEditText.text != null) {
                mPassword = textInputEditText.text.toString()
                if (!TextUtils.isEmpty(mPassword)) {
                    try {
                        val str = TAG
                        Log.d(str, "This is a path " + filePath)
                        pDDocument = if (uri != null) {
                            PDDocument.load(
                                this@PDFViewerActivity.contentResolver.openInputStream(
                                    uri!!
                                ), mPassword
                            )
                        } else {
                            PDDocument.load(File(filePath), mPassword)
                        }
                        pDDocument.close()
                        loadPdfFile(
                            mPassword,
                            pageNumber,
                            swipeHorizontalEnabled,
                            nightModeEnabled,
                            fitPolicy
                        )
                        create.dismiss()
                    } catch (e: Exception) {
                        if (e is InvalidPasswordException) {
                            textInputEditText.error = context!!.getString(R.string.invalid_password)
                            Log.d(TAG, "Invalid Password")
                            return@OnClickListener
                        }
                        e.printStackTrace()
                    }
                } else {
                    textInputEditText.error = context!!.getString(R.string.invalid_password)
                    Log.d(TAG, "Invalid Password")
                }
            }
        })
    }

    /* synthetic */   fun `lambda$showEnterPasswordDialog$7$PDFViewerActivity`(
        dialogInterface: DialogInterface?,
        i: Int
    ) {
        finish()
    }

    fun isValidPageNumber(str: String?): Boolean {
        if (!TextUtils.isEmpty(str) && TextUtils.isDigitsOnly(str)) {
            val pageCount = pdfView!!.pageCount
            try {
                val intValue = Integer.valueOf(str).toInt()
                return !(intValue <= 0 || intValue > pageCount)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun toggle() {
        if (mVisible) {
            `lambda$new$3$PDFViewerActivity`()
            return
        }
        show()
        if (AUTO_HIDE) {
            delayedHide(com.fulcrumy.pdfeditor.data.Constants.AUTO_HIDE_DELAY_MILLIS)
        }
    }

    fun `lambda$new$3$PDFViewerActivity`() {
        val supportActionBar = supportActionBar
        supportActionBar?.hide()
        mVisible = false
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, 1)
    }

    private fun show() {
        pdfView!!.systemUiVisibility = 1536
        mVisible = true
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, 1)
    }

    private fun delayedHide(i: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, i.toLong())
    }

    fun setShowRemoveAds() {
        if (showRemoveAds) {
            Snackbar.make(findViewById(R.id.pdf_container), R.string.dont_like_ads, 4000).setAction(
                R.string.remove,
                View.OnClickListener { view -> `lambda$setShowRemoveAds$8$PDFViewerActivity`(view) })
                .show()
        }
    }

    /* synthetic */   fun `lambda$setShowRemoveAds$8$PDFViewerActivity`(view2: View?) {
        Utils.showSubscriptionOptions(applicationContext)
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun print() {
        val uri2 = uri
        if (uri2 != null) {
            Utils.print(this, uri2)
        } else {
            Utils.print(
                this, Uri.fromFile(
                    File(
                        filePath
                    )
                )
            )
        }
    }

    private fun share() {
        val uri =
            FileProvider.getUriForFile(this@PDFViewerActivity, "{applicationId}.com.me.shareFile", File(filePath))
        val uris = ArrayList<Uri>()
        uris.add(uri)
        shareFile(uris)
    }

    private fun shareFile(uris: ArrayList<Uri>) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "application/pdf"
        startActivity(Intent.createChooser(intent, "Select app to send message…"))
    }

    private fun shareAsPicture() {
        val uri2 = uri
        if (uri2 != null) {
            showShareAsPicture(uri2)
            return
        }
        try {
            showShareAsPicture(Uri.fromFile(File(filePath)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.cant_share_file, 1).show()
        }
    }

    fun setupFeatureDiscoverty() {
        val defaultSharedPreferences =
            android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        if (defaultSharedPreferences.getBoolean("prefs_show_tools_tutorial", true)) {
            TapTargetView.showFor(
                this as Activity,
                TapTarget.forToolbarMenuItem(
                    toolbar,
                    R.id.action_pdf_tools,
                    getString(R.string.pdf_tools) as CharSequence,
                    getString(R.string.show_tools_hint) as CharSequence
                ).titleTextColor(R.color.color54).tintTarget(false),
                object : TapTargetView.Listener() {
                    override fun onTargetCancel(tapTargetView: TapTargetView) {
                        super.onTargetCancel(tapTargetView)
                        defaultSharedPreferences.edit()
                            .putBoolean("prefs_show_tools_tutorial", false).apply()
                    }

                    override fun onTargetClick(tapTargetView: TapTargetView) {
                        super.onTargetClick(tapTargetView)
                        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                            /* synthetic */ var `f$1`: SharedPreferences? = null
                            override fun run() {
                                `lambda$onTargetClick$0$PDFViewerActivity$6`(`f$1`)
                            }

                            init {
                                `f$1` = defaultSharedPreferences
                            }
                        }, 200)
                    }

                    /* synthetic */   fun `lambda$onTargetClick$0$PDFViewerActivity$6`(
                        sharedPreferences: SharedPreferences?
                    ) {
                        showPdfTools()
                        sharedPreferences!!.edit().putBoolean("prefs_show_tools_tutorial", false)
                            .apply()
                    }
                } as TapTargetView.Listener)
        }
    }

    fun showLoadedAd(activity: Activity?) {
        val ad = AdManagerInterstitial.getAd()
        if (ad != null && !AdManagerInterstitial.adShowed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    AdManagerInterstitial.adShowed = true
                    AdManagerInterstitial.createAd(activity)
                }
            }
            ad.show(activity!!)
        }
    }

    private fun showDeleteConfirmDialog(str: String?) {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context!!)
        materialAlertDialogBuilder.setTitle(R.string.permanently_delete_file)
            .setPositiveButton(R.string.delete, object : DialogInterface.OnClickListener {
                /* synthetic */ var `f$1`: String? = null
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    `lambda$showDeleteConfirmDialog$10$PDFViewerActivity`(`f$1`, dialogInterface, i)
                }

                init {
                    `f$1` = str
                }
            } as DialogInterface.OnClickListener)
            .setNegativeButton(R.string.cancel, null as DialogInterface.OnClickListener?)
        materialAlertDialogBuilder.show()
    }

    /* synthetic */   fun `lambda$showDeleteConfirmDialog$10$PDFViewerActivity`(
        str: String?,
        dialogInterface: DialogInterface?,
        i: Int
    ) {
        val file = File(str)
        if (file.delete()) {
            File(context!!.cacheDir.toString() + "/Thumbnails/" + Utils.removeExtension(file.name) + ".jpg").delete()
            MediaScannerConnection.scanFile(
                context,
                arrayOf(str),
                null as Array<String?>?,
                object : OnScanCompletedListener {
                    /* synthetic */ var `f$1`: String? = null
                    override fun onScanCompleted(str: String, uri: Uri) {
                        `lambda$null$9$PDFViewerActivity`(`f$1`, str, uri)
                    }

                    init {
                        `f$1` = str
                    }
                })
            return
        }
        Toast.makeText(context, "Can't delete file", 1).show()
    }

    /* synthetic */   fun `lambda$null$9$PDFViewerActivity`(
        str: String?,
        str2: String?,
        uri2: Uri?
    ) {
        EventBus.getDefault().post(PermanetlyDeleteEvent())
        val str3 = TAG
        Log.d(str3, "File deleted $str")
        finish()
    }

    fun showAdmobBannerAd() {
        MobileAds.initialize(this)
        val adView = AdView(this)
        mAdView = adView
        adView.adUnitId = getString(R.string.admob_banner_ad_id)
        loadBanner()
        mAdView!!.adListener = object : AdListener() {
            override fun onAdClicked() {}
            override fun onAdClosed() {}
            override fun onAdOpened() {}
            override fun onAdLoaded() {
                Log.d(TAG, "Admob AD Loaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val str = TAG
                Log.d(str, "Admob AD Filed to load. " + loadAdError.message)
            }
        }
    }

    private fun loadBanner() {
        val build = AdRequest.Builder().build()
        mAdView!!.adSize = adSize
        mAdView!!.loadAd(build)
    }

    private val adSize: AdSize
        private get() {
            val defaultDisplay = windowManager.defaultDisplay
            val displayMetrics = DisplayMetrics()
            defaultDisplay.getMetrics(displayMetrics)
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                this,
                (displayMetrics.widthPixels.toFloat() / displayMetrics.density).toInt()
            )
        }
}