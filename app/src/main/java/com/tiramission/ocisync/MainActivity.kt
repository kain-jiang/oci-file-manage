package com.tiramission.ocisync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tiramission.ocisync.ui.navigation.OciSyncAppRoot
import com.tiramission.ocisync.ui.theme.OciSyncTheme

/** 应用唯一 Activity:接入完整导航(M5+),底部三 Tab 贯穿全部页面,见 ui-design/ 设计稿。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OciSyncTheme {
                OciSyncAppRoot()
            }
        }
    }
}
