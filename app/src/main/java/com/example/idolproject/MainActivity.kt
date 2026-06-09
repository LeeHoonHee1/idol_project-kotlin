package com.example.idolproject

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.idolproject.Drawer.ComeBack.ComeBackFragment
import com.example.idolproject.Drawer.Community.CommunityFragment
import com.example.idolproject.Drawer.CustomerFragment
import com.example.idolproject.Drawer.EventFragment
import com.example.idolproject.Drawer.Group.GroupFragment
import com.example.idolproject.Drawer.LogOutFragment
import com.example.idolproject.UI.Friend.FriendFragment
import com.example.idolproject.UI.Home.HomeFragment
import com.example.idolproject.UI.Mission.MissionFragment
import com.example.idolproject.UI.MyPage.MyPageFragment
import com.example.idolproject.UI.Ranking.RankingFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import android.view.Menu
import com.example.idolproject.UI.Friend.FriendRequestsFragment
import com.example.idolproject.UI.Friend.FriendRequestRepository
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import com.example.idolproject.Drawer.Community.GroupChatActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigationView: BottomNavigationView
    private val friendRepo = FriendRequestRepository()
    private val auth = FirebaseAuth.getInstance()
    private var pendingListener: ListenerRegistration? = null
    private var pendingCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        // 햄버거 버튼 → Drawer 열기
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Drawer 메뉴 리스너
        navigationView.setNavigationItemSelectedListener(this)

        // Drawer 헤더
        val headerView = navigationView.getHeaderView(0)
        val headerNickname = headerView.findViewById<TextView>(R.id.header_nickname)
        val headerEmail = headerView.findViewById<TextView>(R.id.header_email)
        val headerSettingBtn = headerView.findViewById<ImageButton>(R.id.btn_header_setting)

        headerNickname.text = "훈희 님"
        headerEmail.text = "user@example.com"

        headerSettingBtn.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Drawer 열릴 때 Toolbar 살짝 이동
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val moveX = drawerView.width * slideOffset / 3f
                toolbar.translationX = moveX
                bottomNavigationView.translationX = moveX
            }
        })

        // 앱 첫 실행 시 보여줄 화면 (예시 1개)
        if (savedInstanceState == null) {
            showFragment(HomeFragment())    // ← 네가 원하는 프래그먼트로 바꾸면 됨
            bottomNavigationView.selectedItemId = R.id.nav_home
        }

        // ⭐ 바텀네비 전환 (예시 1개)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(HomeFragment())
                    true
                }
                R.id.nav_friend -> {
                    showFragment(FriendFragment())
                    true
                }
                R.id.nav_ranking -> {
                    showFragment(RankingFragment())
                    true
                }
                R.id.nav_mission -> {
                    showFragment(MissionFragment())
                    true
                }
                R.id.nav_mypage -> {
                    showFragment(MyPageFragment())
                    true
                }
                else -> false
            }
        }

        // 뒤로가기 제스처 처리
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            pendingListener = friendRepo.listenUncheckedPendingCount(uid) { count ->
                pendingCount = count
                invalidateOptionsMenu() // 빨간점 갱신
            }
        }

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        updateCurrentFcmToken()
        handleChatIntent(intent)
    }

    // ⭐ 드로어 전환 (예시 1개)
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_event -> {
                showFragment(EventFragment())
            }
            R.id.nav_group -> {
                showFragment(GroupFragment())
            }
            R.id.nav_comeback -> {
                showFragment(ComeBackFragment())
            }
            R.id.nav_community -> {
                showFragment(CommunityFragment())
            }
            R.id.nav_service -> {
                showFragment(CustomerFragment())
            }
            R.id.nav_logout -> {
                showFragment(LogOutFragment())
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // 공통 프래그먼트 교체 함수
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        supportActionBar?.title = when (fragment) {
            is HomeFragment -> "홈"
            is FriendFragment -> "친구"
            is RankingFragment -> "랭킹"
            is MissionFragment -> "미션"
            is MyPageFragment -> "마이페이지"
            is EventFragment -> "이벤트"
            is GroupFragment -> "그룹 일정"
            is ComeBackFragment -> "컴백 일정"
            is CommunityFragment -> "커뮤니티"
            is CustomerFragment -> "고객센터"
            is LogOutFragment -> "로그아웃"
            else -> getString(R.string.app_name)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_notifications) ?: return super.onPrepareOptionsMenu(menu)

        // MVP: count>0이면 커스텀 actionView로 빨간점 표시
        if (pendingCount > 0) {
            if (item.actionView == null) {
                item.setActionView(R.layout.view_toolbar_badge)
                item.actionView?.setOnClickListener { onOptionsItemSelected(item) }
            }
            item.actionView?.findViewById<View>(R.id.view_red_dot)?.visibility = View.VISIBLE
        } else {
            item.actionView = null
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, FriendRequestsFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        pendingListener?.remove()
        super.onDestroy()
    }
    // (옵션) 현재 프래그먼트 맨 위로 스크롤 – 나중에 ScrollToTop 인터페이스 연결용
//    private fun scrollCurrentFragmentToTop() {
//        val currentFragment =
//            supportFragmentManager.findFragmentById(R.id.fragment_container)
//
//        if (currentFragment is ScrollToTop) {
//            currentFragment.scrollToTop()
//        }
//    }

    private fun updateCurrentFcmToken() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("FCM", "current token = $token")
                db.collection("users")
                    .document(user.uid)
                    .update("fcmToken", token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "get token failed", e)
            }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chat_messages",
                "채팅 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "오픈채팅 새 메시지 알림"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChatIntent(intent)
    }

    private fun handleChatIntent(intent: Intent?) {
        if (intent == null) return

        val shouldOpenChat = intent.getBooleanExtra("open_chat", false)
        val groupId = intent.getStringExtra("group_id")
        val roomName = intent.getStringExtra("room_name")

        if (!shouldOpenChat || groupId.isNullOrBlank()) return

        startActivity(
            GroupChatActivity.newIntent(
                this,
                groupId,
                roomName ?: "${groupId} 오픈채팅"
            )
        )

        intent.removeExtra("open_chat")
        intent.removeExtra("group_id")
        intent.removeExtra("room_name")
    }
}
