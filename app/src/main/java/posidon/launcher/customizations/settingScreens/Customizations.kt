package posidon.launcher.customizations.settingScreens

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import posidon.launcher.Global
import posidon.launcher.R
import posidon.launcher.storage.Settings
import posidon.launcher.tools.Tools
import posidon.launcher.tools.applyFontSetting
import posidon.launcher.tools.dp
import posidon.launcher.tools.onEnd
import kotlin.math.max

class Customizations : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)
        applyFontSetting()
        setContentView(R.layout.customizations)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            findViewById<View>(R.id.quickStepOptions).visibility = View.VISIBLE
        }
        if (Settings["dev:enabled", false]) {
            findViewById<View>(R.id.devoptions).visibility = View.VISIBLE
        }
        findViewById<View>(R.id.catlist).setPadding(0, 0, 0, max(Tools.navbarHeight, 24.dp.toInt()))
        cardThing()
    }

    fun openHome    (v: View) = startActivity(Intent(this, CustomHome          ::class.java))
    fun openNews    (v: View) = startActivity(Intent(this, CustomNews          ::class.java))
    fun openNotif   (v: View) = startActivity(Intent(this, CustomNotifications ::class.java))
    fun openApps    (v: View) = startActivity(Intent(this, CustomDrawer        ::class.java))
    fun openDock    (v: View) = startActivity(Intent(this, CustomDock          ::class.java))
    fun openSearch  (v: View) = startActivity(Intent(this, CustomSearch        ::class.java))
    fun openFolders (v: View) = startActivity(Intent(this, CustomFolders       ::class.java))
    fun openTheme   (v: View) = startActivity(Intent(this, CustomTheme         ::class.java))
    fun openGestures(v: View) = startActivity(Intent(this, CustomGestures      ::class.java))
    fun openOther   (v: View) = startActivity(Intent(this, CustomOther         ::class.java))
    fun openDev     (v: View) = startActivity(Intent(this, CustomDev           ::class.java))
    fun openAbout   (v: View) = startActivity(Intent(this, About               ::class.java))

    fun hideCard(v: View? = null) {
        Settings["rated"] = true
        findViewById<View>(R.id.card).animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .translationY(findViewById<View>(R.id.card).measuredHeight.toFloat())
                .setDuration(200L)
                .onEnd {
                    findViewById<View>(R.id.card).visibility = View.GONE
                }
    }

    private fun cardThing() {
        if (Global.customized && !Settings["rated", false]) {
            findViewById<View>(R.id.card).visibility = View.VISIBLE
            findViewById<View>(R.id.yesBtn).setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(i, ActivityOptions.makeCustomAnimation(this, R.anim.slideup, R.anim.slidedown).toBundle())
                hideCard()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.devoptions).visibility =
            if (Settings["dev:enabled", false]) View.VISIBLE
            else View.GONE
        cardThing()
    }
}
