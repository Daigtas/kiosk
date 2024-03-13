package pl.mrugacz95.kiosk

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.os.BatteryManager
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import pl.mrugacz95.kiosk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mAdminComponentName: ComponentName
    private lateinit var mDevicePolicyManager: DevicePolicyManager
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val LOCK_ACTIVITY_KEY = "pl.mrugacz95.kiosk.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAdminComponentName = MyDeviceAdminReceiver.getComponentName(this)
        mDevicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        mDevicePolicyManager.removeActiveAdmin(mAdminComponentName)

        val isAdmin = isAdmin()
        if (isAdmin) {
            Snackbar.make(binding.content, R.string.device_owner, Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.content, R.string.not_device_owner, Snackbar.LENGTH_SHORT).show()
        }
        binding.btStartLockTask.setOnClickListener {
            setKioskPolicies(true, isAdmin)
        }
        binding.btStopLockTask.setOnClickListener {
            setKioskPolicies(false, isAdmin)
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            intent.putExtra(LOCK_ACTIVITY_KEY, false)
            startActivity(intent)
        }
        binding.btInstallApp.setOnClickListener {
            installApp()
        }
    }

    private fun isAdmin() = mDevicePolicyManager.isDeviceOwnerApp(packageName)

    private fun setKioskPolicies(enable: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            setRestrictions(enable)
            enableStayOnWhilePluggedIn(enable)
            setUpdatePolicy(enable)
            setAsHomeApp(enable)
            setKeyGuardEnabled(enable)
        }
        setLockTask(enable, isAdmin)
        setImmersiveMode(enable)
    }

    // region restrictions
    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_BLUETOOTH_SHARING, disallow)
        setUserRestriction(UserManager.DISALLOW_BLUETOOTH, disallow)
        setUserRestriction(UserManager.DISALLOW_BLUETOOTH, disallow)
        setUserRestriction(UserManager.DISALLOW_CAMERA_TOGGLE, disallow)
        setUserRestriction(UserManager.DISALLOW_CELLULAR_2G, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_DEFAULT_APPS, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_LOCATION, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, disallow)
        setUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING, disallow)
        setUserRestriction(UserManager.DISALLOW_CONTENT_CAPTURE, disallow)
        setUserRestriction(UserManager.DISALLOW_CONTENT_SUGGESTIONS, disallow)
        setUserRestriction(UserManager.DISALLOW_DATA_ROAMING, disallow)
        setUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES, disallow)
        setUserRestriction(UserManager.DISALLOW_GRANT_ADMIN, disallow)
        setUserRestriction(UserManager.DISALLOW_INSTALL_APPS, disallow)
        setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, disallow)
        setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, disallow)
        setUserRestriction(UserManager.DISALLOW_MICROPHONE_TOGGLE, disallow)
        setUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS, disallow)
        setUserRestriction(UserManager.DISALLOW_OUTGOING_BEAM, disallow)
        setUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS, disallow)
        setUserRestriction(UserManager.DISALLOW_PRINTING, disallow)
        setUserRestriction(UserManager.DISALLOW_REMOVE_MANAGED_PROFILE, disallow)
        setUserRestriction(UserManager.DISALLOW_REMOVE_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_SHARE_INTO_MANAGED_PROFILE, disallow)
        setUserRestriction(UserManager.DISALLOW_SHARE_LOCATION, disallow)
        setUserRestriction(UserManager.DISALLOW_SMS, disallow)
        setUserRestriction(UserManager.DISALLOW_ULTRA_WIDEBAND_RADIO, disallow)
        setUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS, disallow)
        setUserRestriction(UserManager.DISALLOW_UNMUTE_MICROPHONE, disallow)
        setUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER, disallow)
        setUserRestriction(UserManager.DISALLOW_USER_SWITCH, disallow)
        setUserRestriction(UserManager.DISALLOW_WIFI_DIRECT, disallow)
        setUserRestriction(UserManager.DISALLOW_WIFI_TETHERING, disallow)
        
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction)
    } else {
        mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction)
    }
    // endregion

    private fun enableStayOnWhilePluggedIn(active: Boolean) = if (active) {
        mDevicePolicyManager.setGlobalSetting(
            mAdminComponentName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            (BatteryManager.BATTERY_PLUGGED_AC
                    or BatteryManager.BATTERY_PLUGGED_USB
                    or BatteryManager.BATTERY_PLUGGED_WIRELESS).toString()
        )
    } else {
        mDevicePolicyManager.setGlobalSetting(mAdminComponentName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0")
    }

    private fun setLockTask(start: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            mDevicePolicyManager.setLockTaskPackages(
                mAdminComponentName, if (start) arrayOf(packageName) else arrayOf()
            )
        }
        if (start) {
            startLockTask()
        } else {
            stopLockTask()
        }
    }

    private fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            mDevicePolicyManager.setSystemUpdatePolicy(
                mAdminComponentName,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, null)
        }
    }

    private fun setAsHomeApp(enable: Boolean) {
        if (enable) {
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            mDevicePolicyManager.addPersistentPreferredActivity(
                mAdminComponentName, intentFilter, ComponentName(packageName, MainActivity::class.java.name)
            )
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                mAdminComponentName, packageName
            )
        }
    }

    private fun setKeyGuardEnabled(enable: Boolean) {
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, !enable)
    }

    @Suppress("DEPRECATION")
    private fun setImmersiveMode(enable: Boolean) {
        if (enable) {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = flags
        } else {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun createIntentSender(context: Context?, sessionId: Int, packageName: String?): IntentSender {
        val intent = Intent("INSTALL_COMPLETE")
        if (packageName != null) {
            intent.putExtra("PACKAGE_NAME", packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            FLAG_IMMUTABLE
        )
        return pendingIntent.intentSender
    }

    private fun installApp() {
        if (!isAdmin()) {
            Snackbar.make(binding.content, R.string.not_device_owner, Snackbar.LENGTH_LONG).show()
            return
        }
        val raw = resources.openRawResource(R.raw.other_app)
        val packageInstaller: PackageInstaller = packageManager.packageInstaller
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName("com.mrugas.smallapp")
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        val out = session.openWrite("SmallApp", 0, -1)
        val buffer = ByteArray(65536)
        var c: Int
        while (raw.read(buffer).also { c = it } != -1) {
            out.write(buffer, 0, c)
        }
        session.fsync(out)
        out.close()
        createIntentSender(this, sessionId, packageName).let { intentSender ->
            session.commit(intentSender)
        }
        session.close()
    }
}
