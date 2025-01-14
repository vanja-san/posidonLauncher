package posidon.launcher.items.users

import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.palette.graphics.Palette
import posidon.launcher.Global
import posidon.launcher.Home
import posidon.launcher.items.App
import posidon.launcher.storage.Settings
import posidon.launcher.tools.dp
import posidon.launcher.tools.theme.Customizer
import posidon.launcher.tools.theme.Graphics
import posidon.launcher.tools.theme.Icons
import posidon.launcher.tools.theme.toBitmap
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class AppLoader (
    context: Context,
    private val onEnd: () -> Unit
) {

    private var tmpApps = ArrayList<App>()
    private val tmpAppSections = ArrayList<ArrayList<App>>()
    private var tmpHidden = ArrayList<App>()
    private val context: WeakReference<Context> = WeakReference(context)
    private var lock = ReentrantLock()

    fun execute() { thread(isDaemon = true, block = ::run) }
    fun run() {
        lock.lock()
        val packageManager = context.get()!!.packageManager
        val iconSize = 65.dp.toInt()
        val iconPackName = Settings["iconpack", "system"]
        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }
        val maskp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        var iconPackInfo = Icons.IconPackInfo()
        var themeRes: Resources? = null
        val uniformOptions = BitmapFactory.Options().apply {
            inScaled = false
        }
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var areUnthemedIconsChanged = false
        try {
            themeRes = packageManager.getResourcesForApplication(iconPackName)
            iconPackInfo = Icons.getIconPackInfo(themeRes, iconPackName)
            if (iconPackInfo.iconBack != null) {
                val intresiconback = themeRes.getIdentifier(iconPackInfo.iconBack, "drawable", iconPackName)
                if (intresiconback != 0) {
                    back = BitmapFactory.decodeResource(themeRes, intresiconback, uniformOptions)
                    areUnthemedIconsChanged = true
                }
            }
            if (iconPackInfo.iconMask != null) {
                val intresiconmask = themeRes.getIdentifier(iconPackInfo.iconMask, "drawable", iconPackName)
                if (intresiconmask != 0) {
                    mask = BitmapFactory.decodeResource(themeRes, intresiconmask, uniformOptions)
                    areUnthemedIconsChanged = true
                }
            }
            if (iconPackInfo.iconFront != null) {
                val intresiconfront = themeRes.getIdentifier(iconPackInfo.iconFront, "drawable", iconPackName)
                if (intresiconfront != 0) {
                    front = BitmapFactory.decodeResource(themeRes, intresiconfront, uniformOptions)
                    areUnthemedIconsChanged = true
                }
            }
        } catch (e: Exception) {}

        val userManager = Home.instance.getSystemService(Context.USER_SERVICE) as UserManager

        val threads = LinkedList<Thread>()

        for (profile in userManager.userProfiles) {

            val appList = (context.get()!!.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps).getActivityList(null, profile)

            for (i in appList.indices) {

                val packageName = appList[i].applicationInfo.packageName
                val name = appList[i].name

                var label = Settings["$packageName/$name?label", appList[i].label.toString()]
                if (label.isEmpty()) {
                    Settings["$packageName/$name?label"] = appList[i].label.toString()
                    label = appList[i].label.toString()
                    if (label.isEmpty()) {
                        label = packageName
                    }
                }

                val app = App(packageName, name, profile, label)

                threads.add(thread (isDaemon = true) {
                    val customIcon = Customizer.getCustomIcon("app:$app:icon")
                    if (customIcon != null) {
                        app.icon = customIcon
                    } else {
                        var intres = 0
                        val iconResource = iconPackInfo.iconResourceNames["ComponentInfo{" + app.packageName + "/" + app.name + "}"]
                        if (iconResource != null) {
                            intres = themeRes!!.getIdentifier(iconResource, "drawable", iconPackName)
                        }
                        if (intres != 0) {
                            try {
                                app.icon = themeRes!!.getDrawable(intres, null)
                            } catch (e: Exception) {
                                app.icon = appList[i].getIcon(0)
                            }
                        } else {
                            app.icon = appList[i].getIcon(0)
                            if (areUnthemedIconsChanged) {
                                try {
                                    var orig = Bitmap.createBitmap(app.icon!!.intrinsicWidth, app.icon!!.intrinsicHeight, Bitmap.Config.ARGB_8888)
                                    app.icon!!.setBounds(0, 0, app.icon!!.intrinsicWidth, app.icon!!.intrinsicHeight)
                                    app.icon!!.draw(Canvas(orig))
                                    val scaledBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                                    Canvas(scaledBitmap).run {
                                        if (back != null) {
                                            drawBitmap(back, Graphics.getResizedMatrix(back, iconSize, iconSize), p)
                                        }
                                        val scaledOrig = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                                        Canvas(scaledOrig).run {
                                            orig = Graphics.getResizedBitmap(orig, (iconSize * iconPackInfo.scaleFactor).toInt(), (iconSize * iconPackInfo.scaleFactor).toInt())
                                            drawBitmap(orig, scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f, scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f, p)
                                            if (mask != null) {
                                                drawBitmap(mask, Graphics.getResizedMatrix(mask, iconSize, iconSize), maskp)
                                            }
                                        }
                                        drawBitmap(Graphics.getResizedBitmap(scaledOrig, iconSize, iconSize), 0f, 0f, p)
                                        if (front != null) {
                                            drawBitmap(front, Graphics.getResizedMatrix(front, iconSize, iconSize), p)
                                        }
                                        scaledOrig.recycle()
                                    }
                                    app.icon = BitmapDrawable(context.get()!!.resources, scaledBitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        app.icon = Icons.generateAdaptiveIcon(app.icon!!)
                    }
                    app.icon = Icons.badgeMaybe(app.icon!!, appList[i].user != Process.myUserHandle())
                    Icons.animateIfShould(context.get()!!, app.icon!!)
                })

                putInSecondMap(app)
                if (Settings["app:$app:hidden", false]) {
                    tmpHidden.add(app)
                } else {
                    tmpApps.add(app)
                }
            }
        }

        for (t in threads) {
            t.join()
        }

        threads.clear()

        if (Settings["drawer:sorting", 0] == 1) tmpApps.sortWith { o1, o2 ->
            val iHsv = floatArrayOf(0f, 0f, 0f)
            val jHsv = floatArrayOf(0f, 0f, 0f)
            Color.colorToHSV(Palette.from(o1.icon!!.toBitmap()).generate().getVibrantColor(0xff252627.toInt()), iHsv)
            Color.colorToHSV(Palette.from(o2.icon!!.toBitmap()).generate().getVibrantColor(0xff252627.toInt()), jHsv)
            iHsv[0].compareTo(jHsv[0])
        }
        else tmpApps.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }

        var currentChar = tmpApps[0].label[0].toUpperCase()
        var currentSection = ArrayList<App>().also { tmpAppSections.add(it) }
        for (app in tmpApps) {
            if (app.label.startsWith(currentChar, ignoreCase = true)) {
                currentSection.add(app)
            }
            else currentSection = ArrayList<App>().apply {
                add(app)
                tmpAppSections.add(this)
                currentChar = app.label[0].toUpperCase()
            }
        }

        lock.unlock()

        Home.instance.runOnUiThread {
            App.onFinishLoad(tmpApps, tmpAppSections, tmpHidden, appsByName)
            onEnd()
        }

        System.gc()
    }

    class Callback(
        val context: Context,
        val onAppLoaderEnd: () -> Unit
    ) : LauncherApps.Callback() {
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle?, replacing: Boolean) = AppLoader(context, onAppLoaderEnd).execute()
        override fun onPackageChanged(packageName: String, user: UserHandle?) = AppLoader(context, onAppLoaderEnd).execute()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle?, replacing: Boolean) = AppLoader(context, onAppLoaderEnd).execute()
        override fun onPackageAdded(packageName: String, user: UserHandle?) = AppLoader(context, onAppLoaderEnd).execute()
        override fun onPackageRemoved(packageName: String, user: UserHandle?) {
            Global.apps.removeAll { it.packageName == packageName }
            val iter = Global.appSections.iterator()
            for (section in iter) {
                section.removeAll {
                    it.packageName == packageName
                }
                if (section.isEmpty()) {
                    iter.remove()
                }
            }
            App.removePackage(packageName)
            onAppLoaderEnd()
        }
    }

    private var appsByName = HashMap<String, ArrayList<App>>()
    private fun putInSecondMap(app: App) {
        val list = appsByName[app.packageName]
        if (list == null) {
            appsByName[app.packageName] = arrayListOf(app)
            return
        }
        val thisAppI = list.indexOfFirst {
            it.name == app.name && it.userHandle.hashCode() == app.userHandle.hashCode()
        }
        if (thisAppI == -1) {
            list.add(app)
            return
        }
        list[thisAppI] = app
    }
}