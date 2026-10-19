package com.fonex.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fonex.dialer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var currentTab = R.id.nav_dialer
    private var pendingNumber: String? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            showTab(currentTab) // reload the visible tab now that permissions may have changed
        }

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateBanner()
            showTab(currentTab)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.nav.setOnItemSelectedListener {
            showTab(it.itemId)
            true
        }
        b.btnDefault.setOnClickListener { requestDefaultDialer() }

        handleIntent(intent)
        if (savedInstanceState == null) {
            b.nav.selectedItemId = R.id.nav_dialer
            showTab(R.id.nav_dialer)
        } else {
            currentTab = b.nav.selectedItemId
        }
        requestPerms()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        b.nav.menu.findItem(R.id.nav_dialer).isChecked = true
        showTab(R.id.nav_dialer)
    }

    override fun onResume() {
        super.onResume()
        updateBanner()
    }

    private fun handleIntent(i: Intent?) {
        if (i == null) return
        if (i.action == Intent.ACTION_DIAL || i.action == Intent.ACTION_VIEW) {
            val data = i.data
            if (data != null && data.scheme == "tel") {
                pendingNumber = Uri.decode(data.schemeSpecificPart)
            }
        }
    }

    fun takePendingNumber(): String? {
        val n = pendingNumber
        pendingNumber = null
        return n
    }

    private fun showTab(id: Int) {
        currentTab = id
        val f: Fragment = when (id) {
            R.id.nav_contacts -> ContactsFragment()
            R.id.nav_history -> HistoryFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> DialerFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commitAllowingStateLoss()
    }

    fun requestPerms() {
        val wanted = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    fun isDefaultDialer(): Boolean {
        val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return tm.defaultDialerPackage == packageName
    }

    private fun updateBanner() {
        b.btnDefault.visibility = if (isDefaultDialer()) View.GONE else View.VISIBLE
    }

    fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            val i = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            roleLauncher.launch(i)
        }
    }
}
