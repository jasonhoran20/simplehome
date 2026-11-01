package com.simplehome.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    // To hide an app from the grid, add its package name here, e.g. "com.amazon.avod"
    private val hidden = setOf<String>(
    )
    // Apps listed here appear first, in this order
private val favorites = listOf<String>(
    "com.hulu.plus",
    "com.amazon.firetv.youtube.tv",
    "flix.com.vision",
    "com.tubitv.ott",
)

    private val columns = 5
    private lateinit var grid: GridLayout
    private var tileWidth = 0
    private var density = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dm = resources.displayMetrics
        density = dm.density
        val sidePad = dp(48)
        tileWidth = (dm.widthPixels - 2 * sidePad) / columns

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(sidePad, dp(32), sidePad, 0)
        }

        val clock = TextClock(this).apply {
            format12Hour = "h:mm a"
            format24Hour = "h:mm a"
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val date = TextClock(this).apply {
            format12Hour = "EEEE, MMMM d"
            format24Hour = "EEEE, MMMM d"
            textSize = 18f
            setTextColor(Color.LTGRAY)
        }
        root.addView(clock)
        root.addView(date)

        grid = GridLayout(this).apply { columnCount = columns }

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            setPadding(0, dp(24), 0, dp(24))
            addView(grid)
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        populate()
    }

    // Home button should stay on this screen, not exit.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // do nothing
    }

    private fun populate() {
        val pm = packageManager
        val seen = HashSet<String>()
        val apps = ArrayList<ResolveInfo>()

        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || pkg in hidden) continue
                if (seen.add(pkg)) apps.add(ri)
            }
        }
        apps.sortWith(
    compareBy<ResolveInfo>(
        {
            val i = favorites.indexOf(it.activityInfo.packageName)
            if (i == -1) Int.MAX_VALUE else i
        },
        { it.loadLabel(pm).toString().lowercase() }
    )
) }

        grid.removeAllViews()
        for (ri in apps) grid.addView(makeTile(ri))
        grid.getChildAt(0)?.requestFocus()
    }

    private fun makeTile(ri: ResolveInfo): View {
        val pm = packageManager

        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isFocusable = true
            isClickable = true
            setPadding(dp(8), dp(12), dp(8), dp(12))
            background = tileBackground(false)
            layoutParams = GridLayout.LayoutParams().apply {
                width = tileWidth
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            setOnFocusChangeListener { v, hasFocus -> v.background = tileBackground(hasFocus) }
            setOnClickListener { launch(ri) }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(ri.loadIcon(pm))
            layoutParams = LinearLayout.LayoutParams(dp(88), dp(88))
        }

        val label = TextView(this).apply {
            text = ri.loadLabel(pm)
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(8), 0, 0)
        }

        tile.addView(icon)
        tile.addView(label)
        return tile
    }

    private fun tileBackground(focused: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        if (focused) {
            setColor(0x33FFFFFF)
            setStroke(dp(3), Color.WHITE)
        } else {
            setColor(Color.TRANSPARENT)
        }

    private fun launch(ri: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open that app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int) = (value * density).toInt()
}
